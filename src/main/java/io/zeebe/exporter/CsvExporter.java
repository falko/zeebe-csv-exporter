/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.exporter;

import io.zeebe.exporter.aggregator.JobAggregator;
import io.zeebe.exporter.aggregator.WorkflowInstanceAggregator;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.record.JobCsvRecord;
import io.zeebe.exporter.record.WorkflowInstanceCsvRecord;
import io.zeebe.exporter.writer.CsvFileWriter;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.Intent;
import io.zeebe.protocol.record.intent.JobBatchIntent;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.JobBatchRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import io.zeebe.protocol.record.value.JobRecordValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;

public class CsvExporter implements Exporter {

  public static final String WORKFLOW_INSTANCE_PREFIX = "workflow-instance";
  public static final String JOB_PREFIX = "job";

  private static final List<ValueType> EXPORT_VALUE_TYPE =
      Arrays.asList(ValueType.JOB, ValueType.WORKFLOW_INSTANCE, ValueType.JOB_BATCH);

  private Logger log;
  private Path output;
  private WorkflowInstanceAggregator workflowInstanceCsvWriter;
  private JobAggregator jobCsvWriter;
  @SuppressWarnings("rawtypes")
  private Map<Long, List<Record>> tracesByElementInstanceKey = new HashMap<Long, List<Record>>();
  @SuppressWarnings("rawtypes")
  private Map<Long, List<Record>> tracesByJobKey = new HashMap<Long, List<Record>>();
  HashMap<String, TimeAggregate> times = new HashMap<String, TimeAggregate>();

  @Override
  public void configure(final Context context) {
    log = context.getLogger();
    final CsvExporterConfiguration configuration =
        context.getConfiguration().instantiate(CsvExporterConfiguration.class);

    try {
      output = createOutput(configuration);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    context.setFilter(
        new Context.RecordFilter() {
          @Override
          public boolean acceptType(RecordType recordType) {
            return true;
          }

          @Override
          public boolean acceptValue(ValueType valueType) {
            return EXPORT_VALUE_TYPE.contains(valueType);
          }
        });
  }

  @Override
  public void open(final Controller controller) {
    workflowInstanceCsvWriter =
        new WorkflowInstanceAggregator(
            new CsvFileWriter(output, WORKFLOW_INSTANCE_PREFIX, WorkflowInstanceCsvRecord.class));
    jobCsvWriter = new JobAggregator(new CsvFileWriter(output, JOB_PREFIX, JobCsvRecord.class));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void export(final Record record) {
    List<Record> trace;
    long key = record.getKey();
    Intent intent = record.getIntent();
    if (isServiceTaskActivating(record)) {
      trace = new ArrayList<Record>();
      tracesByElementInstanceKey.put(key, trace);
    }
    final ValueType valueType = record.getValueType();
    switch (valueType) {
      case WORKFLOW_INSTANCE:
        workflowInstanceCsvWriter.process(record);
        if (isServiceTask(record)) {
          trace = tracesByElementInstanceKey.get(key);
          if (intent == WorkflowInstanceIntent.ELEMENT_COMPLETED) {
            tracesByElementInstanceKey.remove(key);
            analyzeTrace(trace);
          }
        } else {
          // other BPMN elements are ignored
          trace = null;
        }
        break;
      case JOB:
        jobCsvWriter.process(record);
        if (intent == JobIntent.CREATE) {
          trace = tracesByElementInstanceKey.get(((JobRecordValue) record.getValue()).getElementInstanceKey());
        } else if (intent == JobIntent.CREATED) {
          trace = tracesByElementInstanceKey.get(((JobRecordValue) record.getValue()).getElementInstanceKey());
          tracesByJobKey.put(key, trace);
        } else { // JOB:ACTIVATED, JOB:COMPLETE & JOB:COMPLETED
          trace = tracesByJobKey.get(key);
          if (intent == JobIntent.COMPLETED) {
            tracesByJobKey.remove(key);
          }
        }
        break;
      case JOB_BATCH:
        if (intent == JobBatchIntent.ACTIVATE) {
          for (Entry<Long, List<Record>> entry : tracesByJobKey.entrySet()) {
            trace = entry.getValue();
            trace.add(record);    
          }
        } else if (intent == JobBatchIntent.ACTIVATED) {
          List<Long> jobKeys = ((JobBatchRecordValue) record.getValue()).getJobKeys();
          for (Long jobKey : jobKeys) {
            trace = tracesByJobKey.get(jobKey);
            trace.add(record);    
          }
        } else {
          throw new UnsupportedOperationException();          
        }
        return;
    default:
      throw new UnsupportedOperationException();
    }
    if (trace != null) {
      trace.add(record);  
    }
  }

  private void analyzeTrace(List<Record> trace) {
    for (Record record : trace) {
      RecordType recordType = record.getRecordType();
      ValueType valueType = record.getValueType();
      Intent intent = record.getIntent();
      String recordName = recordType + ":" + valueType + ":" + intent;
      if (valueType == ValueType.WORKFLOW_INSTANCE) {
        BpmnElementType bpmnElementType = ((WorkflowInstanceRecordValue) record.getValue()).getBpmnElementType();
        recordName += ":" + bpmnElementType;
      }
      
      try {
        Record nextRecord = trace.get(trace.indexOf(record) + 1);
        // TODO: special handling for everyting aroun JOB_BATCH
        // TODO: EVENT:JOB:CREATED → COMMAND:JOB_BATCH:ACTIVATE with right worker id
        // TODO: COMMAND:JOB_BATCH:ACTIVATE with right worker id → EVENT:JOB:ACTIVATED
        // TODO: ignore any COMMAND:JOB_BATCH:ACTIVATE that doesn't have matching worker id
        //       or if there are multiple with a matching worker id only take the last one
        long time = nextRecord.getTimestamp() - record.getTimestamp();
        if (!times.containsKey(recordName)) {
          times.put(recordName, new TimeAggregate(recordName, time));
        } else {
          TimeAggregate timeAggregate = times.get(recordName);
          timeAggregate.add(time);
        }
      } catch (IndexOutOfBoundsException e) {
      }
      TimeAggregate timeAggregate = times.get(recordName);
      log.info(timeAggregate.toString());
    }
  }

  private boolean isServiceTaskActivating(Record record) {
    if (record.getRecordType() == RecordType.EVENT
        && record.getIntent() == WorkflowInstanceIntent.ELEMENT_ACTIVATING
        && isServiceTask(record)) {
      return true;
    }
    return false;
  }

  private boolean isServiceTask(Record record) {
    return ((WorkflowInstanceRecordValue) record.getValue()).getBpmnElementType() == BpmnElementType.SERVICE_TASK;
  }

  @Override
  public void close() {
    try {
      workflowInstanceCsvWriter.close();
    } catch (Exception e) {
      log.warn("Failed to close CSV exporter workflow instance writer", e);
    }

    try {
      jobCsvWriter.close();
    } catch (Exception e) {
      log.warn("Failed to close CSV exporter job writer", e);
    }
  }

  private Path createOutput(CsvExporterConfiguration configuration) throws IOException {
    Path path = Paths.get(configuration.getOutput()).toAbsolutePath();
    return Files.createDirectories(path);
  }
}

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

import io.zeebe.exporter.analysis.InstanceTraceAnalyzer;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Context.RecordFilter;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.record.TimeRecord;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.Intent;
import io.zeebe.protocol.record.intent.JobBatchIntent;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.JobBatchRecordValue;
import io.zeebe.protocol.record.value.JobRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CsvExporter implements Exporter {

  private static final String DELAY_KEY = "delay";
  private static final long DEFAULT_DELAY = 60 * 1000;
  private static final List<ValueType> EXPORT_VALUE_TYPE =
      Arrays.asList(ValueType.JOB, ValueType.WORKFLOW_INSTANCE, ValueType.JOB_BATCH);

  private ScheduledRecorder scheduledRecorder;
  private Map<Long, List<TimeRecord>> tracesByElementInstanceKey;
  private Map<Long, List<TimeRecord>> tracesByJobKey;

  @Override
  public void configure(final Context context) {
    context.setFilter(
        new RecordFilter() {
          @Override
          public boolean acceptType(RecordType recordType) {
            return true;
          }

          @Override
          public boolean acceptValue(ValueType valueType) {
            return EXPORT_VALUE_TYPE.contains(valueType);
          }
        });
    final int delay = this.getDelay(context);

    this.scheduledRecorder = new ScheduledRecorder(delay, new InstanceTraceAnalyzer());
    this.tracesByElementInstanceKey = new HashMap<>();
    this.tracesByJobKey = new HashMap<>();
  }

  @Override
  public void open(final Controller controller) {
    scheduledRecorder.start();
  }

  @Override
  // TODO: if possible, try to simplify this code
  public void export(final Record record) {
    List<TimeRecord> trace;
    long key = record.getKey();
    Intent intent = record.getIntent();
    if (isServiceTaskActivating(record)) {
      trace = new ArrayList<>();
      tracesByElementInstanceKey.put(key, trace);
    }
    final ValueType valueType = record.getValueType();
    switch (valueType) {
      case WORKFLOW_INSTANCE:
        if (isServiceTask(record)) {
          trace = tracesByElementInstanceKey.get(key);
          if (intent == WorkflowInstanceIntent.ELEMENT_COMPLETED) {
            tracesByElementInstanceKey.remove(key);
            scheduledRecorder.addCompleted(key, trace);
          }
        } else {
          trace = null; // other BPMN elements are ignored
        }
        break;
      case JOB:
        if (intent == JobIntent.CREATE) {
          trace =
              tracesByElementInstanceKey.get(
                  ((JobRecordValue) record.getValue()).getElementInstanceKey());
        } else if (intent == JobIntent.CREATED) {
          trace =
              tracesByElementInstanceKey.get(
                  ((JobRecordValue) record.getValue()).getElementInstanceKey());
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
          for (Entry<Long, List<TimeRecord>> entry : tracesByJobKey.entrySet()) {
            trace = entry.getValue();
            trace.add(new TimeRecord(record));
          }
        } else if (intent == JobBatchIntent.ACTIVATED) {
          List<Long> jobKeys = ((JobBatchRecordValue) record.getValue()).getJobKeys();
          for (Long jobKey : jobKeys) {
            trace = tracesByJobKey.get(jobKey);
            trace.add(new TimeRecord(record));
          }
        } else {
          throw new UnsupportedOperationException();
        }
        return;
      default:
        throw new UnsupportedOperationException();
    }
    if (trace != null) {
      trace.add(new TimeRecord(record));
    }
  }

  private boolean isServiceTaskActivating(final Record record) {
    return record.getRecordType() == RecordType.EVENT
        && record.getIntent() == WorkflowInstanceIntent.ELEMENT_ACTIVATING
        && isServiceTask(record);
  }

  private boolean isServiceTask(final Record record) {
    return ((WorkflowInstanceRecordValue) record.getValue()).getBpmnElementType()
        == BpmnElementType.SERVICE_TASK;
  }

  private int getDelay(final Context context) {
    final Map<String, Object> arguments = context.getConfiguration().getArguments();
    long delay = DEFAULT_DELAY;
    if (arguments.containsKey(DELAY_KEY)) {
      delay = (Long) arguments.get(DELAY_KEY);
    }
    return (int) delay;
  }

  @Override
  public void close() {
    // stop the watcher
    scheduledRecorder.stop();
  }
}

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
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.config.Configuration;
import io.zeebe.exporter.config.LatencyFilter;
import io.zeebe.exporter.time.TimeRecord;
import io.zeebe.exporter.time.TimeRecorder;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.Intent;
import io.zeebe.protocol.record.intent.JobBatchIntent;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;

public class CommandEventLatencyExporter implements Exporter {

  private Controller controller;
  private TimeRecorder timeRecorder;
  private Map<Long, List<TimeRecord>> tracesByElementInstanceKey;
  private Map<Long, List<TimeRecord>> tracesByJobKey;
  private Logger logger;

  @Override
  public void configure(final Context context) {
    final Configuration configuration = new Configuration(context);
    this.logger = context.getLogger();
    this.timeRecorder = new TimeRecorder(configuration, new InstanceTraceAnalyzer(logger));
    this.tracesByElementInstanceKey = new HashMap<>();
    this.tracesByJobKey = new HashMap<>();

    context.setFilter(new LatencyFilter());
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    timeRecorder.start();
  }

  /**
   * Expected order of records per service task:
   *
   * <pre>{@code
   *  1. EVENT:WORKFLOW_INSTANCE:ELEMENT_ACTIVATING:SERVICE_TASK
   *  2. EVENT:WORKFLOW_INSTANCE:ELEMENT_ACTIVATED:SERVICE_TASK
   *  3. COMMAND:JOB:CREATE
   *  4. EVENT:JOB:CREATED
   *  5. COMMAND:JOB_BATCH:ACTIVATE
   *  6. EVENT:JOB:ACTIVATED
   *  7. EVENT:JOB_BATCH:ACTIVATED
   *  8. COMMAND:JOB:COMPLETE
   *  9. EVENT:JOB:COMPLETED
   * 10. EVENT:WORKFLOW_INSTANCE:ELEMENT_COMPLETING:SERVICE_TASK
   * 11. EVENT:WORKFLOW_INSTANCE:ELEMENT_COMPLETED:SERVICE_TASK
   * }</pre>
   */
  @Override
  public void export(final Record record) {
    this.controller.updateLastExportedRecordPosition(record.getPosition());
    final TimeRecord clone = new TimeRecord(record);
    List<TimeRecord> trace;
    long key = clone.getKey();
    Intent intent = clone.getIntent();
    if (clone.isServiceTask() && clone.isActivating()) {
      // 1. EVENT:WORKFLOW_INSTANCE:ELEMENT_ACTIVATING:SERVICE_TASK
      trace = new ArrayList<>();
      tracesByElementInstanceKey.put(key, trace);
    }
    final ValueType valueType = clone.getValueType();
    switch (valueType) {
      case WORKFLOW_INSTANCE:
        if (clone.isServiceTask()) {
          //  1. EVENT:WORKFLOW_INSTANCE:ELEMENT_ACTIVATING:SERVICE_TASK
          //  2. EVENT:WORKFLOW_INSTANCE:ELEMENT_ACTIVATED:SERVICE_TASK
          // 10. EVENT:WORKFLOW_INSTANCE:ELEMENT_COMPLETING:SERVICE_TASK
          // 11. EVENT:WORKFLOW_INSTANCE:ELEMENT_COMPLETED:SERVICE_TASK
          trace = tracesByElementInstanceKey.get(key);
          trace.add(clone);
          if (intent == WorkflowInstanceIntent.ELEMENT_COMPLETED) {
            // 11. EVENT:WORKFLOW_INSTANCE:ELEMENT_COMPLETED:SERVICE_TASK
            tracesByElementInstanceKey.remove(key);
            timeRecorder.offer(trace);
          }
        }
        break;
      case JOB: // key = jobKey
        if (intent == JobIntent.CREATE) { // 3. COMMAND:JOB:CREATE
          trace = tracesByElementInstanceKey.get(clone.getElementInstanceKey());
        } else if (intent == JobIntent.CREATED) { // 4. EVENT:JOB:CREATED
          trace = tracesByElementInstanceKey.get(clone.getElementInstanceKey());
          tracesByJobKey.put(key, trace);
        } else {
          // 6. EVENT:JOB:ACTIVATED
          // 8. COMMAND:JOB:COMPLETE
          // 9. EVENT:JOB:COMPLETED
          trace = tracesByJobKey.get(key);
          if (intent == JobIntent.COMPLETED) {
            // 9. EVENT:JOB:COMPLETED
            tracesByJobKey.remove(key);
          }
        }
        if (trace != null) {
          trace.add(clone);
        } else {
          logger.error("No trace found for record: " + clone);
        }
        break;
      case JOB_BATCH:
        if (intent == JobBatchIntent.ACTIVATE) {
          // 5. COMMAND:JOB_BATCH:ACTIVATE
          for (Entry<Long, List<TimeRecord>> entry : tracesByJobKey.entrySet()) {
            trace = entry.getValue();
            trace.add(clone);
          }
        } else if (intent == JobBatchIntent.ACTIVATED) {
          // 7. EVENT:JOB_BATCH:ACTIVATED
          List<Long> jobKeys = clone.getJobKeys();
          for (Long jobKey : jobKeys) {
            trace = tracesByJobKey.get(jobKey);
            trace.add(clone);
          }
        } else {
          throw new UnsupportedOperationException();
        }
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public void close() {
    timeRecorder.stop();
  }
}
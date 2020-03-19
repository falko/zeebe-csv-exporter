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
package io.zeebe.exporter.record;

import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.Intent;
import io.zeebe.protocol.record.intent.JobBatchIntent;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.JobBatchRecordValue;
import io.zeebe.protocol.record.value.JobRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;

public class TimeRecord {

  private final String workerId;
  private final RecordType recordType;
  private final ValueType valueType;
  private final Intent intent;
  private final Object value;
  private final long timestamp;

  public TimeRecord(final Record<?> record) {
    this.workerId = this.getWorkerId(record);
    this.recordType = record.getRecordType();
    this.valueType = record.getValueType();
    this.intent = record.getIntent();
    this.value = record.getValue(); // TODO extract all necessary data from value object
    this.timestamp = record.getTimestamp();
  }

  public String getWorkerId() {
    return workerId;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public ValueType getValueType() {
    return valueType;
  }

  public Intent getIntent() {
    return intent;
  }

  public Object getValue() {
    return value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isCommandJobBatchActivate() {
    return recordType == RecordType.COMMAND
        && valueType == ValueType.JOB_BATCH
        && intent == JobBatchIntent.ACTIVATE;
  }

  public boolean isEventJobActivated() {
    return recordType == RecordType.EVENT
        && valueType == ValueType.JOB
        && intent == JobIntent.ACTIVATED;
  }

  public String getRecordName() {
    final String recordName = recordType + ":" + valueType + ":" + intent;
    if (valueType == ValueType.WORKFLOW_INSTANCE) {
      final BpmnElementType bpmnElementType =
          ((WorkflowInstanceRecordValue) value).getBpmnElementType();
      return recordName + ":" + bpmnElementType;
    }
    return recordName;
  }

  private String getWorkerId(final Record<?> record) {
    if (record != null) {
      final Object value = record.getValue();
      if (value instanceof JobRecordValue) {
        return ((JobRecordValue) value).getWorker();
      }
      if (value instanceof JobBatchRecordValue) {
        return ((JobBatchRecordValue) value).getWorker();
      }
    }
    return null;
  }
}

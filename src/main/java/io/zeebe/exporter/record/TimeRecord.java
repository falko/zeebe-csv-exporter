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
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.JobBatchRecordValue;
import io.zeebe.protocol.record.value.JobRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import java.util.List;

public class TimeRecord {

  private final RecordType recordType;
  private final ValueType valueType;
  private final Intent intent;
  private final long key;
  private final long timestamp;
  private BpmnElementType bpmnElementType;
  private long elementInstanceKey;
  private List<Long> jobKeys;
  private String workerId;

  public TimeRecord(final Record<?> record) {
    this.recordType = record.getRecordType();
    this.valueType = record.getValueType();
    this.intent = record.getIntent();
    this.key = record.getKey();
    this.timestamp = record.getTimestamp();
    this.cloneValue(record);
  }

  public ValueType getValueType() {
    return valueType;
  }

  public Intent getIntent() {
    return intent;
  }

  public long getKey() {
    return key;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getElementInstanceKey() {
    return elementInstanceKey;
  }

  public List<Long> getJobKeys() {
    return jobKeys;
  }

  public String getWorkerId() {
    return workerId;
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

  public boolean isServiceTask() {
    return bpmnElementType == BpmnElementType.SERVICE_TASK;
  }

  public boolean isActivating() {
    return recordType == RecordType.EVENT && intent == WorkflowInstanceIntent.ELEMENT_ACTIVATING;
  }

  public String getRecordName() {
    final String recordName = recordType + ":" + valueType + ":" + intent;
    if (valueType == ValueType.WORKFLOW_INSTANCE) {
      return recordName + ":" + bpmnElementType;
    }
    return recordName;
  }

  private void cloneValue(final Record<?> record) {
    if (this.isWorkflowInstance(record)) {
      final WorkflowInstanceRecordValue recordValue =
          (WorkflowInstanceRecordValue) record.getValue();
      this.bpmnElementType = recordValue.getBpmnElementType();
    } else if (this.isJob(record)) {
      final JobRecordValue recordValue = (JobRecordValue) record.getValue();
      this.elementInstanceKey = recordValue.getElementInstanceKey();
      this.workerId = recordValue.getWorker();
    } else if (this.isJobBatch(record)) {
      final JobBatchRecordValue recordValue = (JobBatchRecordValue) record.getValue();
      this.jobKeys = recordValue.getJobKeys();
      this.workerId = recordValue.getWorker();
    }
  }

  private boolean isWorkflowInstance(final Record<?> record) {
    return record != null && record.getValue() instanceof WorkflowInstanceRecordValue;
  }

  private boolean isJob(final Record<?> record) {
    return record != null && record.getValue() instanceof JobRecordValue;
  }

  private boolean isJobBatch(final Record<?> record) {
    return record != null && record.getValue() instanceof JobBatchRecordValue;
  }

  @Override
  public String toString() {
    return "TimeRecord{"
        + "recordName="
        + this.getRecordName()
        + ", recordType="
        + recordType
        + ", valueType="
        + valueType
        + ", intent="
        + intent
        + ", key="
        + key
        + ", timestamp="
        + timestamp
        + ", bpmnElementType="
        + bpmnElementType
        + ", elementInstanceKey="
        + elementInstanceKey
        + ", jobKeys="
        + jobKeys
        + ", workerId='"
        + workerId
        + '\''
        + '}';
  }
}

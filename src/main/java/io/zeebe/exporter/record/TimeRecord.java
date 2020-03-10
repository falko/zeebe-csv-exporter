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
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;

public class TimeRecord {

  private final String recordName;
  private final long timestamp;

  public TimeRecord(final Record<?> record) {
    this(getName(record), record.getTimestamp());
  }

  public TimeRecord(final String recordName, final long timestamp) {
    this.recordName = recordName;
    this.timestamp = timestamp;
  }

  public String getRecordName() {
    return recordName;
  }

  public long getTimestamp() {
    return timestamp;
  }

  private static String getName(final Record<?> record) {
    final RecordType recordType = record.getRecordType();
    final ValueType valueType = record.getValueType();
    final Intent intent = record.getIntent();
    final String recordName = recordType + ":" + valueType + ":" + intent;
    if (valueType == ValueType.WORKFLOW_INSTANCE) {
      final BpmnElementType bpmnElementType =
          ((WorkflowInstanceRecordValue) record.getValue()).getBpmnElementType();
      return recordName + ":" + bpmnElementType;
    }
    return recordName;
  }

  @Override
  public String toString() {
    return "TimeRecord{" + "recordName='" + recordName + '\'' + ", timestamp=" + timestamp + '}';
  }
}

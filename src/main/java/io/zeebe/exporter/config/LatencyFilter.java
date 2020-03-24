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
package io.zeebe.exporter.config;

import static io.zeebe.exporter.api.context.Context.RecordFilter;

import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import java.util.Arrays;
import java.util.List;

public class LatencyFilter implements RecordFilter {

  private static final List<ValueType> EXPORT_VALUE_TYPE =
      Arrays.asList(ValueType.JOB, ValueType.WORKFLOW_INSTANCE, ValueType.JOB_BATCH);

  @Override
  public boolean acceptType(final RecordType recordType) {
    return true;
  }

  @Override
  public boolean acceptValue(final ValueType valueType) {
    return EXPORT_VALUE_TYPE.contains(valueType);
  }
}

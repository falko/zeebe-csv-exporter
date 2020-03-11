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

public class TimeAggregate {

  private long sumTime;
  private long minTime;
  private long maxTime;
  private long count;
  private String recordName;

  public TimeAggregate(String recordName, long time) {
    this.recordName = recordName;
    sumTime = minTime = maxTime = time;
    count = 1;
  }

  public void add(long time) {
    sumTime += time;
    if (time < minTime) {
      minTime = time;
    }
    if (time > maxTime) {
      maxTime = time;
    }
    ++count;
  }

  @Override
  public String toString() {
    return recordName
        + " avg="
        + sumTime / count
        + ", count="
        + count
        + ", min="
        + minTime
        + ", max="
        + maxTime;
  }

  public long getSumTime() {
    return sumTime;
  }

  public long getMinTime() {
    return minTime;
  }

  public long getMaxTime() {
    return maxTime;
  }

  public long getCount() {
    return count;
  }

  public String getRecordName() {
    return recordName;
  }
}

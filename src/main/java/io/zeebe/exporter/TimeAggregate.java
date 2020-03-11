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

  private double sumTime;
  private double minTime;
  private double maxTime;
  private double varTime;
  private double count;
  private String recordName;

  public TimeAggregate(String recordName, double time) {
    this.recordName = recordName;
    sumTime = minTime = maxTime = time;
    varTime = Math.pow(time * -1, 2);
    count = 1;
  }

  public void add(double time) {
    sumTime += time;
    varTime += Math.pow(time * -1, 2);
    if (time < minTime) {
      minTime = time;
    }
    if (time > maxTime) {
      maxTime = time;
    }
    ++count;
  }

  public double getMssd() {
    return varTime / (count * 2);
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
        + maxTime
        + ", mssd="
        + String.format("%f", this.getMssd());
  }

  public double getSumTime() {
    return sumTime;
  }

  public double getMinTime() {
    return minTime;
  }

  public double getMaxTime() {
    return maxTime;
  }

  public double getVarTime() {
    return varTime;
  }

  public double getCount() {
    return count;
  }

  public String getRecordName() {
    return recordName;
  }
}

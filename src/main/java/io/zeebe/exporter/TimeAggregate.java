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

  private final String currRecordName;
  private final String nextRecordName;
  private double sumTime;
  private double minTime;
  private double maxTime;
  private double varTime;
  private int count;

  public TimeAggregate(
      final String currRecordName, final String nextRecordName, final double time) {
    this.currRecordName = currRecordName;
    this.nextRecordName = nextRecordName;
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

  private double getMSSD() {
    return Math.sqrt(varTime / (count * 2));
  }

  private String getMSSDText() {
    return String.format("%f", this.getMSSD());
  }

  public String asCSV() {
    return currRecordName
        + ";"
        + nextRecordName
        + ";"
        + sumTime / count
        + ";"
        + count
        + ";"
        + minTime
        + ";"
        + maxTime
        + ";"
        + this.getMSSDText();
  }

  @Override
  public String toString() {
    return currRecordName
        + " next="
        + nextRecordName
        + " avg="
        + sumTime / count
        + ", count="
        + count
        + ", min="
        + minTime
        + ", max="
        + maxTime
        + ", mssd="
        + this.getMSSDText();
  }
}

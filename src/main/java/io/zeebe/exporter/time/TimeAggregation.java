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
package io.zeebe.exporter.time;

@SuppressWarnings("unused")
public class TimeAggregation {

  private final String currRecordName;
  private final String nextRecordName;
  private double sumTime;
  private double minTime;
  private double maxTime;
  private double varTime;
  private int count;

  public TimeAggregation(
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

  public String getCurrRecordName() {
    return currRecordName;
  }

  public String getNextRecordName() {
    return nextRecordName;
  }

  public double getMinTime() {
    return minTime;
  }

  public double getMaxTime() {
    return maxTime;
  }

  public int getCount() {
    return count;
  }

  public double getAVG() {
    return sumTime / count;
  }

  public String getMSSD() {
    return String.format("%f", Math.sqrt(varTime / (count * 2)));
  }

  public String asCSV() {
    return currRecordName
        + ";"
        + nextRecordName
        + ";"
        + this.getAVG()
        + ";"
        + count
        + ";"
        + minTime
        + ";"
        + maxTime
        + ";"
        + this.getMSSD();
  }

  @Override
  public String toString() {
    return currRecordName
        + " next="
        + nextRecordName
        + " avg="
        + this.getAVG()
        + ", count="
        + count
        + ", min="
        + minTime
        + ", max="
        + maxTime
        + ", mssd="
        + this.getMSSD();
  }
}

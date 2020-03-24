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

import io.zeebe.exporter.time.TimeAggregation;
import org.junit.Assert;
import org.junit.Test;

public class TimeAggregationTest {

  @Test
  public void testMSSD() {
    final TimeAggregation timeAggregation =
        new TimeAggregation("SOME_RECORD", "SOME_RECORD_2", 0.500 - 0.480);
    timeAggregation.add(0.480 - 0.490);
    timeAggregation.add(0.490 - 0.500);
    timeAggregation.add(0.500 - 0.505);
    timeAggregation.add(0.505 - 0.500);
    timeAggregation.add(0.500 - 0.490);
    timeAggregation.add(0.490 - 0.498);
    timeAggregation.add(0.498 - 0.500);
    timeAggregation.add(0.500 - 0.479);
    timeAggregation.add(0.479 - 0.490);
    timeAggregation.add(0.490 - 0.510);
    Assert.assertEquals("0.008995", timeAggregation.getMSSD());
  }
}

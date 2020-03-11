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

import org.junit.Assert;
import org.junit.Test;

public class TimeAggregateTest {

  @Test
  public void testMSSD() {
    final TimeAggregate timeAggregate = new TimeAggregate("SOME_RECORD", 0.500 - 0.480);
    timeAggregate.add(0.480 - 0.490);
    timeAggregate.add(0.490 - 0.500);
    timeAggregate.add(0.500 - 0.505);
    timeAggregate.add(0.505 - 0.500);
    timeAggregate.add(0.500 - 0.490);
    timeAggregate.add(0.490 - 0.498);
    timeAggregate.add(0.498 - 0.500);
    timeAggregate.add(0.500 - 0.479);
    timeAggregate.add(0.479 - 0.490);
    timeAggregate.add(0.490 - 0.510);
    Assert.assertEquals("0.000081", String.format("%f", timeAggregate.getMssd()));
  }
}

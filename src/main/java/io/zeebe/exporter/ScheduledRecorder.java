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

import io.zeebe.exporter.analysis.Analyzer;
import io.zeebe.exporter.record.TimeRecord;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ScheduledRecorder {

  private final int delay;

  private Timer timer;
  private final LinkedBlockingQueue<List<TimeRecord>> tracesQueue = new LinkedBlockingQueue<>();
  private final Analyzer analyzer;

  public ScheduledRecorder(final int delay, final Analyzer analyzer) {
    this.delay = delay;
    this.analyzer = analyzer;
  }

  public void offer(final List<TimeRecord> traceByInstanceKey) {
    tracesQueue.offer(traceByInstanceKey);
  }

  public void start() {
    timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            final List<List<TimeRecord>> traces = new ArrayList<>();
            tracesQueue.drainTo(traces);
            analyzer.analyze(traces);
          }
        },
        delay, // time to start to display the data
        delay); // delay time between each display
  }

  public void stop() {
    timer.cancel();
    timer = null;
  }
}

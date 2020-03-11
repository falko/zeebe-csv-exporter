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
import java.util.concurrent.ConcurrentHashMap;

public class ScheduledRecorder {

  private static final int DELAY = 600;

  private Timer timer;
  private final Map<Long, List<TimeRecord>> completed = new ConcurrentHashMap<>();
  private final Analyzer analyzer;

  public ScheduledRecorder(final Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  public void addCompleted(final Long key, final List<TimeRecord> tracesByElementInstanceKey) {
    completed.put(key, tracesByElementInstanceKey);
  }

  public void start() {
    timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            final Map<Long, List<TimeRecord>> copy = copyAndClear();
            // We're looking for an entire period of time here... if we
            // have the necessity to evaluate instance by instance, then
            // the list below should be removed.
            final List<TimeRecord> trace = new ArrayList<>();
            for (final Map.Entry<Long, List<TimeRecord>> entry : copy.entrySet()) {
              trace.addAll(entry.getValue());
            }
            analyzer.analyze(trace);
          }
        },
        DELAY, // time to start to display the data
        DELAY); // delay time between each display
  }

  public void stop() {
    timer.cancel();
    timer = null;
  }

  // We're removing just the keys that are being moved
  // to the log file
  private Map<Long, List<TimeRecord>> copyAndClear() {
    final Map<Long, List<TimeRecord>> copy = new HashMap<>(completed);
    completed.keySet().removeAll(copy.keySet());
    return copy;
  }
}

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

public class TimeRecorder {

  private Timer timer;
  private Map<Long, List<TimeRecord>> completed = new ConcurrentHashMap<>();
  private Analyzer analyzer;

  public TimeRecorder(final Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  public void add(final Long key, List<TimeRecord> tracesByElementInstanceKey) {
    this.completed.put(key, tracesByElementInstanceKey);
  }

  public void start() {
    timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            final Map<Long, List<TimeRecord>> copy = copyAndClear();
            final List<TimeRecord> trace = new ArrayList<>();
            for (final Map.Entry<Long, List<TimeRecord>> entry : copy.entrySet()) {
              trace.addAll(entry.getValue());
            }
            analyzer.check(trace);
          }
        },
        6000, // TODO: turn it configurable
        6000); // TODO: turn it configurable
  }

  public void stop() {
    timer.cancel();
    timer = null;
  }

  private Map<Long, List<TimeRecord>> copyAndClear() {
    final Map<Long, List<TimeRecord>> copy = new HashMap<>(completed);
    completed.keySet().removeAll(copy.keySet());
    return copy;
  }
}

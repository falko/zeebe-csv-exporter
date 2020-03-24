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

import io.zeebe.exporter.analysis.Analyzer;
import io.zeebe.exporter.config.Configuration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TimeRecorder {

  private final Configuration configuration;
  private final Timer timer;
  private final LinkedBlockingQueue<List<TimeRecord>> tracesQueue;
  private final Analyzer analyzer;

  public TimeRecorder(final Configuration configuration, final Analyzer analyzer) {
    this.configuration = configuration;
    this.timer = new Timer();
    this.tracesQueue = new LinkedBlockingQueue<>();
    this.analyzer = analyzer;
  }

  public void offer(final List<TimeRecord> traceByInstanceKey) {
    tracesQueue.offer(traceByInstanceKey);
  }

  public void start() {
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            final List<List<TimeRecord>> traces = new ArrayList<>();
            tracesQueue.drainTo(traces);
            analyzer.analyze(traces);
          }
        },
        0,
        configuration.getFixedRate());
  }

  public void stop() {
    timer.cancel();
  }
}

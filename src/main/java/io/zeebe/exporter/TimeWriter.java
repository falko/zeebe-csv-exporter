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

import io.zeebe.exporter.record.TimeRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class TimeWriter {

  private Timer timer;
  private Map<Long, List<TimeRecord>> tracesCompletedByElementInstanceKey =
      new ConcurrentHashMap<>();

  public void add(final Long key, List<TimeRecord> tracesByElementInstanceKey) {
    this.tracesCompletedByElementInstanceKey.put(key, tracesByElementInstanceKey);
  }

  public void start() {
    timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            // TODO: this seems insecure
            final Map<Long, List<TimeRecord>> completedTraces =
                new LinkedHashMap<>(tracesCompletedByElementInstanceKey);
            tracesCompletedByElementInstanceKey.clear();
            final List<TimeRecord> trace = new ArrayList<>();
            for (final Map.Entry<Long, List<TimeRecord>> entry : completedTraces.entrySet()) {
              trace.addAll(entry.getValue());
            }
            analyzeTrace(trace);
          }
        },
        6000, // TODO: turn it configurable
        6000); // TODO: turn it configurable
  }

  public void stop() {
    timer.cancel();
    timer = null;
  }

  // TODO: the format to be written shouldn't be here(maybe we should create another class)
  // TODO: do we want to group by instance key or by type????
  private void analyzeTrace(final List<TimeRecord> trace) {
    final Map<String, TimeAggregate> cache = new LinkedHashMap<>();
    for (final TimeRecord record : trace) {
      final int next = trace.indexOf(record) + 1;
      if (next < trace.size()) {
        final TimeRecord nextRecord = trace.get(next);
        long time = nextRecord.getTimestamp() - record.getTimestamp();
        if (cache.containsKey(record.getRecordName())) {
          final TimeAggregate timeAggregate = cache.get(record.getRecordName());
          timeAggregate.add(time);
        } else {
          cache.put(record.getRecordName(), new TimeAggregate(record.getRecordName(), time));
        }
      }
      final TimeAggregate timeAggregate = cache.get(record.getRecordName());
      if (timeAggregate != null) {
        System.out.println(timeAggregate);
      }
    }
  }
}

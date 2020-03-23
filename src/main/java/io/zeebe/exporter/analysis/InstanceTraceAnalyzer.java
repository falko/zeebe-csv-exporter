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
package io.zeebe.exporter.analysis;

import io.zeebe.exporter.TimeAggregate;
import io.zeebe.exporter.record.TimeRecord;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;

public class InstanceTraceAnalyzer implements Analyzer {

  private final Map<String, TimeAggregate> timeDiffCache = new LinkedHashMap<>();
  private static final String CSV_HEADER = "Event;AVG;Count;Min;Max;MSSD";

  private final Logger logger;

  public InstanceTraceAnalyzer(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public void analyze(final List<List<TimeRecord>> traces) {
    for (final List<TimeRecord> trace : traces) {
      if (trace.isEmpty()) {
        logger.error("Unexpected empty trace in analysis");
        continue;
      }
      trace.sort(Comparator.comparingLong(TimeRecord::getTimestamp));

      for (final TimeRecord currRecord : trace) {
        final int nextIndex = trace.indexOf(currRecord) + 1;
        // Ignore all COMMAND:JOB_BATCH:ACTIVATE to process
        // when we have a corresponding EVENT:JOB:ACTIVATED
        if (nextIndex < trace.size() && !currRecord.isCommandJobBatchActivate()) {
          if (currRecord.isEventJobActivated()) {
            this.pushJobBatchTimeDiff(timeDiffCache, trace, currRecord, nextIndex - 1);
          }
          final TimeRecord nextRecord = trace.get(nextIndex);
          // Push new aggregated data to the cache, or update
          // the aggregated data by event type
          this.pushTimeDiff(timeDiffCache, currRecord, nextRecord);
        }
      }
    }
    // Show processed data until now
    this.showData(timeDiffCache);
  }

  private void showData(final Map<String, TimeAggregate> timeDiffCache) {
    if (!timeDiffCache.isEmpty()) {
      logger.info(CSV_HEADER);
      for (final Entry<String, TimeAggregate> entry : timeDiffCache.entrySet()) {
        logger.info(entry.getValue().toCsvRow());
      }
    }
  }

  // Just look backwards in the trace to find the
  // matching COMMAND:JOB_BATCH:ACTIVATE for a
  // EVENT:JOB:ACTIVATED. It starts in the same index
  // of the EVENT:JOB:ACTIVATED event
  private void pushJobBatchTimeDiff(
      final Map<String, TimeAggregate> timeDiffCache,
      final List<TimeRecord> trace,
      final TimeRecord currRecord,
      final int currIndex) {
    if (currRecord.getWorkerId() != null) {
      for (int index = currIndex; index > 0; index--) {
        final TimeRecord candidateRecord = trace.get(index);
        if (candidateRecord.isCommandJobBatchActivate()
            && candidateRecord.getWorkerId() != null
            && candidateRecord.getWorkerId().equals(currRecord.getWorkerId())) {
          final TimeRecord nextRecord = trace.get(index + 1);
          this.pushTimeDiff(timeDiffCache, candidateRecord, nextRecord);
          break;
        }
      }
    }
  }

  // Just sum the values
  private void pushTimeDiff(
      final Map<String, TimeAggregate> cache,
      final TimeRecord currRecord,
      final TimeRecord nextRecord) {
    final String recordName = currRecord.getRecordName();
    final long time = nextRecord.getTimestamp() - currRecord.getTimestamp();
    if (cache.containsKey(recordName)) {
      final TimeAggregate timeAggregate = cache.get(recordName);
      timeAggregate.add(time);
    } else {
      cache.put(recordName, new TimeAggregate(recordName, time));
    }
  }
}

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

import static io.zeebe.exporter.writer.CsvFileWriter.CSV_HEADERS;

import io.zeebe.exporter.config.Configuration;
import io.zeebe.exporter.time.TimeAggregation;
import io.zeebe.exporter.time.TimeRecord;
import io.zeebe.exporter.writer.CsvFileWriter;
import io.zeebe.exporter.writer.CsvWriter;
import java.util.*;
import java.util.Map.Entry;
import org.slf4j.Logger;

public class InstanceTraceAnalyzer implements Analyzer {

  private final Map<String, TimeAggregation> timeDiffCache = new LinkedHashMap<>();

  private final Configuration configuration;
  private final Logger logger;

  public InstanceTraceAnalyzer(final Configuration configuration, final Logger logger) {
    this.configuration = configuration;
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
    this.write(timeDiffCache);
  }

  private void write(final Map<String, TimeAggregation> timeDiffCache) {
    if (!timeDiffCache.isEmpty()) {
      logger.info(String.join(";", CSV_HEADERS));
      for (final Entry<String, TimeAggregation> entry : timeDiffCache.entrySet()) {
        final TimeAggregation timeAggregation = entry.getValue();
        logger.info(timeAggregation.asCSV());
      }
      if (configuration.getCsvEnabled()) {
        final CsvWriter csvWriter = new CsvFileWriter(configuration);
        for (final Entry<String, TimeAggregation> entry : timeDiffCache.entrySet()) {
          final TimeAggregation timeAggregation = entry.getValue();
          if (configuration.getCsvEnabled()) {
            csvWriter.write(timeAggregation);
          }
        }
      }
    }
  }

  // Just look backwards in the trace to find the
  // matching COMMAND:JOB_BATCH:ACTIVATE for a
  // EVENT:JOB:ACTIVATED. It starts in the same index
  // of the EVENT:JOB:ACTIVATED event
  private void pushJobBatchTimeDiff(
      final Map<String, TimeAggregation> timeDiffCache,
      final List<TimeRecord> trace,
      final TimeRecord currRecord,
      final int currIndex) {
    if (currRecord.getWorkerId() != null) {
      boolean backward = this.findBackward(timeDiffCache, trace, currRecord, currIndex);
      if (!backward) {
        this.findForehead(timeDiffCache, trace, currRecord, currIndex);
      }
    }
  }

  private boolean findBackward(
      final Map<String, TimeAggregation> timeDiffCache,
      final List<TimeRecord> trace,
      final TimeRecord currRecord,
      final int currIndex) {
    for (int index = currIndex; index >= 0; index--) {
      final TimeRecord candidateRecord = trace.get(index);
      if (candidateRecord.isCommandJobBatchActivate()
          && candidateRecord.getWorkerId() != null
          && candidateRecord.getWorkerId().equals(currRecord.getWorkerId())) {
        this.pushTimeDiff(timeDiffCache, candidateRecord, currRecord);
        return true;
      }
    }
    return false;
  }

  private void findForehead(
      final Map<String, TimeAggregation> timeDiffCache,
      final List<TimeRecord> trace,
      final TimeRecord currRecord,
      final int currIndex) {
    for (int index = currIndex + 1; index < trace.size(); index++) {
      final TimeRecord candidateRecord = trace.get(index);
      if (candidateRecord.isCommandJobBatchActivate()
          && candidateRecord.getWorkerId() != null
          && candidateRecord.getWorkerId().equals(currRecord.getWorkerId())) {
        this.pushTimeDiff(timeDiffCache, candidateRecord, currRecord);
        break;
      }
    }
  }

  // Just sum the values
  private void pushTimeDiff(
      final Map<String, TimeAggregation> cache,
      final TimeRecord currRecord,
      final TimeRecord nextRecord) {
    final String currRecordName = currRecord.getRecordName();
    final String nextRecordName = nextRecord.getRecordName();
    final long time = Math.abs(nextRecord.getTimestamp() - currRecord.getTimestamp());
    if (cache.containsKey(currRecordName)) {
      final TimeAggregation timeAggregation = cache.get(currRecordName);
      timeAggregation.add(time);
    } else {
      final TimeAggregation timeAggregation =
          new TimeAggregation(currRecordName, nextRecordName, time);
      cache.put(currRecordName, timeAggregation);
    }
  }
}

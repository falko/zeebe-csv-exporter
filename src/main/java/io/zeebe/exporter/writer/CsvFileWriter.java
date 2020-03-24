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
package io.zeebe.exporter.writer;

import static java.nio.file.StandardOpenOption.*;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.zeebe.exporter.config.Configuration;
import io.zeebe.exporter.time.TimeAggregation;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CsvFileWriter implements CsvWriter {

  public static final String[] CSV_HEADERS = {
    "currRecordName", "nextRecordName", "avg", "count", "minTime", "maxTime", "mssd"
  };
  private static final String SUFFIX = ".csv";
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  private final Path output;
  private final ObjectWriter objectWriter;
  private SequenceWriter sequenceWriter;

  public CsvFileWriter(final Configuration configuration) {
    this.output = this.createOutput(configuration);
    final CsvMapper mapper = new CsvMapper();
    final CsvSchema schema =
        mapper.schemaFor(TimeAggregation.class).sortedBy(CSV_HEADERS).withHeader();
    this.objectWriter = mapper.writer(schema);
  }

  @Override
  public void write(final TimeAggregation aggregation) {
    try {
      if (sequenceWriter == null) {
        final Path path = output.resolve(TIME_FORMATTER.format(LocalDateTime.now()) + SUFFIX);
        final OpenOption openOption = Files.exists(path) ? TRUNCATE_EXISTING : CREATE_NEW;
        final BufferedWriter bufferedWriter =
            Files.newBufferedWriter(path, StandardCharsets.UTF_8, openOption);
        sequenceWriter = objectWriter.writeValues(bufferedWriter);
      }
      sequenceWriter.write(aggregation);
    } catch (final Exception e) {
      e.printStackTrace(); // TODO: take care of it
    }
  }

  @Override
  public void close() throws Exception {
    if (sequenceWriter != null) {
      sequenceWriter.close();
    }
  }

  private Path createOutput(final Configuration configuration) {
    try {
      final Path path = Paths.get(configuration.getCsvOutput()).toAbsolutePath();
      if (!Files.exists(path)) {
        return Files.createDirectories(path);
      }
      return path;
    } catch (final Exception e) {
      e.printStackTrace(); // TODO: take care of it
      return null;
    }
  }
}

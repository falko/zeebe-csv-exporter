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
package io.zeebe.exporter.config;

import io.zeebe.exporter.api.context.Context;
import java.util.Map;

public class Configuration {

  private static final String DEFAULT_CSV_OUTPUT = "command-event-latency/";
  private static final Integer DEFAULT_FIXED_RATE = 60;
  private static final Boolean DEFAULT_CSV_ENABLED = false;

  private static final String CSV_OUTPUT = "csvOutput";
  private static final String FIXED_RATE = "fixedRate";
  private static final String CSV_ENABLED = "csvEnabled";

  private final String csvOutput;
  private final Integer fixedRate;
  private final Boolean csvEnabled;

  public Configuration(final Context context) {
    this.csvOutput = this.getCsvOutput(context);
    this.fixedRate = this.getFixedRate(context) * 1000;
    this.csvEnabled = this.getCsvEnabled(context);
  }

  public Configuration(final String csvOutput, final Integer fixedRate, final Boolean csvEnabled) {
    this.csvOutput = csvOutput;
    this.fixedRate = fixedRate;
    this.csvEnabled = csvEnabled;
  }

  public String getCsvOutput() {
    return csvOutput;
  }

  public Integer getFixedRate() {
    return fixedRate;
  }

  public Boolean getCsvEnabled() {
    return csvEnabled;
  }

  private String getCsvOutput(final Context context) {
    final Object output = this.getArgument(context, CSV_OUTPUT);
    return output != null ? (String) output : DEFAULT_CSV_OUTPUT;
  }

  private int getFixedRate(final Context context) {
    final Object fixedRate = this.getArgument(context, FIXED_RATE);
    return fixedRate != null ? ((Number) fixedRate).intValue() : DEFAULT_FIXED_RATE;
  }

  private Boolean getCsvEnabled(final Context context) {
    final Object output = this.getArgument(context, CSV_ENABLED);
    return output != null ? (Boolean) output : DEFAULT_CSV_ENABLED;
  }

  private Object getArgument(final Context context, final String key) {
    final Map<String, Object> arguments = context.getConfiguration().getArguments();
    return arguments != null ? arguments.get(key) : null;
  }
}

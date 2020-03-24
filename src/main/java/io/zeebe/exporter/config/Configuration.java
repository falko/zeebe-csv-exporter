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

  private static final String DEFAULT_OUTPUT = "latency/";
  private static final int DEFAULT_FIXED_RATE = 60;

  private static final String OUTPUT = "output";
  private static final String FIXED_RATE = "fixedRate";

  private final String output;
  private final Integer fixedRate;

  public Configuration(final Context context) {
    this.output = this.getOutput(context);
    this.fixedRate = this.getFixedRate(context) * 1000;
  }

  public Configuration(final String output, final int fixedRate) {
    this.output = output;
    this.fixedRate = fixedRate;
  }

  public String getOutput() {
    return output;
  }

  public Integer getFixedRate() {
    return fixedRate;
  }

  private String getOutput(final Context context) {
    final Object output = this.getArgument(context, OUTPUT);
    return output != null ? (String) output : DEFAULT_OUTPUT;
  }

  private int getFixedRate(final Context context) {
    final Object fixedRate = this.getArgument(context, FIXED_RATE);
    return fixedRate != null ? ((Number) fixedRate).intValue() : DEFAULT_FIXED_RATE;
  }

  private Object getArgument(final Context context, final String key) {
    final Map<String, Object> arguments = context.getConfiguration().getArguments();
    return arguments != null ? arguments.get(key) : null;
  }
}

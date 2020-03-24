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

import io.zeebe.exporter.config.Configuration;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// TODO: write some tests
public class CommandEventLatencyExporterIT {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ExporterIntegrationRule integrationRule;

  @Before
  public void setUp() {
    final Configuration configuration = new Configuration("command-event-latency/", 1, false);
    integrationRule =
        new ExporterIntegrationRule()
            .configure("latency", CommandEventLatencyExporter.class, configuration);
    integrationRule.start();
  }

  @After
  public void tearDown() {
    integrationRule.stop();
  }

  @Test
  public void shouldExportRecords() throws Exception {
    // when
    integrationRule.performSampleWorkload();

    // waiting for timer task
    TimeUnit.MILLISECONDS.sleep(100);

    // then
    assertRecords();
  }

  private void assertRecords() {
    // TODO:
  }
}

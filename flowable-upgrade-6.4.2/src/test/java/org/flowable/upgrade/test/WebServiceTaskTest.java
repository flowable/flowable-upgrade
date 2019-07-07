/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.upgrade.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.webservice.MockWebServiceContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
@RunOnlyWithTestDataFromVersion(versions = "6.4.0")
public class WebServiceTaskTest extends UpgradeTestCase {

    private MockWebServiceContext mockWebServiceContext = MockWebServiceContext.create(MockWebServiceContext.WEBSERVICE_MOCK_ADDRESS);

    @Before
    public void initializeWebService() {
        mockWebServiceContext.start();
    }

    @After
    public void shutdownWebService() {
        if (mockWebServiceContext != null) {
            mockWebServiceContext.stopIfStarted();
        }
    }

    @Test
    public void testWebServiceInvocation() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("webServiceInvocation")
            .processInstanceBusinessKey("webServiceInvocation")
            .singleResult();

        assertThat(runtimeService.getVariables(processInstance.getId()))
            .containsOnly(
                entry("initialVariable", "initial")
            );

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .includeProcessVariables()
            .singleResult();
        assertThat(historicProcessInstance.getProcessVariables())
            .containsOnly(
                entry("initialVariable", "initial")
            );
    }

    @Test
    public void testWebServiceInvocationDataStructure() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("webServiceInvocationDataStructure")
            .processInstanceBusinessKey("webServiceInvocationDataStructure")
            .singleResult();

        Date startDate = Date.from(ZonedDateTime.of(LocalDate.of(2015, Month.APRIL, 23), LocalTime.MIDNIGHT, ZoneId.systemDefault()).toInstant());

        assertThat(runtimeService.getVariables(processInstance.getId()))
            .containsOnly(
                entry("initialVariable", "initial"),
                entry("startDate", startDate)
            );

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .includeProcessVariables()
            .singleResult();
        assertThat(historicProcessInstance.getProcessVariables())
            .containsOnly(
                entry("initialVariable", "initial"),
                entry("startDate", startDate)
            );
    }

}

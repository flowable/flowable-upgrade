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
package org.activiti.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.upgrade.helper.UpgradeUtil;
import org.flowable.upgrade.webservice.MockWebServiceContext;

/**
 * @author Joram Barrez
 */
public class DataGenerator {

    public static void main(String[] args) {
        ProcessEngine processEngine = UpgradeUtil.getProcessEngine();
        createCommonData(processEngine);
        // System.exit is needed because the cxf Server keeps the thread alive for some reason
        System.exit(0);
    }

    private static void createCommonData(ProcessEngine processEngine) {
        generateSimplestTaskData(processEngine);
        generateTaskWithExecutionVariableskData(processEngine);
        generateCallActivityData(processEngine);
        generateWebServicesData(processEngine);
    }

    private static void generateCallActivityData(ProcessEngine processEngine) {
        RuntimeService runtimeService = processEngine.getRuntimeService();

        processEngine.getRepositoryService().createDeployment()
            .name("callActivityProcess")
            .addClasspathResource("org/flowable/upgrade/test/CallSimpleSubProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/upgrade/test/CalledProcess.bpmn20.xml")
            .deploy();

        runtimeService.startProcessInstanceByKey("callSimpleSubProcess", "callSimpleSubProcess");
    }

    private static void generateTaskWithExecutionVariableskData(ProcessEngine processEngine) {
        RuntimeService runtimeService = processEngine.getRuntimeService();

        processEngine.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testTaskWithExecutionVariables.bpmn20.xml")
            .deploy();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("instrument", "trumpet");
        variables.put("player", "gonzo");
        runtimeService.startProcessInstanceByKey("taskWithExecutionVariablesProcess", variables);
    }

    private static void generateSimplestTaskData(ProcessEngine processEngine) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        TaskService taskService = processEngine.getTaskService();

        processEngine.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testSimplestTask.bpmn20.xml")
            .deploy();

        runtimeService.startProcessInstanceByKey("simpleTaskProcess", "changeAssignee");
        runtimeService.startProcessInstanceByKey("simpleTaskProcess", "changeOwner");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleTaskProcess", "completeTask");
        String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
        taskService.complete(taskId);
    }

    private static void generateWebServicesData(ProcessEngine processEngine) {
        MockWebServiceContext mockWebServiceContext = null;
        try {
            mockWebServiceContext = MockWebServiceContext.create(MockWebServiceContext.WEBSERVICE_MOCK_ADDRESS);
            mockWebServiceContext.start();

            processEngine.getRepositoryService()
                .createDeployment()
                .name("webServices")
                .addClasspathResource("org/flowable/upgrade/test/WebServiceTaskTest.testWebServiceInvocation.bpmn20.xml")
                .addClasspathResource("org/flowable/upgrade/test/WebServiceTaskTest.testWebServiceInvocationDataStructure.bpmn20.xml")
                .deploy();

            RuntimeService runtimeService = processEngine.getRuntimeService();

            ProcessInstance processInstance1 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("webServiceInvocation")
                .businessKey("webServiceInvocation")
                .variable("initialVariable", "initial")
                .start();
            Date startDate = Date.from(ZonedDateTime.of(LocalDate.of(2015, Month.APRIL, 23), LocalTime.MIDNIGHT, ZoneId.systemDefault()).toInstant());
            ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("webServiceInvocationDataStructure")
                .businessKey("webServiceInvocationDataStructure")
                .variable("startDate", startDate)
                .variable("initialVariable", "initial")
                .start();

            assertThat(runtimeService.getVariables(processInstance1.getId()))
                .containsOnly(
                    entry("initialVariable", "initial"),
                    entry("org.flowable.engine.impl.bpmn.CURRENT_MESSAGE", null)
                );
            assertThat(runtimeService.getVariables(processInstance2.getId()))
                .containsOnly(
                    entry("initialVariable", "initial"),
                    entry("startDate", startDate),
                    entry("dataInputOfServiceTaskRequest", null),
                    entry("org.flowable.engine.impl.bpmn.CURRENT_MESSAGE", null)
                );
        } finally {
            if (mockWebServiceContext != null) {
                mockWebServiceContext.stopIfStarted();
            }
        }
    }
}

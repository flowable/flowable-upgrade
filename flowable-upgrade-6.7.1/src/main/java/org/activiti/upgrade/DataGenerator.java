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

import java.util.HashMap;
import java.util.Map;

import org.flowable.app.engine.AppEngine;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.upgrade.helper.UpgradeUtil;

/**
 * @author Joram Barrez
 */
public class DataGenerator {
    
    public static void main(String[] args) {
        AppEngine appEngine = UpgradeUtil.getAppEngine();
        ProcessEngineConfigurationImpl processEngineConfiguration = createCommonData(appEngine);
        createEventRegistryData(
                (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations().get(ScopeTypes.EVENT_REGISTRY));
        // System.exit is needed because the cxf Server keeps the thread alive for some reason
        System.exit(0);
    }

    private static ProcessEngineConfigurationImpl createCommonData(AppEngine appEngine) {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) appEngine.getAppEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        
        generateSimplestTaskData(processEngineConfiguration);
        generateTaskWithExecutionVariableskData(processEngineConfiguration);
        generateCallActivityData(processEngineConfiguration);
        
        return processEngineConfiguration;
    }

    private static void generateCallActivityData(ProcessEngineConfigurationImpl processEngineConfiguration) {
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();

        processEngineConfiguration.getRepositoryService().createDeployment()
            .name("callActivityProcess")
            .addClasspathResource("org/flowable/upgrade/test/CallSimpleSubProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/upgrade/test/CalledProcess.bpmn20.xml")
            .deploy();

        runtimeService.startProcessInstanceByKey("callSimpleSubProcess", "callSimpleSubProcess");
    }

    private static void generateTaskWithExecutionVariableskData(ProcessEngineConfigurationImpl processEngineConfiguration) {
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();

        processEngineConfiguration.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testTaskWithExecutionVariables.bpmn20.xml")
            .deploy();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("instrument", "trumpet");
        variables.put("player", "gonzo");
        runtimeService.startProcessInstanceByKey("taskWithExecutionVariablesProcess", variables);
    }

    private static void generateSimplestTaskData(ProcessEngineConfigurationImpl processEngineConfiguration) {
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();

        processEngineConfiguration.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testSimplestTask.bpmn20.xml")
            .deploy();

        runtimeService.startProcessInstanceByKey("simpleTaskProcess", "changeAssignee");
        runtimeService.startProcessInstanceByKey("simpleTaskProcess", "changeOwner");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleTaskProcess", "completeTask");
        String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
        taskService.complete(taskId);
    }

    private static void createEventRegistryData(EventRegistryEngineConfiguration engineConfiguration) {
        EventRepositoryService repositoryService = engineConfiguration.getEventRepositoryService();
        repositoryService.createInboundChannelModelBuilder()
                .key("jmsInbound")
                .jmsChannelAdapter("test-customer")
                .eventProcessingPipeline()
                .jsonDeserializer()
                .fixedEventKey("test")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        repositoryService.createInboundChannelModelBuilder()
                .key("rabbitInbound")
                .rabbitChannelAdapter("test-customer")
                .eventProcessingPipeline()
                .jsonDeserializer()
                .fixedEventKey("test")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        repositoryService.createOutboundChannelModelBuilder()
                .key("jmsOutbound")
                .jmsChannelAdapter("order")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        repositoryService.createOutboundChannelModelBuilder()
                .key("rabbitOutbound")
                .rabbitChannelAdapter("order")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();
    }

}

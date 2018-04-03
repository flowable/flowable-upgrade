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

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.upgrade.helper.UpgradeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DataGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataGenerator.class);

    public static void main(String[] args) {
        ProcessEngine processEngine = UpgradeUtil.getProcessEngine();
        createCommonData(processEngine);
        create621Data(processEngine);
    }

    private static void createCommonData(ProcessEngine processEngine) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        TaskService taskService = processEngine.getTaskService();

        processEngine.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testSimplestTask.bpmn20.xml")
            .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleTaskProcess");
        String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
        taskService.complete(taskId);

        processEngine.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testTaskWithExecutionVariables.bpmn20.xml") 
            .deploy();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("instrument", "trumpet");
        variables.put("player", "gonzo");
        runtimeService.startProcessInstanceByKey("taskWithExecutionVariablesProcess", variables);
    }
    
    private static void create621Data(ProcessEngine processEngine) {
        LOGGER.info("Generating 6.2.1 specific data");
        
        RepositoryService repositoryService = processEngine.getRepositoryService();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        TaskService taskService = processEngine.getTaskService();
        
        repositoryService.createDeployment()
            .name("entityCountingTest")
            .addClasspathResource("org/flowable/upgrade/test/EntityCountingTest.testCounts.bpmn20.xml")
            .deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testEntityCounts");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        
        taskService.addUserIdentityLink(task.getId(), "someUser", IdentityLinkType.PARTICIPANT); 
        
        String executionId = task.getExecutionId();
        runtimeService.setVariableLocal(executionId, "someVariable", "someValue");
    }

}

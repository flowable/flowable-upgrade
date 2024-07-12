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
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.upgrade.helper.UpgradeUtil;

/**
 * @author Joram Barrez
 */
public class DataGenerator {

    public static void main(String[] args) {
        AppEngine appEngine = UpgradeUtil.getAppEngine();
        createCommonData(appEngine);
    }

    private static void createCommonData(AppEngine appEngine) {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) appEngine.getAppEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();

        processEngineConfiguration.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testSimplestTask.bpmn20.xml")
            .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleTaskProcess");
        String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
        taskService.complete(taskId);

        processEngineConfiguration.getRepositoryService().createDeployment()
            .name("simpleTaskProcess")
            .addClasspathResource("org/flowable/upgrade/test/UserTaskBeforeTest.testTaskWithExecutionVariables.bpmn20.xml") 
            .deploy();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("instrument", "trumpet");
        variables.put("player", "gonzo");
        runtimeService.startProcessInstanceByKey("taskWithExecutionVariablesProcess", variables);
    }

}

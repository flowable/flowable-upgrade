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

import static org.junit.Assert.assertEquals;

import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.task.service.Task;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RunOnlyWithTestDataFromVersion(versions = {"5.17.0", "5.18.0", "5.19.0", "5.20.0", "5.21.0", "5.22.0"})
public class UpgradeTaskOneTest extends UpgradeTestCase {

  @Test
  public void testSimplestTask() {
    RuntimeService runtimeService = processEngine.getRuntimeService();
    TaskService taskService = processEngine.getTaskService();
    ManagementService managementService = processEngine.getManagementService();
    HistoryService historyService = processEngine.getHistoryService();
    DynamicBpmnService dynamicBpmnService = processEngine.getDynamicBpmnService();

    Task task = taskService
      .createTaskQuery()
      .taskName("simpleTask2")
      .singleResult();
    
    String processInstanceId = task.getProcessInstanceId();
    
    long expectedHistoryTaskInstances = -1;
    String schemaHistory = managementService.getProperties().get("schema.history");
    if (schemaHistory.startsWith("create(5.0)")) {
      expectedHistoryTaskInstances = 0;
    } else {
      expectedHistoryTaskInstances = 2;
    }
    
    assertEquals(expectedHistoryTaskInstances, 
      historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstanceId)
        .orderByTaskName().asc()
        .count());
    
    ObjectNode infoNode = dynamicBpmnService.changeUserTaskName("simpleTask3", "Changed simple task 3");
    dynamicBpmnService.saveProcessDefinitionInfo(task.getProcessDefinitionId(), infoNode);
      
    taskService.complete(task.getId());
    
    assertEquals(1, runtimeService
            .createExecutionQuery()
            .processInstanceId(processInstanceId)
            .list()
            .size());

    assertEquals(expectedHistoryTaskInstances+1, 
            historyService.createHistoricTaskInstanceQuery()
              .processInstanceId(processInstanceId)
              .orderByTaskName().asc()
              .count());
            
    task = taskService
      .createTaskQuery()
      .taskName("Changed simple task 3")
      .singleResult();

    taskService.complete(task.getId());

    assertEquals(0, runtimeService
            .createExecutionQuery()
            .processInstanceId(processInstanceId)
            .list()
            .size());

    assertEquals(expectedHistoryTaskInstances+1, 
            historyService.createHistoricTaskInstanceQuery()
              .processInstanceId(processInstanceId)
              .orderByTaskName().asc()
              .count());
    
    // Cleanup
	  Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey("simpleTaskProcess").singleResult();
	  repositoryService.deleteDeployment(deployment.getId(), true);
  }
}

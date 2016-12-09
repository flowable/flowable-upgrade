package org.flowable.upgrade.test;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.activiti.engine.ActivitiException;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.test.helper.UpgradeTestCase;
import org.junit.Test;

/**
 * Suspending and activating process definitions and process instances was added
 * in Activiti 5.11. This class tests this newly added functionality.
 * 
 * @author Joram
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.7", "5.8", "5.9", "5.10"})
public class SuspendAndActivateFunctionalityTest extends UpgradeTestCase {

	@Test
	public void testProcessSuspension() {

		// PROCESS DEFINITION SUSPENSION
		
    ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("suspendAndActivate")
            .singleResult();
    assertNotNull(processDefinition);
    assertFalse(processDefinition.isSuspended());

    assertEquals(5, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().count());
    assertEquals(0, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).suspended().count());

    // Suspend
    processEngine.getRepositoryService().suspendProcessDefinitionById(processDefinition.getId(), true, null);
    try {
      processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());
    } catch (ActivitiException e) {
      assertTrue(e.getMessage().toLowerCase().contains("suspend"));
    }

    // Verify process instances
    assertEquals(0, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().count());
    assertEquals(5, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).suspended().count());

    // Activate again
    processEngine.getRepositoryService().activateProcessDefinitionById(processDefinition.getId(), true, null);
    assertEquals(5, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().count());
    assertEquals(0, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).suspended().count());

    
    // PROCESS INSTANCE SUSPENSION
    
    processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("suspendAndActivate")
            .singleResult();
    assertNotNull(processDefinition);
    assertFalse(processDefinition.isSuspended());

    assertEquals(5, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().count());
    assertEquals(0, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).suspended().count());

    ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list()
            .get(0);
    assertFalse(processInstance.isSuspended());

    // Suspend process instance
    processEngine.getRuntimeService().suspendProcessInstanceById(processInstance.getId());
    assertEquals(4, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).active().count());
    assertEquals(1, processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).suspended().count());

    try {
      Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
      processEngine.getTaskService().complete(task.getId());
    } catch (ActivitiException e) {
      assertTrue(e.getMessage().toLowerCase().contains("suspend"));
    }

    // Activate again
    processEngine.getRuntimeService().activateProcessInstanceById(processInstance.getId());

    // Cleanup
	  Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey("suspendAndActivate").singleResult();
	  repositoryService.deleteDeployment(deployment.getId(), true);
    
  }

}

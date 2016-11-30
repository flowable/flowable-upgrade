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
package org.activiti.upgrade.test;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.21.0", "5.22.0"})
public class Activiti5ToV6SuspendProcessInstanceTest extends UpgradeTestCase {

	@Test
	public void testActivateProcess() {

		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
		    .processDefinitionKey("suspendProcess-activiti5").singleResult();
		Assert.assertTrue(processInstance.isSuspended());

		runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());
		processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("suspendProcess-activiti5").singleResult();
		Assert.assertFalse(processInstance.isSuspended());

		Task task = taskService.createTaskQuery().taskName("The famous task").singleResult();
		taskService.complete(task.getId());

		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
		    .processInstanceId(processInstance.getProcessInstanceId()).singleResult();

		Assert.assertNotNull(historicProcessInstance.getEndTime());
	}

}

package org.activiti.upgrade.test;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.Test;

/**
 * This is an upgrade test added for the 5.15 release. In that release, we've
 * added a database column to store the create and last updated time for a historic variable.
 * 
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.14"})
public class HistoricVariableTimeStoreTest extends UpgradeTestCase {

  @Test
  public void testCreateAndLastUpdatedTimeStamp() {
  	ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
  			.processDefinitionKey("historicVariableTimeStoreTestProcess").singleResult();
  	HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
  			.processInstanceId(processInstance.getId()).singleResult();
  	
  	// In Activiti <5.14, no create or last update time was available
  	assertNotNull(historicVariableInstance);
  	assertNull(historicVariableInstance.getCreateTime());
  	assertNull(historicVariableInstance.getLastUpdatedTime());
  	
  	// In Activiti 5.15+, create time and last update time should be set
  	runtimeService.setVariable(processInstance.getId(), "myVar", "newValue");
  	historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
  			.processInstanceId(processInstance.getId()).singleResult();
  	assertNull(historicVariableInstance.getCreateTime());
  	assertNotNull(historicVariableInstance.getLastUpdatedTime());
  	
  	runtimeService.setVariable(processInstance.getId(), "newVar", "someValue");
  	historyService.createHistoricVariableInstanceQuery()
			.processInstanceId(processInstance.getId())
			.variableName("newVar")
			.singleResult();
  	assertNull(historicVariableInstance.getCreateTime());
  	assertNotNull(historicVariableInstance.getLastUpdatedTime());
  	
  	
  	 // Cleanup
	  Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey("historicVariableTimeStoreTestProcess").singleResult();
	  repositoryService.deleteDeployment(deployment.getId(), true);
  }

}

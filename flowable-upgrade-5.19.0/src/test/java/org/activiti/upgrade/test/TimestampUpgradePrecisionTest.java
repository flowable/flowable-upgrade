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

import java.util.List;

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.EventSubscriptionQueryImpl;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.task.Task;
import org.activiti.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.Assert;
import org.junit.Test;


/**
 * This is an upgrade test added for the 5.15 release. In that release, 
 * we've upgraded the timestamp columns for mysql for ms precision.
 * 
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.14"})
public class TimestampUpgradePrecisionTest extends UpgradeTestCase {

  @Test
  public void testCreateAndLastUpdatedTimeStamp() {
	  
	  // Find process definition
	  ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
			  .processDefinitionKey("testTimestampPrecisionUpgrade").singleResult();
	  
	  // After upgrade, there should be three jobs availble.
	  //One has a lock time set
	  
	  List<Job> jobs = managementService.createJobQuery().processDefinitionId(processDefinition.getId()).list();
	  Assert.assertEquals(6, jobs.size());
	  boolean oneHasLockDate = false;
	  int nrWithDueDate = 0;
	  int nrWithoutDuedate = 0;
	  for (Job job : jobs) {
		  
		  JobEntity jobEntity = (JobEntity) job;
		  if (jobEntity.getLockExpirationTime() != null && !oneHasLockDate) {
			  oneHasLockDate = true;
		  } else if (jobEntity.getLockExpirationTime() != null && oneHasLockDate) {
			  Assert.fail("Only one with a lock is expected");
		  }
		  
		  // due date assertion
		  if (job.getDuedate() != null) {
		  	nrWithDueDate++;
		  } else {
		  	nrWithoutDuedate++;
		  }
	  }
	  Assert.assertEquals(3, nrWithDueDate);
	  Assert.assertEquals(3, nrWithoutDuedate);;
	  
	  Assert.assertTrue("One job should have lock expiration date, but none found", oneHasLockDate);
	  
	  // Assert deploy time upgrade
	  for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
	  	Assert.assertNotNull(deployment.getDeploymentTime());
	  }

	  // Assert create time for tasks
	  List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("testTimestampPrecisionUpgrade").list();
	  Assert.assertEquals(3, tasks.size());
	  
	  boolean oneWithDuedate = false;
	  for (Task task : tasks) {
	  	Assert.assertNotNull(task.getCreateTime());
	  	
	  	if (task.getDueDate() != null && !oneWithDuedate) {
	  		oneWithDuedate = true;
	  	} else if (task.getDueDate() != null && oneWithDuedate) {
	  		Assert.fail("Only one task with due date is expected");
	  	}
	  }
	  
	  // Assert event subscription date
	  List<EventSubscriptionEntity> eventSubscriptionEntities = new EventSubscriptionQueryImpl(((ProcessEngineImpl)processEngine).getProcessEngineConfiguration().getCommandExecutor()).list();
	  Assert.assertEquals(3, eventSubscriptionEntities.size());
	  for (EventSubscriptionEntity eventSubscriptionEntity : eventSubscriptionEntities) {
	  	Assert.assertNotNull(eventSubscriptionEntity);
	  }
	  
	  // Assert process instance start time (history)
	  for (HistoricProcessInstance historicProcessInstance : historyService.createHistoricProcessInstanceQuery().processDefinitionKey("testTimestampPrecisionUpgrade").list()) {
	  	Assert.assertNotNull(historicProcessInstance.getStartTime());
	  	Assert.assertNull("process has not yet ended, but end time was not null", historicProcessInstance.getEndTime());
	  }
	  
	  // Assert activity start and end time
	  for (HistoricActivityInstance historicActivityInstance : historyService.createHistoricActivityInstanceQuery().list()) {
	  	Assert.assertNotNull(historicActivityInstance.getStartTime());
	  }
	  
	  // Assert historic start time
	  for (HistoricTaskInstance historicTaskInstance : historyService.createHistoricTaskInstanceQuery().processDefinitionKey("testTimestampPrecisionUpgrade").list()) {
	  	Assert.assertNotNull(historicTaskInstance.getStartTime());
	  	Assert.assertNull(historicTaskInstance.getEndTime());
	  	Assert.assertNull(historicTaskInstance.getClaimTime());
	  }
	  
	  // Cleanup
	  Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey("testTimestampPrecisionUpgrade").singleResult();
	  repositoryService.deleteDeployment(deployment.getId(), true);
	  
  }

}

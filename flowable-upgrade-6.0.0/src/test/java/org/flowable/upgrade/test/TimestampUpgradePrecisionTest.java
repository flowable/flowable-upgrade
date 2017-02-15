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

import java.util.List;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.task.Task;
import org.flowable.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.test.helper.UpgradeTestCase;
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
	  
	  List<Job> timerJobs = managementService.createTimerJobQuery().processDefinitionId(processDefinition.getId()).list();
	  Assert.assertEquals(3, timerJobs.size());
	  for (Job timerJob : timerJobs) {
	  	Assert.assertNotNull(timerJob.getDuedate());
	  }
	  
	  List<Job> jobs = managementService.createJobQuery().processDefinitionId(processDefinition.getId()).list();
	  Assert.assertEquals(3, jobs.size());
	  boolean oneHasLockDate = false;
	  for (Job job : jobs) {
		  
		  JobEntity jobEntity = (JobEntity) job;
		  if (jobEntity.getLockExpirationTime() != null && !oneHasLockDate) {
			  oneHasLockDate = true;
		  } else if (jobEntity.getLockExpirationTime() != null && oneHasLockDate) {
			  Assert.fail("Only one with a lock is expected");
		  }
		  
	  }
	  
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
	  List<EventSubscription> eventSubscriptionEntities = new EventSubscriptionQueryImpl(((ProcessEngineImpl)processEngine).getProcessEngineConfiguration().getCommandExecutor()).list();
	  Assert.assertEquals(3, eventSubscriptionEntities.size());
	  for (EventSubscription eventSubscriptionEntity : eventSubscriptionEntities) {
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

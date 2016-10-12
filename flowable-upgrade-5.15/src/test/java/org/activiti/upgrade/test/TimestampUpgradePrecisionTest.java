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

import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Job;
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
	  // One has a duedate set and one has a lock time set
	  
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
  }

}

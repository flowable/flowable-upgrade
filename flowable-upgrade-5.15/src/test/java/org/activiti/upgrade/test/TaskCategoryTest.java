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

import static org.junit.Assert.assertNull;
import junit.framework.Assert;

import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;
import org.activiti.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.Test;

/**
 * This is an upgrade test added for the 5.15 release. In that release, we've
 * added a database column to store the category of a task
 * 
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.14"})
public class TaskCategoryTest extends UpgradeTestCase {

  @Test
  public void testTaskCategoryAfterUpgrade() {
  	
  	// After upgrade, task category should be null
  	Task task = taskService.createTaskQuery().taskName("Task that will get a category after upgrade").singleResult();
  	assertNull(task.getCategory());

  	// Verify history
  	HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
  	assertNull(historicTaskInstance.getCategory());
  	
  	// Update category
  	String newCategory = "test123";
  	task.setCategory(newCategory);
  	taskService.saveTask(task);
  	
  	// Verify
  	task = taskService.createTaskQuery().taskName("Task that will get a category after upgrade").singleResult();
  	Assert.assertEquals(task.getCategory(), newCategory);
  	
  	// Finish process
  	taskService.complete(task.getId());
  }

}

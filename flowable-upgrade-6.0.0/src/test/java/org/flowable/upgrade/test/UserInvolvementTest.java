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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.IdentityLink;
import org.flowable.engine.task.IdentityLinkType;
import org.flowable.engine.task.Task;
import org.flowable.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.test.helper.UpgradeTestCase;
import org.junit.Test;


/**
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.7", "5.8", "5.9", "5.10", "5.11"})
public class UserInvolvementTest extends UpgradeTestCase {
  
  @Test
  public void testUserInvolvementAfterUpgrade() {
    
    // For old processes, no user involvement data should have been generated
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("kermit").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("fozzie").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("gonzo").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("mispiggy").count());
    
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("userInvolvementUpgradeTest").singleResult();
    assertNotNull(processInstance);
    
    // When we continue the process now, the new tasks should generate user involvements
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertEquals(2, tasks.size());
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("kermit").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("fozzie").count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().involvedUser("gonzo").count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().involvedUser("mispiggy").count());
    
    List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
    assertEquals(2, identityLinks.size());
    for(IdentityLink identityLink : identityLinks) {
      assertTrue(identityLink.getUserId().equals("gonzo") || identityLink.getUserId().equals("mispiggy"));
    }
    
    // Try to involve another user
    runtimeService.addUserIdentityLink(processInstance.getId(), "jos", IdentityLinkType.PARTICIPANT);
    identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
    assertEquals(3, identityLinks.size());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("kermit").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().involvedUser("fozzie").count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().involvedUser("gonzo").count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().involvedUser("mispiggy").count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().involvedUser("jos").count());
    
    // Cleanup
	  Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey("userInvolvementProcess").singleResult();
	  repositoryService.deleteDeployment(deployment.getId(), true);
  }

}

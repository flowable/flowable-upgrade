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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.junit.Test;

/**
 * 
 * In 6.2.1 -> 6.3.0, entity counting on executions is enabled by default.
 * 
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = "6.2.1")
public class EntityCountingTest extends UpgradeTestCase {
    
    @Test
    public void testNoCountsForOldProcessInstance() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("testEntityCounts").singleResult();
        assertNotNull(processInstance);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        CountingTaskEntity countingTaskEntity = (CountingTaskEntity) task;
        assertFalse(countingTaskEntity.isCountEnabled());
        assertEquals(0, countingTaskEntity.getIdentityLinkCount());
        assertEquals(0, countingTaskEntity.getSubTaskCount());
        assertEquals(0, countingTaskEntity.getVariableCount());
        
        Execution executionAtUserTask = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
                .activityId("sid-5CAF1453-E0C5-4742-8944-D061FF301F48")
                .singleResult();
        assertNotNull(executionAtUserTask);
        assertTrue(executionAtUserTask instanceof CountingExecutionEntity);
        
        CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) executionAtUserTask;
        assertFalse(countingExecutionEntity.isCountEnabled());
        assertEquals(0, countingExecutionEntity.getDeadLetterJobCount());
        assertEquals(0, countingExecutionEntity.getEventSubscriptionCount());
        assertEquals(0, countingExecutionEntity.getIdentityLinkCount());
        assertEquals(0, countingExecutionEntity.getJobCount());
        assertEquals(0, countingExecutionEntity.getSuspendedJobCount());
        assertEquals(0, countingExecutionEntity.getTaskCount());
        assertEquals(0, countingExecutionEntity.getTimerJobCount());
        assertEquals(0, countingExecutionEntity.getVariableCount());
        
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job.getId());
        managementService.executeJob(job.getId());
        
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    public void testCountsForNewProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testEntityCounts");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addUserIdentityLink(task.getId(), "someUser", IdentityLinkType.PARTICIPANT); 
        taskService.setVariableLocal(task.getId(), "taskVar", "test");
        assertTrue(task instanceof CountingTaskEntity);
        
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        CountingTaskEntity countingTaskEntity = (CountingTaskEntity) task;
        assertTrue(countingTaskEntity.isCountEnabled());
        assertEquals(1, countingTaskEntity.getIdentityLinkCount());
        assertEquals(0, countingTaskEntity.getSubTaskCount());
        assertEquals(1, countingTaskEntity.getVariableCount());

        String executionId = task.getExecutionId();
        runtimeService.setVariableLocal(executionId, "someVariable", "someValue");
        runtimeService.setVariableLocal(executionId, "someVariable2", "someValue2");
        
        Execution executionAtUserTask = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        assertNotNull(executionAtUserTask);
        assertTrue(executionAtUserTask instanceof CountingExecutionEntity);
        
        CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) executionAtUserTask;
        assertTrue(countingExecutionEntity.isCountEnabled());
        assertEquals(0, countingExecutionEntity.getDeadLetterJobCount());
        assertEquals(0, countingExecutionEntity.getEventSubscriptionCount());
        assertEquals(0, countingExecutionEntity.getIdentityLinkCount());
        assertEquals(0, countingExecutionEntity.getJobCount());
        assertEquals(0, countingExecutionEntity.getSuspendedJobCount());
        assertEquals(1, countingExecutionEntity.getTaskCount());
        assertEquals(0, countingExecutionEntity.getTimerJobCount());
        assertEquals(2, countingExecutionEntity.getVariableCount());
        
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job.getId());
        Execution executionAtJob = runtimeService.createExecutionQuery().executionId(job.getExecutionId()).singleResult();
        assertNotNull(executionAtJob);
        assertTrue(executionAtJob instanceof CountingExecutionEntity);
        
        countingExecutionEntity = (CountingExecutionEntity) executionAtJob;
        assertTrue(countingExecutionEntity.isCountEnabled());
        assertEquals(0, countingExecutionEntity.getDeadLetterJobCount());
        assertEquals(0, countingExecutionEntity.getEventSubscriptionCount());
        assertEquals(0, countingExecutionEntity.getIdentityLinkCount());
        assertEquals(1, countingExecutionEntity.getJobCount());
        assertEquals(0, countingExecutionEntity.getSuspendedJobCount());
        assertEquals(0, countingExecutionEntity.getTaskCount());
        assertEquals(0, countingExecutionEntity.getTimerJobCount());
        assertEquals(0, countingExecutionEntity.getVariableCount());
        
        managementService.executeJob(job.getId());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

}

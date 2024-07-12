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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.task.api.Task;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.helper.EntitySnapShotUtil;
import org.flowable.upgrade.helper.EntitySnapShotUtil.EntitySnapShot;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jayway.awaitility.Awaitility;


/**
 * @author Joram Barrez
 */
@RunOnlyWithTestDataFromVersion(versions = {"5.21.0", "5.22.0"})
public class V5ToV6JobMigrationTest extends UpgradeTestCase {
	
	/*
	 * See the DataGenerator in 5.21. it lists all kind of cases, which are referenced here by their number.
	 */
	
	private Date startTime;
	
	private EntitySnapShot entitySnapShot;
	
	@Before
	public void moreSetup() throws Exception {
		if (runningTests) {
			entitySnapShot = EntitySnapShotUtil.createEntitySnapshot(processEngine);
		
			this.startTime = new Date();
			processEngineConfiguration.getClock().setCurrentTime(startTime);
		}
	}
	
	@After
	public void resetData() throws Exception {
		if (runningTests) {
		    EntitySnapShotUtil.restore(processEngine, entitySnapShot);
		}
	}
	
	@Test
	public void testCase0() {
		
		// There should be one timer job matching the timer start event 
		
		List<Job> timerJobs = managementService.createTimerJobQuery().list();
		Job startEventTimer = null;
		for (Job timerJob : timerJobs) {
			if (timerJob.getProcessInstanceId() == null && timerJob.getExecutionId() == null) {
				if (startEventTimer != null) {
					Assert.fail("There is more than one start event time");
				} else {
					startEventTimer = timerJob;
				}
			}
		}
		Assert.assertNotNull(startEventTimer);
	}
	
	@Test
	public void testCase01() {
		
		// Case01 should be in async service task 01
		
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case01").singleResult();
		List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(1, jobs.size());

		Job serviceTaskAJob = jobs.get(0);
		
		// Running the job executor should move the job forward (non deterministic to where, but at least away from service task A)
		waitUntilAsyncJobsAreHandled(serviceTaskAJob.getId());
		
		// Get jobs for process instance, none should be in the service task A
		assertNoServiceTaskWithName(processInstance, "A");
	}
	
	@Test
	public void testCase2() {
		
		// Case 02 should be in B/C/D/E/F + a timer
		
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case02").singleResult();
		List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(5, jobs.size());
		
		waitUntilAsyncJobsAreHandled(getJobIds(jobs));
		
		assertNoServiceTaskWithName(processInstance, "A");
		assertNoServiceTaskWithName(processInstance, "B");
		assertNoServiceTaskWithName(processInstance, "C");
		assertNoServiceTaskWithName(processInstance, "D");
		assertNoServiceTaskWithName(processInstance, "E");
		assertNoServiceTaskWithName(processInstance, "F");
	}
	
	@Test
	public void testCase02Bis() {
		
		// For case02, there should be one timer, one the boundary event.
		// When it triggers, the subprocess should be destroyed
		
		final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case02bis").singleResult();
		List<Job> timerJobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(1, timerJobs.size());
		
		// Move clock 6 mins (timer is on 5 mins, so is enough)
		processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (6 * 60 * 1000)));
		
		// let the job executor work until user task L is reached
		startAsyncExecutor();
		Awaitility.await().atMost(30, TimeUnit.SECONDS).until(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("L").singleResult() != null;
			}
		});
		stopAsyncExecutor();
		
		String timerJobId = timerJobs.get(0).getId();
		Assert.assertNull(managementService.createTimerJobQuery().jobId(timerJobId).singleResult());
		Assert.assertNull(managementService.createJobQuery().jobId(timerJobId).singleResult());
		Assert.assertNull(managementService.createDeadLetterJobQuery().jobId(timerJobId).singleResult());
		Assert.assertNull(managementService.createSuspendedJobQuery().jobId(timerJobId).singleResult());
		
		assertNoServiceTaskWithName(processInstance, "A");
		assertNoServiceTaskWithName(processInstance, "B");
		assertNoServiceTaskWithName(processInstance, "C");
		assertNoServiceTaskWithName(processInstance, "D");
		assertNoServiceTaskWithName(processInstance, "E");
		assertNoServiceTaskWithName(processInstance, "F");
	}
	
	@Test
	public void testCase03() {
		
		// Case03: intermediate throw timer is available
		// There should be no async timers available for this particular process instance
		final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case03").singleResult();
		Assert.assertEquals(0L, managementService.createJobQuery().processInstanceId(processInstance.getId()).count());
		
		List<Job> timerJobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(2, timerJobs.size());
		
		// The boundary timer on the embedded subprocess is 5mins, the intermediate throw is 10s. 
		// Move the clock 15 seconds and the latter should fire and the process should be in user task G.
		Date lowestDueDate = timerJobs.get(0).getDuedate();
		if (timerJobs.get(1).getDuedate().compareTo(lowestDueDate) < 0) {
			lowestDueDate = timerJobs.get(1).getDuedate();
		}
		processEngineConfiguration.getClock().setCurrentTime(new Date(lowestDueDate.getTime() + (15 * 1000)));
		waitUntilNumberOfTimerJobsForProcessInstanceEqualTo(processInstance, 1);
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertNotNull(task.getId());
		
		// Completing the task should finish the process instance
		taskService.complete(task.getId());
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);
		
	}
	
	@Test
	public void testCase03bis() {
		
		// Case03: In the parallel gateways
		final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case03bis").singleResult();
		Assert.assertEquals(1, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
		
		// Letting the async executor work should bring it in the timer 
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
		Assert.assertEquals(2, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
	}
	
	@Test
	public void testCase04() {
		
		final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case04").singleResult();
		
		// Case04: user task G should be active 
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertNotNull(task.getId());
		Assert.assertEquals("G", task.getName());
		
		// Completing the task should finish the process instance
		taskService.complete(task.getId());
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);
		
	}
	
	@Test
	public void testCase04_02() {
		
		final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case04").singleResult();
		
		// Firing timer 
		List<Job> timerJobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(1, timerJobs.size());
		managementService.moveTimerToExecutableJob(timerJobs.get(0).getId());
		managementService.executeJob(timerJobs.get(0).getId());
		
		Assert.assertEquals(1, managementService.createJobQuery().processInstanceId(processInstance.getId()).count());
		
	}
	
	@Test
	public void testCase05() {
		
		// Case05: boundary timer fired, F should be active.
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case05").singleResult();
		List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(1, jobs.size());

		assertServiceTaskWithName(processInstance, "After subprocess");
		Job serviceTaskFJob = jobs.get(0);
		
		// Running the job executor should move the job forward (non deterministic to where, but at least away from service task A)
		waitUntilAsyncJobsAreHandled(serviceTaskFJob.getId());
		
		// Get jobs for process instance, none should be in the service task A
		assertNoServiceTaskWithName(processInstance, "F");
		assertNoServiceTaskWithName(processInstance, "After subprocess");
	}
	
	@Test
	public void testCase06() {
		
		// Case06: H/I/J are active
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case06").singleResult();
		assertNoServiceTaskWithName(processInstance, "After subprocess");
		assertNoServiceTaskWithName(processInstance, "G");
		
		// Let the async executor work until no service tasks left, user task L should be active
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("L", task.getName());
		
		// Process should be done
		taskService.complete(task.getId());
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);

	}
	
	@Test
	public void testCase07() {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case07").singleResult();
		assertServiceTaskWithName(processInstance, "I");
		assertServiceTaskWithName(processInstance, "J");
		
	  // Let the async executor work until no service tasks left, user task L should be active
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
			
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("L", task.getName());
			
		// Process should be done
		taskService.complete(task.getId());
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);
	}
	
	@Test
	public void testCase08() {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case08").singleResult();
		assertServiceTaskWithName(processInstance, "K");
		
	  // Let the async executor work until no service tasks left, user task L should be active
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
			
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("L", task.getName());
			
		// Process should be done
		taskService.complete(task.getId());
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);
	}
	
	@Test
	public void testCase09() {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case09").singleResult();
			
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("L", task.getName());
		
		// Should be one timer, firing it brings it to the next user task
		Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
		managementService.moveTimerToExecutableJob(timerJob.getId());
		managementService.executeJob(timerJob.getId());
		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("M", task.getName());
		
		timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
		managementService.moveTimerToExecutableJob(timerJob.getId());
		managementService.executeJob(timerJob.getId());
		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("N", task.getName());
		
		timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
		managementService.moveTimerToExecutableJob(timerJob.getId());
		managementService.executeJob(timerJob.getId());
		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertEquals("O", task.getName());
		
		// Last user tasks goes to async service task J
		timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
		managementService.moveTimerToExecutableJob(timerJob.getId());
		managementService.executeJob(timerJob.getId());
		assertServiceTaskWithName(processInstance, "J");
		
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
		
		// Process should be done
		Assert.assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult() == null);
	}
	
	@Test
	public void testCase10() {
		
		// Case 10: suspended in B/C/D/E/F
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("case10").singleResult();
		List<Job> suspendedJobs = managementService.createSuspendedJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(6, suspendedJobs.size());
		Assert.assertEquals(0L, managementService.createJobQuery().processInstanceId(processInstance.getId()).count());
		Assert.assertEquals(0L, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
		
		// Reactive the process instance
		runtimeService.activateProcessInstanceById(processInstance.getId());
		suspendedJobs = managementService.createSuspendedJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(0, suspendedJobs.size());
		Assert.assertEquals(5L, managementService.createJobQuery().processInstanceId(processInstance.getId()).count());
		Assert.assertEquals(1L, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
	}
	
	@Test
	public void testStartProcessInstanceInV6() {
		
		// Verify process definitions
		ProcessDefinition oldProcessDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey("plenty-of-jobs").singleResult();
		Assert.assertEquals(Flowable5Util.V5_ENGINE_TAG, oldProcessDefinition.getEngineVersion());
		
		repositoryService.createDeployment()
			.addClasspathResource("V5To6Test.processWithPlentyJobs.bpmn20.xml").deploy();
		Assert.assertEquals(2L, repositoryService.createProcessDefinitionQuery().processDefinitionKey("plenty-of-jobs").count());
		
		ProcessDefinition latestProcessDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey("plenty-of-jobs").latestVersion().singleResult();
		Assert.assertNull(latestProcessDefinition.getEngineVersion());
		
		// Start process instance
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("plenty-of-jobs");
		
		// Let the async executor work. It should go to the intermediate timer
		assertServiceTaskWithName(processInstance, "A");
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
		List<Job> timerJobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
		Assert.assertEquals(2, timerJobs.size());
		
		// The timer with the highest duedate is the boundary timer on the embedded subprocess
		Job boundaryTimerJob = timerJobs.get(0);
		if (timerJobs.get(1).getDuedate().compareTo(boundaryTimerJob.getDuedate()) > 0) {
			boundaryTimerJob = timerJobs.get(1);
		}
		managementService.moveTimerToExecutableJob(boundaryTimerJob.getId());
		managementService.executeJob(boundaryTimerJob.getId());
		
		assertServiceTaskWithName(processInstance, "After subprocess");
		waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(processInstance, 0);
		
		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
		Assert.assertNotNull(task);
	}
	
	
	// ///////////////////////////////////////////////////////////////////////
	// HELPER METHODS ////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////

	private void startAsyncExecutor() {
		AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
		if (!asyncExecutor.isActive()) {
			asyncExecutor.start();
		}
	}
	
	private void stopAsyncExecutor() {
		AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
		if (asyncExecutor.isActive()) {
			asyncExecutor.shutdown();
		}
	}
	
	private void waitUntilAsyncJobsAreHandled(String jobId) {
		waitUntilAsyncJobsAreHandled(Arrays.asList(jobId));
	}
	
	private void waitUntilAsyncJobsAreHandled(List<String> jobIds) {
		startAsyncExecutor();
		Awaitility.await().atMost(20, TimeUnit.SECONDS).until(asyncJobsAreHandled(jobIds));
		stopAsyncExecutor();
	}
	
	private void waitUntilNumberOfTimerJobsForProcessInstanceEqualTo(final ProcessInstance processInstance,final  long count) {
		startAsyncExecutor();
		Awaitility.await().atMost(20, TimeUnit.SECONDS).until(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count() == count;
			}
		});
		stopAsyncExecutor();
	}
	
	private void waitUntilNumberOfAsyncJobsForProcessInstanceEqualTo(final ProcessInstance processInstance,final  long count) {
		startAsyncExecutor();
		Awaitility.await().atMost(20, TimeUnit.SECONDS).until(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return managementService.createJobQuery().processInstanceId(processInstance.getId()).count() == count;
			}
		});
		stopAsyncExecutor();
	}
	
	private Callable<Boolean> asyncJobsAreHandled(final List<String> jobIds) {
		return new Callable<Boolean>() {
			public Boolean call() throws Exception {
				for (String jobId : jobIds) {
					Job job = managementService.createJobQuery().jobId(jobId).singleResult();
					if (job != null) {
						return false;
					}
				}
				return true;
			}
		};
	}
	
	private void assertServiceTaskWithName(ProcessInstance processInstance, String serviceTaskName) {
		List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
		boolean found = false;
		for (Job job : jobs) {
			if (job.getExecutionId() != null) {
				ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(job.getExecutionId()).singleResult();
				BpmnModel bpmnModel = repositoryService.getBpmnModel(execution.getProcessDefinitionId());
				FlowElement flowElement = bpmnModel.getFlowElement(execution.getActivityId());
				if (serviceTaskName.equals(flowElement.getName())) {
					found = true;
				}
			}
		}
		Assert.assertTrue(found);
	}
	
	private void assertNoServiceTaskWithName(ProcessInstance processInstance, String serviceTaskName) {
		List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
		for (Job job : jobs) {
			if (job.getExecutionId() != null) {
				ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(job.getExecutionId()).singleResult();
				BpmnModel bpmnModel = repositoryService.getBpmnModel(execution.getProcessDefinitionId());
				FlowElement flowElement = bpmnModel.getFlowElement(execution.getActivityId());
				Assert.assertFalse(serviceTaskName.equals(flowElement.getName()));
			}
		}
	}
	
	private List<String> getJobIds(List<Job> jobs) {
		List<String> ids = new ArrayList<String>();
		for (Job job : jobs) {
			ids.add(job.getId());
		}
		return ids;
	}
	

}

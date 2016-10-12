package org.activiti.upgrade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ParallelGateway;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.upgrade.helper.UpgradeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * @author Joram Barrez
 */
public class DataGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataGenerator.class);
  
  public static void main(String[] args) {
    ProcessEngine processEngine = UpgradeUtil.getProcessEngine();
    createCommonData(processEngine);
    create5To6Data(processEngine);
  }
  
  private static void createCommonData(ProcessEngine processEngine) {
    // UpgradeTaskOneTest in 5.8
    RuntimeService runtimeService = processEngine.getRuntimeService();
    TaskService taskService = processEngine.getTaskService();

    processEngine.getRepositoryService()
      .createDeployment()
      .name("simpleTaskProcess")
      .addClasspathResource("org/activiti/upgrade/test/UserTaskBeforeTest.testSimplestTask.bpmn20.xml")
      .deploy();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleTaskProcess");
    String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
    taskService.complete(taskId);
    
    // UpgradeTaskTwoTest in 5.8
    processEngine.getRepositoryService().createDeployment().name("simpleTaskProcess")
            .addClasspathResource("org/activiti/upgrade/test/UserTaskBeforeTest.testTaskWithExecutionVariables.bpmn20.xml")
            .deploy();

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("instrument", "trumpet");
    variables.put("player", "gonzo");
    runtimeService.startProcessInstanceByKey("taskWithExecutionVariablesProcess", variables);
  }
  
  private static void create5To6Data(ProcessEngine processEngine) {
 	 	createSuspendTestData(processEngine);
 	 	createFailingServiceTaskData(processEngine);
 	 	createJobsTestData(processEngine);
 }

	private static void createSuspendTestData(ProcessEngine processEngine) {
		RepositoryService repositoryService = processEngine.getRepositoryService();
    repositoryService.createDeployment().addClasspathResource("org/activiti/upgrade/test/Activiti5To6Test.testSuspendProcess.bpmn20.xml").deploy();

    RuntimeService runtimeService = processEngine.getRuntimeService();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("suspendProcess-activiti5");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());
	}
	
	private static void createJobsTestData(ProcessEngine processEngine) {
		processEngine.getRepositoryService().createDeployment()
			.addClasspathResource("org/activiti/upgrade/test/Activiti5To6Test.processWithPlentyJobs.bpmn20.xml").deploy();
		
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ManagementService managementService = processEngine.getManagementService();
		
		// There is a png for this process to make things a bit more understandable.
		
		// Case 0: there is a timer start for the timer start event
		
		// Case 1: 	Process instance is in service task A
		LOGGER.info("Case 1");
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case01");
		
		// Case 2: Process instance is past A: service task B/C/D/E/F should be active
		// Also a timer on the boundary event should be active
		LOGGER.info("Case 2");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case02");
		completeAsyncServiceTasks(processEngine, processInstance, "A");
		
		// Case 2bis: Same as case2, but now to test the timer
		LOGGER.info("Case 2bis");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case02bis");
		completeAsyncServiceTasks(processEngine, processInstance, "A");
		
		// Case 3: service task B/C/D/E/F are completed. There should be a timer throw active now.
		LOGGER.info("Case 3");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case03");
		completeAsyncServiceTasks(processEngine, processInstance, "A", "B", "C", "D", "E", "F");
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		
		// Case 3bis: service task B/C/D/E/F are completed, but the parallel gateways not yet
		LOGGER.info("Case 3bis");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case03bis");
		completeAsyncServiceTasks(processEngine, processInstance, "A", "B", "C", "D", "E", "F");

		// Case 4: the intermediate timer is completed: user task G should be active
		LOGGER.info("Case 4");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case04");
		completeAsyncServiceTasks(processEngine, processInstance,  "A", "B", "C", "D", "E", "F");
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		Job intermediateTimerJob = findJobWithType(processEngine, processInstance, TimerCatchIntermediateEventJobHandler.TYPE);
		managementService.executeJob(intermediateTimerJob.getId());
		
		// Case 5: the boundary timer on the embedded subprocess is completed: F should be active
		LOGGER.info("Case 5");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case05");
		completeAsyncServiceTasks(processEngine, processInstance, "A");
		Job boundaryTimer = findJobWithType(processEngine, processInstance, TimerExecuteNestedActivityJobHandler.TYPE);
		managementService.executeJob(boundaryTimer.getId());
		
		// Case 6: boundary timer fired + service task F and G completed : H, I, J should be active
		LOGGER.info("Case 6");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case06");
		completeAsyncServiceTasks(processEngine, processInstance, "A"); 
		boundaryTimer = findJobWithType(processEngine, processInstance, TimerExecuteNestedActivityJobHandler.TYPE);
		managementService.executeJob(boundaryTimer.getId());
		completeAsyncServiceTasks(processEngine, processInstance, "After subprocess", "G");
		
		// Case 7: case 6 +  service task H completed
		LOGGER.info("Case 7");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case07");
		completeAsyncServiceTasks(processEngine, processInstance, "A"); 
		boundaryTimer = findJobWithType(processEngine, processInstance, TimerExecuteNestedActivityJobHandler.TYPE);
		managementService.executeJob(boundaryTimer.getId());
		completeAsyncServiceTasks(processEngine, processInstance, "After subprocess", "G", "H");
		
		// Case 8: H/I/J are completed : K is active
		LOGGER.info("Case 8");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case08");
		completeAsyncServiceTasks(processEngine, processInstance, "A"); // A
		boundaryTimer = findJobWithType(processEngine, processInstance, TimerExecuteNestedActivityJobHandler.TYPE);
		managementService.executeJob(boundaryTimer.getId());
		completeAsyncServiceTasks(processEngine, processInstance, "After subprocess", "G", "H", "I", "J");
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		
		// Case 9: K is completed: user task L is active
		LOGGER.info("Case 9");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case09");
		completeAsyncServiceTasks(processEngine, processInstance, "A"); 
		boundaryTimer = findJobWithType(processEngine, processInstance, TimerExecuteNestedActivityJobHandler.TYPE);
		managementService.executeJob(boundaryTimer.getId());
		completeAsyncServiceTasks(processEngine, processInstance, "After subprocess", "G", "H", "I", "J");
		compleAsyncJoiningParallelGateways(processEngine, processInstance);
		completeAsyncServiceTasks(processEngine, processInstance, "K");
		
		// Case 10: in B/C/D/E/F, but suspended
		LOGGER.info("Case 10");
		processInstance = runtimeService.startProcessInstanceByKey("activiti5-plenty-of-jobs", "case10");
		completeAsyncServiceTasks(processEngine, processInstance, "A");
		runtimeService.suspendProcessInstanceById(processInstance.getId());
	}
	
	private static void createFailingServiceTaskData(ProcessEngine processEngine) {
		processEngine.getRepositoryService().createDeployment()
		.addClasspathResource("org/activiti/upgrade/test/Activiti5To6Test.processWithFailingServiceTask.bpmn20.xml").deploy();
	
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ManagementService managementService = processEngine.getManagementService();
		
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activiti5-failing-job");
		Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
		
		// Execute 3 times - put retries on 0
		try { managementService.executeJob(job.getId()); } catch (Exception e) {}
		try { managementService.executeJob(job.getId()); } catch (Exception e) {}
		try { managementService.executeJob(job.getId()); } catch (Exception e) {}
	}

	private static void completeAsyncServiceTasks(ProcessEngine processEngine, ProcessInstance processInstance, String activityName) {
		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ManagementService managementService = processEngine.getManagementService();
		
		// Find the activityId
		String activityId = null;
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
		Process process = bpmnModel.getMainProcess();
		List<ServiceTask> serviceTasks = process.findFlowElementsOfType(ServiceTask.class);
		for (ServiceTask serviceTask : serviceTasks) {
			if (activityName.equals(serviceTask.getName())) {
				activityId = serviceTask.getId();
				break;
			}
		}
		
		// Find the job
		List<Job> jobs = managementService.createJobQuery().messages().processInstanceId(processInstance.getId()).list();
		boolean executed = false;
		for (Job job : jobs) {
			if (job.getExecutionId() != null) {
				 Execution execution = runtimeService.createExecutionQuery().executionId(job.getExecutionId()).singleResult();
				 if (execution.getActivityId().equals(activityId)) {
					 managementService.executeJob(job.getId());
					 executed = true;
					 LOGGER.info("Executed job for service task with name " + activityName);
					 break;
				 }
			}
		}
		
		if (!executed) {
			LOGGER.warn("Watch out: did NOT find a job for activity " + activityName);
		}
	}
	
	private static void completeAsyncServiceTasks(ProcessEngine processEngine, ProcessInstance processInstance, String ... activityNames) {
		for (String activityName : activityNames) {
			completeAsyncServiceTasks(processEngine, processInstance, activityName);
		}
	}
	
	private static void compleAsyncJoiningParallelGateways(ProcessEngine processEngine, ProcessInstance processInstance) {
		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ManagementService managementService = processEngine.getManagementService();
		
		Process process = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId()).getMainProcess();
		
		
		// Find the job
		List<Job> jobs = managementService.createJobQuery().messages().processInstanceId(processInstance.getId()).list();
		for (Job job : jobs) {
			if (job.getExecutionId() != null) {
				 Execution execution = runtimeService.createExecutionQuery().executionId(job.getExecutionId()).singleResult();
				 FlowElement flowElement = process.getFlowElementRecursive(execution.getActivityId());
				 if (flowElement instanceof ParallelGateway) {
					 managementService.executeJob(job.getId());
				 }
			}
		}
	}
	
	private static Job findJobWithType(ProcessEngine processEngine, ProcessInstance processInstance, String type) {
		List<Job> jobs = processEngine.getManagementService().createJobQuery().processInstanceId(processInstance.getId()).list();
		for (Job job : jobs) {
			JobEntity jobEntity = (JobEntity) job;
			if (jobEntity.getJobHandlerType().equals(type)) {
				return job;
			}
		}
		return null;
	}
	
  
}

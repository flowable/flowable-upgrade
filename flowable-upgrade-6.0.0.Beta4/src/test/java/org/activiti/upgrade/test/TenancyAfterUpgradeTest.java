package org.activiti.upgrade.test;

import java.util.ArrayList;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * A copy of 'TenancyTest' in the regular test suite.
 * After an upgrade, should be working fully. Tenancy was not available in older versions.
 * 
 * @author jbarrez
 */
public class TenancyAfterUpgradeTest extends UpgradeTestCase {
	
	private static final String TEST_TENANT_ID = "myTenantId";
	
	private List<String> autoCleanedUpDeploymentIds = new ArrayList<String>();
	
	@Before
	public void setUp() throws Exception {
	  this.autoCleanedUpDeploymentIds.clear();;
	}
	
	@After
	public void tearDown() throws Exception {
	  if (autoCleanedUpDeploymentIds.size() > 0) {
	  	for (String deploymentId : autoCleanedUpDeploymentIds) {
	  		repositoryService.deleteDeployment(deploymentId, true);
	  	}
	  }
	}
	
	/**
	 * Deploys the one task process woth the test tenand id.
	 * 
	 * @return The process definition id of the deployed process definition.
	 */
	private String deployTestProcessWithTestTenant() {
	  return deployTestProcessWithTestTenant(TEST_TENANT_ID);
  }
	
	private String deployTestProcessWithTestTenant(String tenantId) {
	  String id = repositoryService.createDeployment()
			.addBpmnModel("testProcess.bpmn20.xml", createOneTaskTestProcess())
			.tenantId(tenantId)
			.deploy()
			.getId();
	  
	  autoCleanedUpDeploymentIds.add(id);
	  
	  return repositoryService.createProcessDefinitionQuery()
	  		.deploymentId(id)
	  		.singleResult()
	  		.getId();
  }
	
	
	
	private String deployTestProcessWithTwoTasksWithTestTenant() {
	  String id = repositoryService.createDeployment()
			.addBpmnModel("testProcess.bpmn20.xml", createTwoTasksTestProcess())
			.tenantId(TEST_TENANT_ID)
			.deploy()
			.getId();
	  
	  autoCleanedUpDeploymentIds.add(id);
	  
	  return repositoryService.createProcessDefinitionQuery()
	  		.deploymentId(id)
	  		.singleResult()
	  		.getId();
  }
	
	@Test
	public void testDeploymentTenancy() {
		
		deployTestProcessWithTestTenant();
		
		Assert.assertEquals(TEST_TENANT_ID, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").singleResult().getTenantId());
		Assert.assertEquals(1, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(1, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentId(autoCleanedUpDeploymentIds.get(0)).deploymentTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(1, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentTenantIdLike("my%").list().size());
		Assert.assertEquals(1, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentTenantIdLike("%TenantId").list().size());
		Assert.assertEquals(1, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentTenantIdLike("m%Ten%").list().size());
		Assert.assertEquals(0, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentTenantIdLike("noexisting%").list().size());
		Assert.assertEquals(0, repositoryService.createDeploymentQuery().processDefinitionKey("oneTaskProcess").deploymentWithoutTenantId().list().size());
	}

	@Test
	public void testProcessDefinitionTenancy() {

		// Deploy a process with tenant and verify
		
		String processDefinitionIdWithTenant = deployTestProcessWithTestTenant();
		Assert.assertEquals(TEST_TENANT_ID, repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionIdWithTenant).singleResult().getTenantId());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("m%").list().size());
		Assert.assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("somethingElse%").list().size());
		Assert.assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionWithoutTenantId().list().size());
		
		// Deploy another process, without tenant
		String processDefinitionIdWithoutTenant = deployOneTaskTestProcess();
		Assert.assertEquals(2, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list().size());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("m%").list().size());
		Assert.assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("somethingElse%").list().size());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionWithoutTenantId().list().size());
		
		// Deploy another process with the same tenant
		String processDefinitionIdWithTenant2 = deployTestProcessWithTestTenant();
		Assert.assertEquals(3, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list().size());
		Assert.assertEquals(2, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(2, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("m%").list().size());
		Assert.assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantIdLike("somethingElse%").list().size());
		Assert.assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionWithoutTenantId().list().size());
		
		// Extra check: we deployed the one task process twice, but once with tenant and once without. The latest query should show this.
		Assert.assertEquals(processDefinitionIdWithTenant2, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantId(TEST_TENANT_ID).latestVersion().singleResult().getId());
		Assert.assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionTenantId("Not a tenant").latestVersion().count());
		Assert.assertEquals(processDefinitionIdWithoutTenant, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").processDefinitionWithoutTenantId().latestVersion().singleResult().getId());
	}
	
	@Test
	public void testProcessInstanceTenancy() {
		
		// Start a number of process instances with tenant
		String processDefinitionId = deployTestProcessWithTestTenant();
		int nrOfProcessInstancesWithTenant = 6;
		for (int i=0; i<nrOfProcessInstancesWithTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionId);
		}
		
		// Start a number of process instance without tenantid
		String processDefinitionIdNoTenant = deployOneTaskTestProcess();
		int nrOfProcessInstancesNoTenant = 8;
		for (int i=0; i<nrOfProcessInstancesNoTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionIdNoTenant);
		}
		
		// Check the query results
		Assert.assertEquals(TEST_TENANT_ID, runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionId).list().get(0).getTenantId());
		Assert.assertEquals(nrOfProcessInstancesNoTenant + nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").list().size());
		Assert.assertEquals(nrOfProcessInstancesNoTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").processInstanceWithoutTenantId().list().size());
		Assert.assertEquals(nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").processInstanceTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").processInstanceTenantIdLike("%enan%").list().size());
		
	}
	
	@Test
	public void testExecutionTenancy() {
		
		// Start a number of process instances with tenant
		String processDefinitionId = deployTestProcessWithTwoTasksWithTestTenant();
		int nrOfProcessInstancesWithTenant = 4;
		for (int i=0; i<nrOfProcessInstancesWithTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionId);
		}
		
		// Start a number of process instance without tenantid
		String processDefinitionIdNoTenant = deployTwoTasksTestProcess();
		int nrOfProcessInstancesNoTenant = 2;
		for (int i=0; i<nrOfProcessInstancesNoTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionIdNoTenant);
		}
		
		// Check the query results:
		// note: 3 executions per process instance due to parallelism!
		Assert.assertEquals(TEST_TENANT_ID, runtimeService.createExecutionQuery().processDefinitionId(processDefinitionId).list().get(0).getTenantId());
		Assert.assertEquals("", runtimeService.createExecutionQuery().processDefinitionId(processDefinitionIdNoTenant).processDefinitionKey("twoTasksProcess").list().get(0).getTenantId());
		Assert.assertEquals(3 * (nrOfProcessInstancesNoTenant + nrOfProcessInstancesWithTenant), runtimeService.createExecutionQuery().processDefinitionKey("twoTasksProcess").list().size());
		Assert.assertEquals(3 * nrOfProcessInstancesNoTenant, runtimeService.createExecutionQuery().processDefinitionKey("twoTasksProcess").executionWithoutTenantId().list().size());
		Assert.assertEquals(3 * nrOfProcessInstancesWithTenant, runtimeService.createExecutionQuery().processDefinitionKey("twoTasksProcess").executionTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(3 * nrOfProcessInstancesWithTenant, runtimeService.createExecutionQuery().processDefinitionKey("twoTasksProcess").executionTenantIdLike("%en%").list().size());
		
		// Check the process instance query results, just to be sure
		Assert.assertEquals(TEST_TENANT_ID, runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionId).list().get(0).getTenantId());
		Assert.assertEquals(nrOfProcessInstancesNoTenant + nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("twoTasksProcess").list().size());
		Assert.assertEquals(nrOfProcessInstancesNoTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("twoTasksProcess").processInstanceWithoutTenantId().list().size());
		Assert.assertEquals(nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("twoTasksProcess").processInstanceTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(nrOfProcessInstancesWithTenant, runtimeService.createProcessInstanceQuery().processDefinitionKey("twoTasksProcess").processInstanceTenantIdLike("%en%").list().size());
	}
	
	@Test
	public void testTaskTenancy() {
		
		// Generate 10 tasks with tenant
		String processDefinitionIdWithTenant = deployTestProcessWithTwoTasksWithTestTenant();
		int nrOfProcessInstancesWithTenant = 5;
		for (int i=0; i<nrOfProcessInstancesWithTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionIdWithTenant);
		}
		
		// Generate 4 tasks without tenant
		String processDefinitionIdNoTenant = deployTwoTasksTestProcess();
		int nrOfProcessInstancesNoTenant = 2;
		for (int i = 0; i < nrOfProcessInstancesNoTenant; i++) {
			runtimeService.startProcessInstanceById(processDefinitionIdNoTenant);
		}
		
		// Check the query results
		Assert.assertEquals(TEST_TENANT_ID, taskService.createTaskQuery().processDefinitionId(processDefinitionIdWithTenant).list().get(0).getTenantId());
		Assert.assertEquals("", taskService.createTaskQuery().processDefinitionId(processDefinitionIdNoTenant).list().get(0).getTenantId());
		
		Assert.assertEquals(14, taskService.createTaskQuery().processDefinitionKey("twoTasksProcess").list().size());
		Assert.assertEquals(10, taskService.createTaskQuery().processDefinitionKey("twoTasksProcess").taskTenantId(TEST_TENANT_ID).list().size());
		Assert.assertEquals(0, taskService.createTaskQuery().processDefinitionKey("twoTasksProcess").taskTenantId("Another").list().size());
		Assert.assertEquals(10, taskService.createTaskQuery().processDefinitionKey("twoTasksProcess").taskTenantIdLike("my%").list().size());
		Assert.assertEquals(4, taskService.createTaskQuery().processDefinitionKey("twoTasksProcess").taskWithoutTenantId().list().size());
		
	}
	
	
  /**
   * Since the 'one task process' is used everywhere the actual process content
   * doesn't matter, instead of copying around the BPMN 2.0 xml one could use 
   * this method which gives a {@link BpmnModel} version of the same process back.
   */
  public BpmnModel createOneTaskTestProcess() {
  	BpmnModel model = new BpmnModel();
  	org.activiti.bpmn.model.Process process = new org.activiti.bpmn.model.Process();
    model.addProcess(process);
    process.setId("oneTaskProcess");
    process.setName("The one task process");
   
    StartEvent startEvent = new StartEvent();
    startEvent.setId("start");
    process.addFlowElement(startEvent);
    
    UserTask userTask = new UserTask();
    userTask.setName("The Task");
    userTask.setId("theTask");
    userTask.setAssignee("kermit");
    process.addFlowElement(userTask);
    
    EndEvent endEvent = new EndEvent();
    endEvent.setId("theEnd");
    process.addFlowElement(endEvent);;
    
    process.addFlowElement(new SequenceFlow("start", "theTask"));
    process.addFlowElement(new SequenceFlow("theTask", "theEnd"));
    
    return model;
  }
  
  public BpmnModel createTwoTasksTestProcess() {
  	BpmnModel model = new BpmnModel();
  	org.activiti.bpmn.model.Process process = new org.activiti.bpmn.model.Process();
    model.addProcess(process);
    process.setId("twoTasksProcess");
    process.setName("The two tasks process");
   
    StartEvent startEvent = new StartEvent();
    startEvent.setId("start");
    process.addFlowElement(startEvent);
    
    UserTask userTask = new UserTask();
    userTask.setName("The First Task");
    userTask.setId("task1");
    userTask.setAssignee("kermit");
    process.addFlowElement(userTask);
    
    UserTask userTask2 = new UserTask();
    userTask2.setName("The Second Task");
    userTask2.setId("task2");
    userTask2.setAssignee("kermit");
    process.addFlowElement(userTask2);
    
    EndEvent endEvent = new EndEvent();
    endEvent.setId("theEnd");
    process.addFlowElement(endEvent);;
    
    process.addFlowElement(new SequenceFlow("start", "task1"));
    process.addFlowElement(new SequenceFlow("start", "task2"));
    process.addFlowElement(new SequenceFlow("task1", "theEnd"));
    process.addFlowElement(new SequenceFlow("task2", "theEnd"));
    
    return model;
  }
  
  /**
   * Creates and deploys the one task process. See {@link #createOneTaskTestProcess()}.
   * 
   * @return The process definition id (NOT the process definition key) of deployed one task process.
   */
  public String deployOneTaskTestProcess() {
  	BpmnModel bpmnModel = createOneTaskTestProcess();
  	Deployment deployment = repositoryService.createDeployment()
  			.addBpmnModel("oneTasktest.bpmn20.xml", bpmnModel).deploy();
  	
  	autoCleanedUpDeploymentIds.add(deployment.getId()); // For auto-cleanup
  	
  	ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
  			.deploymentId(deployment.getId()).singleResult();
  	return processDefinition.getId(); 
  }
  
  public String deployTwoTasksTestProcess() {
  	BpmnModel bpmnModel = createTwoTasksTestProcess();
  	Deployment deployment = repositoryService.createDeployment()
  			.addBpmnModel("twoTasksTestProcess.bpmn20.xml", bpmnModel).deploy();
  	
  	autoCleanedUpDeploymentIds.add(deployment.getId()); // For auto-cleanup

  	ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
  			.deploymentId(deployment.getId()).singleResult();
  	return processDefinition.getId(); 
  }

	
}

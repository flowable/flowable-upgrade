package org.flowable.upgrade.test;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.junit.Test;

@RunOnlyWithTestDataFromVersion(versions = "6.4.0")
public class RuntimeActivityInstanceTest extends UpgradeTestCase {

    @Test
    public void testCompleteTask() {
        Task task = getTaskFromGeneratedData("completeTask");

        taskService.complete(task.getId());

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("simpleTask2").singleResult();
        assertThat(activityInstance, notNullValue());
        assertThat(activityInstance.getEndTime(), notNullValue());
        assertHistoricActivityInstancesIsSame(activityInstance);
    }

    @Test
    public void testChangeAssignee() {
        Task task = getTaskFromGeneratedData("changeAssignee");

        taskService.unclaim(task.getId());
        taskService.claim(task.getId(), "newAssignee");

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().processInstanceId(task.getProcessInstanceId()).activityId("simpleTask1").singleResult();
        assertThat(activityInstance, notNullValue());
        assertThat(activityInstance.getAssignee(), is("newAssignee"));
        assertHistoricActivityInstancesIsSame(activityInstance);
    }

    @Test
    public void testChangeOwner() {
        Task task = getTaskFromGeneratedData("changeOwner");

        taskService.setOwner(task.getId(), "newOwner");
        taskService.complete(task.getId());

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().processInstanceId(task.getProcessInstanceId()).activityId("simpleTask1").singleResult();
        assertThat(activityInstance, notNullValue());
        assertThat(activityInstance.getEndTime(), notNullValue());
        assertHistoricActivityInstancesIsSame(activityInstance);
    }

    @Test
    public void testCallSimpleSubProcess() {
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();
        assertNotNull(subProcessInstance);

        Task task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(subProcessInstance.getId()).count(), is(0l));

        taskService.complete(task.getId());

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().processInstanceId(task.getProcessInstanceId()).activityId("subProcessTask").singleResult();
        assertThat(activityInstance, notNullValue());
        assertThat(activityInstance.getEndTime(), notNullValue());
        assertHistoricActivityInstancesIsSame(activityInstance);

        Execution mainExecution = runtimeService.createExecutionQuery().processDefinitionKey("calledProcess").onlyChildExecutions().singleResult();
        runtimeService.trigger(mainExecution.getId());

        activityInstance = runtimeService.createActivityInstanceQuery().processInstanceId(task.getProcessInstanceId()).activityId("subProcessTask").singleResult();
        assertThat("Activity instance from subprocess is deleted", activityInstance, nullValue());
    }

    protected Task getTaskFromGeneratedData(String processBusinessKey) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(processBusinessKey).singleResult();
        assertNotNull(processInstance);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).count(), is(0l));
        return task;
    }

    protected void assertHistoricActivityInstancesIsSame(ActivityInstance activityInstance) {
        HistoricActivityInstance historicActInst = historyService.createHistoricActivityInstanceQuery().activityInstanceId(activityInstance.getId()).singleResult();
        assertEquals(historicActInst.getId(), activityInstance.getId());
        assertEquals(historicActInst.getActivityId(), activityInstance.getActivityId());
        assertEquals(historicActInst.getEndTime(), activityInstance.getEndTime());
        assertEquals(historicActInst.getProcessDefinitionId(), activityInstance.getProcessDefinitionId());
        assertEquals(historicActInst.getStartTime(), activityInstance.getStartTime());
        assertEquals(historicActInst.getExecutionId(), activityInstance.getExecutionId());
        assertEquals(historicActInst.getActivityType(), activityInstance.getActivityType());
        assertEquals(historicActInst.getProcessInstanceId(), activityInstance.getProcessInstanceId());
        assertEquals(historicActInst.getAssignee(), activityInstance.getAssignee());
        assertEquals(historicActInst.getDurationInMillis(), activityInstance.getDurationInMillis());
        assertEquals(historicActInst.getTenantId(), activityInstance.getTenantId());
        assertEquals(historicActInst.getDeleteReason(), activityInstance.getDeleteReason());
        assertEquals(historicActInst.getActivityName(), activityInstance.getActivityName());
        assertEquals(historicActInst.getCalledProcessInstanceId(), activityInstance.getCalledProcessInstanceId());
        assertEquals(historicActInst.getTaskId(), activityInstance.getTaskId());
        assertEquals(historicActInst.getTime(), activityInstance.getTime());
    }

}
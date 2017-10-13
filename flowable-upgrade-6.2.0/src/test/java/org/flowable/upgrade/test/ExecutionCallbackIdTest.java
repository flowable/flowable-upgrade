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

import java.util.List;

import org.flowable.cmmn.api.PlanItemInstanceCallbackType;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Test;

/**
 * In 6.2.0, callback identifier and callback type were added to the executions/process instances/historic process instances
 * 
 * @author Joram Barrez
 */
public class ExecutionCallbackIdTest extends UpgradeTestCase {
    
    @Test
    public void testCallbackIdSet() {
        Deployment deployment = repositoryService.createDeployment().addClasspathResource("one-user-task.bpmn20.xml").deploy();
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment().addClasspathResource("ExecutionCallbackIdTest.cmmn").deploy();
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("ExecutionCallbackIdTest").start();
        assertNotNull(caseInstance);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneUserTask").count());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneUserTask").singleResult();
        assertNotNull(processInstance);
        assertEquals(planItemInstances.get(0).getId(), processInstance.getCallbackId());
        assertEquals(PlanItemInstanceCallbackType.CHILD_PROCESS, processInstance.getCallbackType());
        
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertNotNull(historicProcessInstance);
        assertEquals(processInstance.getCallbackId(), historicProcessInstance.getCallbackId());
        assertEquals(processInstance.getCallbackType(), historicProcessInstance.getCallbackType());
        
        repositoryService.deleteDeployment(deployment.getId(), true);
        cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
    }

}

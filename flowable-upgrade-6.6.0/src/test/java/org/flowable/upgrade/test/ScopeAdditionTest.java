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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.Test;

/**
 * In 6.2.0, scope identitfier/type was added to tasks and variables.
 * 
 * This test works against any previous version.
 * 
 * @author Joram Barrrez
 */
public class ScopeAdditionTest extends UpgradeTestCase {
    
    @Test
    public void testScopeAdditionToTask() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment().addClasspathResource("one-human-task.cmmn").deploy();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertNotNull(task.getScopeId());
        assertNotNull(task.getSubScopeId());
        assertNotNull(task.getScopeDefinitionId());
        assertNotNull(task.getScopeType());
        
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskId(task.getId())
                .singleResult();
        assertEquals(task.getScopeId(), historicTaskInstance.getScopeId());
        assertEquals(task.getSubScopeId(), historicTaskInstance.getSubScopeId());
        assertEquals(task.getScopeDefinitionId(), historicTaskInstance.getScopeDefinitionId());
        assertEquals(task.getScopeType(), historicTaskInstance.getScopeType());
        
        cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
    }
    
    @Test
    public void testScopeAdditionToVariables() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment().addClasspathResource("one-human-task.cmmn").deploy();
        final CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varA", "a")
                .variable("varB", "b")
                .variable("varC", "c")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        
        List<VariableInstanceEntity> variableInstanceEntities = managementService.executeCommand(new Command<List<VariableInstanceEntity>>() {
            @Override
            public List<VariableInstanceEntity> execute(CommandContext commandContext) {
            	ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
                VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
            	return variableService.findVariableInstanceByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN);
            }
        });
        
        assertEquals(3, variableInstanceEntities.size());
        for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
            assertNotNull("no scope id", variableInstanceEntity.getScopeId());
            assertNotNull("no scope type", variableInstanceEntity.getScopeType());
            assertNull("No sub scope should be set", variableInstanceEntity.getSubScopeId());
        }
        
        List<HistoricVariableInstanceEntity> historicVariableInstances = managementService.executeCommand(new Command<List<HistoricVariableInstanceEntity>>() {
            @Override
            public List<HistoricVariableInstanceEntity> execute(CommandContext commandContext) {
            	ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
            	VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();
                return variableServiceConfiguration.getHistoricVariableInstanceEntityManager()
                        .findHistoricalVariableInstancesByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN);
            }
        });
        assertEquals(3, historicVariableInstances.size());
        for (HistoricVariableInstanceEntity historicVariableInstanceEntity : historicVariableInstances) {
            assertNotNull("no scope id", historicVariableInstanceEntity.getScopeId());
            assertNotNull("no scope type", historicVariableInstanceEntity.getScopeType());
            assertNull("No sub scope should be set", historicVariableInstanceEntity.getSubScopeId());
        }
        
        cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
    }
    

}

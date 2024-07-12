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

import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.upgrade.TestAnnotationUtil;
import org.flowable.upgrade.helper.UpgradeUtil;
import org.junit.Before;
import org.junit.Ignore;

/**
 * @author Joram Barrez
 */
@Ignore
public abstract class UpgradeTestCase {
    
    protected AppEngine appEngine;
    protected AppEngineConfiguration appEngineConfiguration;

    protected ProcessEngine processEngine;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected TaskService taskService;
    protected RuntimeService runtimeService;
    protected RepositoryService repositoryService;
    protected ManagementService managementService;
    protected FormService formService;
    protected IdentityService identityService;
    protected HistoryService historyService;

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnManagementService cmmnManagementService;

    protected boolean runningTests;

    @Before
    public void setup() {
        this.appEngine = UpgradeUtil.getAppEngine("flowable.cfg.xml");
        this.appEngineConfiguration = appEngine.getAppEngineConfiguration();
        
        this.processEngineConfiguration = (ProcessEngineConfigurationImpl) appEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        this.taskService = processEngine.getTaskService();
        this.runtimeService = processEngine.getRuntimeService();
        this.repositoryService = processEngine.getRepositoryService();
        this.managementService = processEngine.getManagementService();
        this.formService = processEngine.getFormService();
        this.identityService = processEngine.getIdentityService();
        this.historyService = processEngine.getHistoryService();

        this.cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
        this.cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();

        this.runningTests = TestAnnotationUtil.validateRunOnlyWithTestDataFromVersionAnnotation(this.getClass());
    }

}

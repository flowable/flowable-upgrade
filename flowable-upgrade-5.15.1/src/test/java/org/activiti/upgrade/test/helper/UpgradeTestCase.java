package org.activiti.upgrade.test.helper;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.upgrade.helper.ActivitiVersion;
import org.activiti.upgrade.helper.UpgradeUtil;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
public class UpgradeTestCase {
	
	private static final Logger logger = LoggerFactory.getLogger(UpgradeTestCase.class);
  
  protected ProcessEngine processEngine;
  protected TaskService taskService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  protected FormService formService;
  protected IdentityService identityService;
  protected HistoryService historyService;
  
  @Before
  public void setup() {
    this.processEngine = UpgradeUtil.getProcessEngine("activiti.cfg.xml");
    this.taskService = processEngine.getTaskService();
    this.runtimeService = processEngine.getRuntimeService();
    this.repositoryService = processEngine.getRepositoryService();
    this.managementService = processEngine.getManagementService();
    this.formService = processEngine.getFormService();
    this.identityService = processEngine.getIdentityService();
    this.historyService = processEngine.getHistoryService();
    
    // verify if there is an minimal version requirement on the test
    RunOnlyWithTestDataFromVersion runOnlyWithTestDataFromVersion = this.getClass().getAnnotation(RunOnlyWithTestDataFromVersion.class);
    if (runOnlyWithTestDataFromVersion != null) {
      
      String[] versions = runOnlyWithTestDataFromVersion.versions();
      boolean versionMatches = false;
      
      int index = 0;
      while (!versionMatches && index < versions.length) {
        ActivitiVersion requiredVersion = new ActivitiVersion(versions[index]);
        ActivitiVersion oldVersion = UpgradeUtil.getOldVersion(); // This is the version against which the data was generated
        versionMatches = oldVersion.compareTo(requiredVersion) == 0;
        
        index++;
      }
      
      if (versionMatches) {
      	logger.info("@RunOnlyWithTestDataFromVersion found on test " + this.getClass() + ": version matches. Running test");
      } else {
      	logger.info("@RunOnlyWithTestDataFromVersion found on test " + this.getClass() + ": version DOES NOT match. This test will not run.");
      }
    
      // A failed assumption does not mean the code is broken, but that the test provides no useful information
      Assume.assumeTrue(versionMatches);
      
    } else {
    	logger.info("No @RunOnlyWithTestDataFromVersion found on test " + this.getClass() + ": running test.");
    }
    
  }
  
}

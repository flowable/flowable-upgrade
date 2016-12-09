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
package org.flowable.upgrade.test.helper;

import org.flowable.upgrade.helper.ActivitiVersion;
import org.flowable.upgrade.helper.UpgradeUtil;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez 
 */
public class TestAnnotationUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(TestAnnotationUtil.class);
	
	public static boolean validateRunOnlyWithTestDataFromVersionAnnotation(Class<?> clazz) {
		// verify if there is an minimal version requirement on the test
    RunOnlyWithTestDataFromVersion runOnlyWithTestDataFromVersion = clazz.getAnnotation(RunOnlyWithTestDataFromVersion.class);
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
      	logger.info("@RunOnlyWithTestDataFromVersion found on test " + clazz + ": version matches. Running test");
      } else {
      	logger.info("@RunOnlyWithTestDataFromVersion found on test " + clazz + ": version DOES NOT match. This test will not run.");
      }
    
      // A failed assumption does not mean the code is broken, but that the test provides no useful information
      // This effectively stops further test execution
      Assume.assumeTrue(versionMatches);
      return versionMatches;
      
    } else {
    	logger.info("No @RunOnlyWithTestDataFromVersion found on test " + clazz + ": running test.");
    	return true;
    }
	}

}

package org.activiti.upgrade.test.helper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * Annotation for upgrade unit test extending from {@link UpgradeTestCase}.
 * Allows to indicate where this test must be executed, eg
 * 
 * <code>@RunOnlyWithTestDataFromVersion(versions = {"5.7", "5.8", "5.9", "5.10"})</code>
 * 
 * Means that the 5.7 - 5.10 data generators create test data usable by this test.
 * This probably means that the bug was fixed in 5.11. As such, executing the test
 * doesn't make sense on 5.11+, as the test data would correct (ie not bugged).
 * 
 * @author Joram Barrez
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RunOnlyWithTestDataFromVersion {
  
  String[] versions();

}

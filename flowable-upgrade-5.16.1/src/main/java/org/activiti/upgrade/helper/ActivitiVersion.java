package org.activiti.upgrade.helper;
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
public class ActivitiVersion implements Comparable<ActivitiVersion> {
  
  private int majorVersion;
  private int minorVersion;
  
  public ActivitiVersion(String version) {
    String[] splittedVersion = version.replace("-SNAPSHOT", "").split("\\.");
    this.majorVersion = Integer.valueOf(splittedVersion[0]);
    this.minorVersion = Integer.valueOf(splittedVersion[1]);
  }
  
  @Override
  public int compareTo(ActivitiVersion other) {
    if (getMajorVersion() == other.getMajorVersion() && getMinorVersion() == other.getMinorVersion()) {
      return 0;
    } else if ( (getMajorVersion() < other.getMajorVersion()) 
            || ( (getMajorVersion() == other.getMajorVersion()) && (getMinorVersion() < other.getMinorVersion()) ) ) {
      return -1;
    } else {
      return 1;
    }
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public void setMajorVersion(int majorVersion) {
    this.majorVersion = majorVersion;
  }

  public int getMinorVersion() {
    return minorVersion;
  }

  public void setMinorVersion(int minorVersion) {
    this.minorVersion = minorVersion;
  }
  
}
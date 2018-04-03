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
package org.flowable.upgrade;

/**
 * @author Joram Barrez
 */
public class EngineVersion implements Comparable<EngineVersion> {

    private int majorVersion;
    private int minorVersion;
    private int microVersion = -1;

    public EngineVersion(String version) {
        String[] splittedVersion = version.replace("-SNAPSHOT", "").split("\\.");
        this.majorVersion = Integer.valueOf(splittedVersion[0]);
        this.minorVersion = Integer.valueOf(splittedVersion[1]);
        if (splittedVersion.length > 2) {
            microVersion = Integer.valueOf(splittedVersion[2]);
        }
    }

    @Override
    public int compareTo(EngineVersion other) {
        if (getMajorVersion() == other.getMajorVersion() 
                && getMinorVersion() == other.getMinorVersion()
                && getMicroVersion() == other.getMicroVersion()) {
            return 0;
        } else if ((getMajorVersion() < other.getMajorVersion())
                || ((getMajorVersion() == other.getMajorVersion()) && (getMinorVersion() < other.getMinorVersion()))) {
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

    public int getMicroVersion() {
        return microVersion;
    }

    public void setMicroVersion(int microVersion) {
        this.microVersion = microVersion;
    }
    
}

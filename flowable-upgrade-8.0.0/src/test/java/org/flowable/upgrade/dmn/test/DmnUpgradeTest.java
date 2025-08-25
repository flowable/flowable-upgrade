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
package org.flowable.upgrade.dmn.test;

import static org.junit.Assert.assertEquals;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.upgrade.test.UpgradeTestCase;
import org.junit.Test;

public class DmnUpgradeTest extends UpgradeTestCase {

    @Test
    public void testVersion() {
    	String dmnSchemaVersion = dmnEngineConfiguration.getCommandExecutor().execute(new Command<String>() {

			@Override
			public String execute(CommandContext commandContext) {
				PropertyEntityManager propertyEntityManager = dmnEngineConfiguration.getPropertyEntityManager();
				PropertyEntity propertyEntity = propertyEntityManager.findById("dmn.schema.version");
				if (propertyEntity != null) {
					return propertyEntity.getValue();
				}
				
				return null;
			}
    		
    	});
    	
    	assertEquals("7.2.0.2", dmnSchemaVersion);
        
    }
}

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

import java.util.logging.Logger;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;


/**
 * @author Joram Barrez
 */
public class UpgradeUtil {
  
 private static Boolean DATABASE_DROPPED = false;
  
  private static final Logger LOG = Logger.getLogger(UpgradeUtil.class.getName());
  
  public static ProcessEngine getProcessEngine() {
    return getProcessEngine("activiti.cfg.xml");
  }

  public static ProcessEngine getProcessEngine(String configResource) {
    ProcessEngineConfigurationImpl processEngineConfiguration 
      = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(configResource);
    
    // When the 'old version' tests are run, we drop the schema always once for the first test
    if (!DATABASE_DROPPED && isTestRunningAgainstOldVersion()) {
      synchronized (DATABASE_DROPPED) {
        if (!DATABASE_DROPPED) {
          
          LOG.info("JdbcDriver = " + processEngineConfiguration.getJdbcDriver());
          LOG.info("JdbcURL = " + processEngineConfiguration.getJdbcUrl());
          LOG.info("JdbcUser = " + processEngineConfiguration.getJdbcUsername());
          LOG.info("JdbcPassword = " + processEngineConfiguration.getJdbcPassword());
          
          PooledDataSource dataSource = new PooledDataSource(processEngineConfiguration.getJdbcDriver(),
                  processEngineConfiguration.getJdbcUrl(),
                  processEngineConfiguration.getJdbcUsername(),
                  processEngineConfiguration.getJdbcPassword());
          
          DatabaseConnection connection = null;
          Database database = null;
          try {
            connection = new JdbcConnection(dataSource.getConnection());
            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            Liquibase liquibase = new Liquibase((String) null, new ClassLoaderResourceAccessor(), database);
            LOG.info("Dropping upgrade database...");
            liquibase.dropAll();
          } catch (Exception exception) {
            exception.printStackTrace();
            
            if (connection != null) {
              try {
                connection.close();
              } catch (DatabaseException e) {
                e.printStackTrace();
              }
            }
            
            if (database != null) {
              try {
                database.close();
              } catch (DatabaseException e) {
                e.printStackTrace();
              }
            }
          }
          
          LOG.info("Dropping upgrade database completed");
          DATABASE_DROPPED = true;
        }
      }
    }
    
    // Buidling the process engine will also recreate the schema (in that particular version)
    return processEngineConfiguration.buildProcessEngine();
  }
  
  protected static boolean isTestRunningAgainstOldVersion() {
    String runningTest = System.getProperty("maven.test.skip"); // We're piggybacking on the maven test skip property to know if we're generating data in the old version
    return runningTest != null && runningTest.equals("true");
  }
  
  public static ActivitiVersion getOldVersion() {
    String oldVersion = System.getProperty("oldVersion");
    return new ActivitiVersion(oldVersion);
  }


  
}

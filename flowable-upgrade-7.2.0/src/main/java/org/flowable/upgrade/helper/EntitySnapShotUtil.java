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
package org.flowable.upgrade.helper;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.impl.db.EntityToTableMap;
import org.flowable.engine.impl.persistence.entity.AttachmentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricScopeInstanceEntityImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class EntitySnapShotUtil {

    public static class EntitySnapShot {

        protected List<List<? extends Object>> entitiesList;

        public EntitySnapShot(List<List<? extends Object>> entitiesList) {
            this.entitiesList = entitiesList;
        }

        public List<List<? extends Object>> getEntitiesList() {
            return entitiesList;
        }

        public void setEntitiesList(List<List<? extends Object>> entitiesList) {
            this.entitiesList = entitiesList;
        }

    }

    public static EntitySnapShot createEntitySnapshot(final ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<List<? extends Object>> list = new ArrayList<List<? extends Object>>();

        final List<ProcessInstance> allProcessInstances = processEngineConfiguration.getRuntimeService().createProcessInstanceQuery().list();
        final List<Task> allTasks = processEngineConfiguration.getTaskService().createTaskQuery().list();
        final List<Deployment> allDeployments = processEngineConfiguration.getRepositoryService().createDeploymentQuery().list();

        list.add(processEngineConfiguration.getHistoryService().createHistoricDetailQuery().list());
        list.add(processEngineConfiguration.getHistoryService().createHistoricVariableInstanceQuery().list());
        list.add(processEngineConfiguration.getHistoryService().createHistoricTaskInstanceQuery().list());
        list.add(processEngineConfiguration.getHistoryService().createHistoricProcessInstanceQuery().list());
        list.add(processEngineConfiguration.getHistoryService().createHistoricActivityInstanceQuery().list());

        list.add(processEngineConfiguration.getIdentityService().createUserQuery().list());
        list.add(processEngineConfiguration.getIdentityService().createGroupQuery().list());

        list.add(processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery().list());

        list.add(orderExecutionsForInsertionOrder(processEngineConfiguration.getRuntimeService().createExecutionQuery().list()));
        list.add(allTasks);
        
        IdentityLinkService identityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService();
        HistoricIdentityLinkService historicIdentityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService();
        
        // Historic identity links
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                List<HistoricIdentityLinkEntity> historicIdentityLinks = new ArrayList<HistoricIdentityLinkEntity>();
                for (ProcessInstance processInstance : allProcessInstances) {
                    historicIdentityLinks.addAll(historicIdentityLinkService.findHistoricIdentityLinksByProcessInstanceId(processInstance.getId()));
                }
                for (Task task : allTasks) {
                    historicIdentityLinks.addAll(historicIdentityLinkService.findHistoricIdentityLinksByTaskId(task.getId()));
                }
                return historicIdentityLinks;
            }
        }));

        // Identity links
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                List<IdentityLinkEntity> identityLinks = new ArrayList<IdentityLinkEntity>();
                for (ProcessInstance processInstance : allProcessInstances) {
                    identityLinks.addAll(identityLinkService.findIdentityLinksByProcessInstanceId(processInstance.getId()));
                }
                for (Task task : allTasks) {
                    identityLinks.addAll(identityLinkService.findIdentityLinksByTaskId(task.getId()));
                }
                return identityLinks;
            }
        }));

        // Event subscriptions
        EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration = processEngineConfiguration.getEventSubscriptionServiceConfiguration();
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                EventSubscriptionQueryImpl query = new EventSubscriptionQueryImpl(commandContext, eventSubscriptionServiceConfiguration);
                return eventSubscriptionServiceConfiguration.getEventSubscriptionService().findEventSubscriptionsByQueryCriteria(query);
            }
        }));

        list.add(allDeployments);

        // Byte arrays
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                return CommandContextUtil.getByteArrayEntityManager().findAll();
            }
        }));

        // Variables
        VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                List<VariableInstanceEntity> variables = new ArrayList<VariableInstanceEntity>();
                for (ProcessInstance processInstance : allProcessInstances) {
                    variables.addAll(variableService.findVariableInstancesByExecutionId(processInstance.getId()));
                }
                for (Task task : allTasks) {
                    variables.addAll(variableService.createInternalVariableInstanceQuery().taskId(task.getId()).list());
                }
                return variables;
            }
        }));

        list.add(processEngineConfiguration.getManagementService().createJobQuery().list());
        list.add(processEngineConfiguration.getManagementService().createTimerJobQuery().list());
        list.add(processEngineConfiguration.getManagementService().createDeadLetterJobQuery().list());
        list.add(processEngineConfiguration.getManagementService().createSuspendedJobQuery().list());

        list.add(processEngineConfiguration.getManagementService().getEventLogEntries(0L, (long) Integer.MAX_VALUE));

        // Comments
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                List<Object> variables = new ArrayList<Object>();
                for (ProcessInstance processInstance : allProcessInstances) {
                    variables.addAll(CommandContextUtil.getCommentEntityManager()
                            .findCommentsByProcessInstanceId(processInstance.getId()));
                }
                for (Task task : allTasks) {
                    variables.addAll(CommandContextUtil.getCommentEntityManager().findCommentsByTaskId(task.getId()));
                }
                return variables;
            }
        }));

        // Attachments
        list.add(processEngineConfiguration.getManagementService().executeCommand(new Command<List<? extends Object>>() {
            public List<? extends Object> execute(CommandContext commandContext) {
                List<AttachmentEntity> attachments = new ArrayList<AttachmentEntity>();
                for (ProcessInstance processInstance : allProcessInstances) {
                    attachments.addAll(CommandContextUtil.getAttachmentEntityManager()
                            .findAttachmentsByProcessInstanceId(processInstance.getId()));
                }
                for (Task task : allTasks) {
                    attachments.addAll(
                            CommandContextUtil.getAttachmentEntityManager().findAttachmentsByTaskId(task.getId()));
                }
                return attachments;
            }
        }));

        return new EntitySnapShot(list);
    }

    protected static List<Execution> orderExecutionsForInsertionOrder(List<Execution> executions) {
        List<Execution> result = new ArrayList<Execution>(executions.size());
        List<Execution> rootExecutions = new ArrayList<Execution>();
        Map<String, Set<Execution>> parentChildMap = new HashMap<String, Set<Execution>>();

        // Fill parent-child map and find root executions
        for (Execution execution : executions) {

            // Root executions
            if (execution.getParentId() == null && ((ExecutionEntityImpl) execution).getSuperExecutionId() == null) {
                rootExecutions.add(execution);
            }

            // Parent-child map
            String parentExecutionId = execution.getParentId();
            if (parentExecutionId != null) {
                if (!parentChildMap.containsKey(parentExecutionId)) {
                    parentChildMap.put(parentExecutionId, new HashSet<Execution>());
                }
                parentChildMap.get(parentExecutionId).add(execution);
            }

            String superExecutionId = ((ExecutionEntityImpl) execution).getSuperExecutionId();
            if (superExecutionId != null) {
                if (!parentChildMap.containsKey(superExecutionId)) {
                    parentChildMap.put(superExecutionId, new HashSet<Execution>());
                }
                parentChildMap.get(superExecutionId).add(execution);
            }
        }

        // Create ordered list
        for (Execution rootExecution : rootExecutions) {
            result.add(rootExecution);
            if (parentChildMap.containsKey(rootExecution.getId())) {
                LinkedList<Execution> currentParents = new LinkedList<Execution>();
                currentParents.add(rootExecution);
                while (!currentParents.isEmpty()) {
                    Execution currentParent = currentParents.pop();
                    if (parentChildMap.containsKey(currentParent.getId())) {
                        for (Execution childExecution : parentChildMap.get(currentParent.getId())) {
                            result.add(childExecution);
                            currentParents.add(childExecution);
                        }
                    }
                }

            }
        }

        return result;
    }

    public static void restore(final ProcessEngineConfigurationImpl processEngineConfiguration, final EntitySnapShot entitySnapshot) {

        deleteAll(processEngineConfiguration);

        processEngineConfiguration.getManagementService().executeCommand(new Command<Void>() {
            public Void execute(CommandContext commandContext) {
                for (List<? extends Object> entityList : entitySnapshot.getEntitiesList()) {
                    for (Object entity : entityList) {
                        CommandContextUtil.getDbSqlSession().insert((Entity) entity, processEngineConfiguration.getIdGenerator());
                    }
                }
                return null;
            }
        });
    }

    public static void deleteAll(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.getManagementService().executeCommand(new Command<Void>() {
            public Void execute(CommandContext commandContext) {
                for (Class<? extends Entity> entity : EntityDependencyOrder.DELETE_ORDER) {
                    if (isDeleteable(entity)) {
                        Statement statement = null;
                        try {
                            statement = CommandContextUtil.getDbSqlSession().getSqlSession().getConnection()
                                    .createStatement();
                            String table = entityClassToTable(entity);
                            if (table != null) {
                                statement.executeUpdate("delete from " + table);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            if (statement != null) {
                                try {
                                    statement.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

    public static boolean isDeleteable(Class<? extends Entity> clazz) {
        return !clazz.equals(HistoricScopeInstanceEntityImpl.class) && !clazz.equals(PropertyEntityImpl.class);
    }

    protected static String entityClassToTable(Class<? extends Entity> entityClass) {
        Map<Class<? extends Entity>, String> entityToTableNameMap = EntityToTableMap.entityToTableNameMap;
        if (entityToTableNameMap.containsKey(entityClass)) {
            return entityToTableNameMap.get(entityClass);
        }

        Class[] interfaces = entityClass.getInterfaces();
        for (Class interfaceClass : interfaces) {
            if (entityToTableNameMap.containsKey(interfaceClass)) {
                return entityToTableNameMap.get(interfaceClass);
            }
        }
        return null;
    }

}

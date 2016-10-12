package org.activiti.upgrade.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.event.EventLogEntry;
import org.activiti.engine.impl.event.logger.EventLogger;
import org.activiti.engine.impl.event.logger.handler.Fields;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.util.CollectionUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.upgrade.test.helper.UpgradeTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * In the 5.16 release, database event logging was added.
 * 
 * This is a copy of the test found in the test suite.
 * It starts a process and checks if event logs are correctly created,
 * which wasn't possible in releases before that.
 * 
 * @author Joram Barrez
 */
public class DatabaseEventLoggerTest extends UpgradeTestCase {
	
	protected EventLogger databaseEventLogger;
	
	protected ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void setup() {
		super.setup();
	  databaseEventLogger = new EventLogger(processEngineConfiguration.getClock());
	  runtimeService.addEventListener(databaseEventLogger);
	}
	
	@After
	public void tearDown() throws Exception {
		runtimeService.removeEventListener(databaseEventLogger);
	}
	
	@Test
	public void testDatabaseEvents() throws JsonParseException, JsonMappingException, IOException {
		
		Deployment deployment = repositoryService.createDeployment()
			.addClasspathResource("org/activiti/upgrade/test/DatabaseEventLoggerProcess.bpmn20.xml")
			.tenantId("myTenant")
			.deploy();
		
		int nrOfEntries = managementService.getEventLogEntries(null, null).size();
		
		// Run process to gather data
		ProcessInstance processInstance = 
				runtimeService.startProcessInstanceByKeyAndTenantId("DatabaseEventLoggerProcess", CollectionUtil.singletonMap("testVar", "helloWorld"), "myTenant");
		
		
			// Verify event log entries
			List<EventLogEntry> eventLogEntries = managementService.getEventLogEntries(null, null);
			
			String processDefinitionId = processInstance.getProcessDefinitionId();
			Iterator<EventLogEntry> iterator = eventLogEntries.iterator();
			while (iterator.hasNext()) {
				EventLogEntry entry = iterator.next();
				if (entry.getProcessDefinitionId() != null && !entry.getProcessDefinitionId().equals(processDefinitionId)) {
					iterator.remove();
				}
			}
			
			assertEquals(nrOfEntries + 15, eventLogEntries.size());
			
			long lastLogNr = -1;
			for (int i=0; i< eventLogEntries.size(); i++) {
				
				EventLogEntry entry = eventLogEntries.get(i);
				
				if (i == 0) {
					
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.VARIABLE_CREATED.name());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.PROCESS_INSTANCE_ID));
					assertNotNull(data.get(Fields.VALUE_STRING));
				}
				
				// process instance start
				if (i == 1) {
					
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), "PROCESSINSTANCE_START");
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.TENANT_ID));
					
					@SuppressWarnings("unchecked")
	        Map<String, Object> variableMap = (Map<String, Object>) data.get(Fields.VARIABLES);
	        assertEquals(1, variableMap.size());
	        assertEquals("helloWorld", variableMap.get("testVar"));
					
					assertFalse(data.containsKey(Fields.NAME));
					assertFalse(data.containsKey(Fields.BUSINESS_KEY));
				}
				
				
				// Activity started
				if (i == 2 || i == 5 || i == 8 || i == 11)
				
				// Leaving start
				if (i == 3) {
					
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.ACTIVITY_COMPLETED.name());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ACTIVITY_ID));
					assertEquals("startEvent1", data.get(Fields.ACTIVITY_ID));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.PROCESS_INSTANCE_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.BEHAVIOR_CLASS));
					
				}
				
				// Sequence flow taken
				if (i == 4 || i == 7 || i == 11) {
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.SEQUENCEFLOW_TAKEN.name());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_ID));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_NAME));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_BEHAVIOR_CLASS));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_ID));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_NAME));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_BEHAVIOR_CLASS));
				}
				
				// Leaving parallel gateway
				if (i == 6) {
					
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.ACTIVITY_COMPLETED.name());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ACTIVITY_ID));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.PROCESS_INSTANCE_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.BEHAVIOR_CLASS));
					
				}
				
				// Tasks
				if (i == 10 || i == 14) {
				
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.TASK_CREATED.name());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getExecutionId());
					assertNotNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.NAME));
					assertNotNull(data.get(Fields.ASSIGNEE));
					assertNotNull(data.get(Fields.CREATE_TIME));
					assertNotNull(data.get(Fields.PRIORITY));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.TENANT_ID));
					
					assertFalse(data.containsKey(Fields.DESCRIPTION));
					assertFalse(data.containsKey(Fields.CATEGORY));
					assertFalse(data.containsKey(Fields.OWNER));
					assertFalse(data.containsKey(Fields.DUE_DATE));
					assertFalse(data.containsKey(Fields.FORM_KEY));
					assertFalse(data.containsKey(Fields.USER_ID));
					
				}
				
				if (i == 9 || i == 13) {
					
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.TASK_ASSIGNED.name());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getExecutionId());
					assertNotNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.NAME));
					assertNotNull(data.get(Fields.ASSIGNEE));
					assertNotNull(data.get(Fields.CREATE_TIME));
					assertNotNull(data.get(Fields.PRIORITY));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.TENANT_ID));
					
					assertFalse(data.containsKey(Fields.DESCRIPTION));
					assertFalse(data.containsKey(Fields.CATEGORY));
					assertFalse(data.containsKey(Fields.OWNER));
					assertFalse(data.containsKey(Fields.DUE_DATE));
					assertFalse(data.containsKey(Fields.FORM_KEY));
					assertFalse(data.containsKey(Fields.USER_ID));
					
				}
				
				lastLogNr = entry.getLogNumber();
			}
			
			// Completing two tasks
			for (Task task : taskService.createTaskQuery().processDefinitionId(processDefinitionId).list()) {
				Authentication.setAuthenticatedUserId(task.getAssignee());
				Map<String, Object> varMap = new HashMap<String, Object>();
		    varMap.put("test", "test");
				taskService.complete(task.getId(), varMap);
				Authentication.setAuthenticatedUserId(null);
			}
			
			// Verify events
			eventLogEntries = managementService.getEventLogEntries(lastLogNr, 100L);
			
			iterator = eventLogEntries.iterator();
			while (iterator.hasNext()) {
				EventLogEntry entry = iterator.next();
				if (entry.getProcessDefinitionId() != null && !entry.getProcessDefinitionId().equals(processDefinitionId)) {
					iterator.remove();
				}
			}
			
			assertEquals(15, eventLogEntries.size());
			
			for (int i=0; i< eventLogEntries.size(); i++) {
				
				EventLogEntry entry = eventLogEntries.get(i);
				
				// Task completion 
				if (i == 1 || i == 6) {
					assertNotNull(entry.getType());
					assertEquals(ActivitiEventType.TASK_COMPLETED.name(), entry.getType());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getExecutionId());
					assertNotNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.NAME));
					assertNotNull(data.get(Fields.ASSIGNEE));
					assertNotNull(data.get(Fields.CREATE_TIME));
					assertNotNull(data.get(Fields.PRIORITY));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.TENANT_ID));
					assertNotNull(data.get(Fields.USER_ID));
					@SuppressWarnings("unchecked")
	        Map<String, Object> variableMap = (Map<String, Object>) data.get(Fields.VARIABLES);
					assertEquals(1, variableMap.size());
					assertEquals("test", variableMap.get("test"));
					
					assertFalse(data.containsKey(Fields.DESCRIPTION));
					assertFalse(data.containsKey(Fields.CATEGORY));
					assertFalse(data.containsKey(Fields.OWNER));
					assertFalse(data.containsKey(Fields.DUE_DATE));
					assertFalse(data.containsKey(Fields.FORM_KEY));
					
				}
				
				// Activity Completed
				if (i == 2 || i == 7 || i == 10 || i == 13) {
					assertNotNull(entry.getType());
					assertEquals(ActivitiEventType.ACTIVITY_COMPLETED.name(), entry.getType());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ACTIVITY_ID));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.PROCESS_INSTANCE_ID));
					assertNotNull(data.get(Fields.EXECUTION_ID));
					assertNotNull(data.get(Fields.ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.BEHAVIOR_CLASS));
					
					if (i == 1) {
						assertEquals("userTask", data.get(Fields.ACTIVITY_TYPE));
					} else if (i == 5) {
						assertEquals("userTask", data.get(Fields.ACTIVITY_TYPE));
					} else if (i == 8) {
						assertEquals("parallelGateway", data.get(Fields.ACTIVITY_TYPE));
					} else if (i == 11) {
						assertEquals("endEvent", data.get(Fields.ACTIVITY_TYPE));
					}
					
				}
				
				// Sequence flow taken
				if (i == 3 || i == 8 || i == 11) {
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), ActivitiEventType.SEQUENCEFLOW_TAKEN.name());
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNotNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_ID));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.SOURCE_ACTIVITY_BEHAVIOR_CLASS));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_ID));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_TYPE));
					assertNotNull(data.get(Fields.TARGET_ACTIVITY_BEHAVIOR_CLASS));
				}
					
				if (i == 14) {
					assertNotNull(entry.getType());
					assertEquals(entry.getType(), "PROCESSINSTANCE_END");
					assertNotNull(entry.getProcessDefinitionId());
					assertNotNull(entry.getProcessInstanceId());
					assertNotNull(entry.getTimeStamp());
					assertNull(entry.getExecutionId());
					assertNull(entry.getTaskId());
					
					Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>(){});
					assertNotNull(data.get(Fields.ID));
					assertNotNull(data.get(Fields.PROCESS_DEFINITION_ID));
					assertNotNull(data.get(Fields.TENANT_ID));
					
					assertFalse(data.containsKey(Fields.NAME));
					assertFalse(data.containsKey(Fields.BUSINESS_KEY));
				}
			}
		
		// Cleanup
		for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
			managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
		}
		
		repositoryService.deleteDeployment(deployment.getId(), true);
	}
	
	
}

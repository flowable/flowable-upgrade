package org.flowable.upgrade.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
@RunOnlyWithTestDataFromVersion(versions = "6.7.2")
public class EventSubscriptionLockingTest extends UpgradeTestCase {

    @Test
    public void testEventSubscriptionCanBeLockedAndUnlocked() {
        ProcessDefinition processDefinition = processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey("eventSubscriptionLocking").singleResult();

        EventSubscription eventSubscription = processEngineConfiguration.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription).isNotNull();

        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();

        EventSubscription finalEventSubscription = eventSubscription;
        processEngineConfiguration.getManagementService().executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.lockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = processEngineConfiguration.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription.getLockOwner()).isNotNull();
        assertThat(eventSubscription.getLockTime()).isNotNull();

        processEngineConfiguration.getManagementService().executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.unlockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = processEngineConfiguration.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();
    }

}

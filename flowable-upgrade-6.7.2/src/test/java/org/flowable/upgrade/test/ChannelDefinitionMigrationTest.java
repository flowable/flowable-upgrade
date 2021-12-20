package org.flowable.upgrade.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.upgrade.RunOnlyWithTestDataFromVersion;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
@RunOnlyWithTestDataFromVersion(versions = "6.7.1")
public class ChannelDefinitionMigrationTest extends UpgradeTestCase {

    @Test
    public void testChannelDefinitionsCorrectlyMigrated() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(ScopeTypes.EVENT_REGISTRY);
        EventRepositoryService eventRepositoryService = eventRegistryEngineConfiguration.getEventRepositoryService();

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("jmsInbound", "inbound", "jms"),
                        tuple("rabbitInbound", "inbound", "rabbit"),
                        tuple("jmsOutbound", "outbound", "jms"),
                        tuple("rabbitOutbound", "outbound", "rabbit")
                );
    }

}

package org.flowable.upgrade.test;

import java.util.List;

import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.test.helper.UpgradeTestCase;
import org.junit.Assert;
import org.junit.Test;

@RunOnlyWithTestDataFromVersion(versions = {"5.19.0"})
public class VerifyMessageStartEventProcessDefinitionIdTest extends UpgradeTestCase {
  
  @Test
  public void testProcessDefinitionIdSet() {
    Assert.assertEquals(1L, runtimeService.createProcessInstanceQuery().processDefinitionKey("messageTest").count());
    
    List<EventSubscriptionEntity> eventSubscriptionEntities = managementService.executeCommand(new Command<List<EventSubscriptionEntity>>() {
      @Override
      public List<EventSubscriptionEntity> execute(CommandContext commandContext) {
        EventSubscriptionQueryImpl query = new EventSubscriptionQueryImpl(commandContext);
        query.eventType("message");
        query.eventName("myStartMessage");
        return query.list();
      }
    });
    
    Assert.assertEquals(1, eventSubscriptionEntities.size());
    EventSubscriptionEntity eventSubscription = eventSubscriptionEntities.get(0);
    Assert.assertNotNull(eventSubscription.getProcessDefinitionId());
    Assert.assertNull(eventSubscription.getExecutionId());
    Assert.assertNull(eventSubscription.getProcessInstanceId());
    
  }

}

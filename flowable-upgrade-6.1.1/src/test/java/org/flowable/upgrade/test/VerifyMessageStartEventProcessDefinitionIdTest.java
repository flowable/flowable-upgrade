package org.flowable.upgrade.test;

import java.util.List;

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.upgrade.test.helper.RunOnlyWithTestDataFromVersion;
import org.flowable.upgrade.test.helper.UpgradeTestCase;
import org.junit.Assert;
import org.junit.Test;

@RunOnlyWithTestDataFromVersion(versions = {"5.19.0"})
public class VerifyMessageStartEventProcessDefinitionIdTest extends UpgradeTestCase {
  
  @Test
  public void testProcessDefinitionIdSet() {
    Assert.assertEquals(1L, runtimeService.createProcessInstanceQuery().processDefinitionKey("messageTest").count());
    
    List<EventSubscription> eventSubscriptionEntities = managementService.executeCommand(new Command<List<EventSubscription>>() {
      @Override
      public List<EventSubscription> execute(CommandContext commandContext) {
        EventSubscriptionQueryImpl query = new EventSubscriptionQueryImpl(commandContext);
        query.eventType("message");
        query.eventName("myStartMessage");
        return query.list();
      }
    });
    
    Assert.assertEquals(1, eventSubscriptionEntities.size());
    EventSubscription eventSubscription = eventSubscriptionEntities.get(0);
    Assert.assertNotNull(eventSubscription.getProcessDefinitionId());
    Assert.assertNull(eventSubscription.getExecutionId());
    Assert.assertNull(eventSubscription.getProcessInstanceId());
    
  }

}

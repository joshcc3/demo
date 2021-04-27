package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;

public interface IStackPresenterCallback {

    public void stacksConnectionLost(final String remoteAppName);

    public void stackStrategyUpdated(final StackStrategy strategy);

    public void stackGroupCreated(final StackGroup stackGroup, final StackClientHandler stackClientHandler);

    public void stackGroupUpdated(final StackGroup group);
}

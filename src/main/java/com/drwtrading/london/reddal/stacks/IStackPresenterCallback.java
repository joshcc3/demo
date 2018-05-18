package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;

public interface IStackPresenterCallback {

    public void stacksConnectionLost(final String remoteAppName);

    public void stackGroupCreated(final StackGroup stackGroup, final StackClientHandler stackClientHandler);

    public void stackGroupUpdated(final StackGroup group);
}

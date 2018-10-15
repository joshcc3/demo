package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;

public class StackPresenterMultiplexor implements IStackPresenterCallback {

    private final IStackPresenterCallback a;
    private final IStackPresenterCallback b;

    public StackPresenterMultiplexor(final IStackPresenterCallback a, final IStackPresenterCallback b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void stacksConnectionLost(final String remoteAppName) {

        a.stacksConnectionLost(remoteAppName);
        b.stacksConnectionLost(remoteAppName);
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup, final StackClientHandler stackClientHandler) {

        a.stackGroupCreated(stackGroup, stackClientHandler);
        b.stackGroupCreated(stackGroup, stackClientHandler);
    }

    @Override
    public void stackGroupUpdated(final StackGroup group) {

        a.stackGroupUpdated(group);
        b.stackGroupUpdated(group);
    }
}

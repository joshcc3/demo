package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import org.jetlang.channels.Publisher;

public class StackManagerGroupCallbackBatcher extends StackGroupCallbackBatcher {

    private final Publisher<String> stackFamilySymbolPublisher;

    public StackManagerGroupCallbackBatcher(final LadderPresenter ladderPresenter, final Publisher<String> stackFamilySymbolPublisher) {

        super(ladderPresenter);
        this.stackFamilySymbolPublisher = stackFamilySymbolPublisher;
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {
        super.stackGroupCreated(stackGroup);
        stackFamilySymbolPublisher.publish(stackGroup.getSymbol());
    }
}

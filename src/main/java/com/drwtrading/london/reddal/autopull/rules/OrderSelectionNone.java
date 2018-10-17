package com.drwtrading.london.reddal.autopull.rules;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.util.Struct;

public class OrderSelectionNone extends Struct implements IOrderSelection {

    public static final OrderSelectionNone NONE = new OrderSelectionNone();

    @Override
    public boolean isSelectionMet(final WorkingOrder order) {
        return false;
    }
}

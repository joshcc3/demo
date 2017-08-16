package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.Set;

public class RemoteOrderTypeTest {

    @Test
    public void allOldTypesAccountedForTest() {

        final Set<eeif.execution.RemoteOrderType> accountedForOrderTypes =
                EnumSet.of(eeif.execution.RemoteOrderType.BROKER, eeif.execution.RemoteOrderType.GTC_AUTOPULL);

        for (final RemoteOrderType orderType : RemoteOrderType.values()) {
            final boolean freshlyAdded = accountedForOrderTypes.add(orderType.remoteOrderType);
            Assert.assertTrue(freshlyAdded, "Order type [" + orderType + "] mapped duplicate times.");
        }

        for (final eeif.execution.RemoteOrderType orderType : eeif.execution.RemoteOrderType.values()) {
            Assert.assertTrue(accountedForOrderTypes.contains(orderType), "Order type [" + orderType + "] missing.");
        }
    }

    @Test
    public void marketTypesTest() {

        final Set<RemoteOrderType> allowedMarketTypes = EnumSet.of(RemoteOrderType.MARKET, RemoteOrderType.MKT_CLOSE);

        for (final RemoteOrderType orderType : RemoteOrderType.values()) {
            Assert.assertTrue(OrderType.MARKET != orderType.orderType || allowedMarketTypes.contains(orderType),
                    "unexpected market order.");
        }
    }
}

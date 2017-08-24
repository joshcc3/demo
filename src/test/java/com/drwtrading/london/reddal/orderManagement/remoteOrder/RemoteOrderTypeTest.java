package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.Set;

public class RemoteOrderTypeTest {

    @Test
    public void marketTypesTest() {

        final Set<RemoteOrderType> allowedMarketTypes = EnumSet.of(RemoteOrderType.MARKET, RemoteOrderType.MKT_CLOSE);

        for (final RemoteOrderType orderType : RemoteOrderType.values()) {
            Assert.assertTrue(OrderType.MARKET != orderType.orderType || allowedMarketTypes.contains(orderType),
                    "unexpected market order.");
        }
    }
}

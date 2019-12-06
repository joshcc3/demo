package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.Map;

public class GTCBettermentPrices {

    public final long requestTimeMilliSinceMidnight;
    public final Publisher<WebSocketOutboundData> responseChannel;
    public final EnumMap<BookSide, Map<String, Long>> existingOrders;

    public GTCBettermentPrices(final long requestTimeMilliSinceMidnight, final Publisher<WebSocketOutboundData> responseChannel,
            final EnumMap<BookSide, Map<String, Long>> bettermentPrices) {

        this.requestTimeMilliSinceMidnight = requestTimeMilliSinceMidnight;
        this.responseChannel = responseChannel;
        this.existingOrders = bettermentPrices;
    }
}

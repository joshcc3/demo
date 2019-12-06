package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.Map;

public class GTCBettermentPricesRequest {

    public final long requestTimeMilliSinceMidnight;
    public final Publisher<WebSocketOutboundData> responseChannel;
    public final Map<BookSide, Map<String, WorkingOrder>> existingOrders;

    public GTCBettermentPricesRequest(final long requestTimeMilliSinceMidnight, final Publisher<WebSocketOutboundData> responseChannel,
            final Map<BookSide, Map<String, WorkingOrder>> existingOrders) {

        this.requestTimeMilliSinceMidnight = requestTimeMilliSinceMidnight;
        this.responseChannel = responseChannel;
        this.existingOrders = existingOrders;
    }
}

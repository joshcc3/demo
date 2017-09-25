package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.LadderBoardRow;
import com.drwtrading.london.reddal.ladders.LadderBookView;
import com.drwtrading.london.reddal.ladders.LadderHTMLRow;
import com.drwtrading.london.reddal.ladders.LadderHTMLTable;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import eeif.execution.Side;
import eeif.execution.WorkingOrderUpdate;

import java.util.ArrayList;
import java.util.List;

class ShredderBookView {

    private static final int MAX_VISUAL_ORDER_SIZE = 20;
    private long scalingFactor = 100;
    private long scalingStep = 10;
    private long ordersPerRow = ShredderView.INITAL_ORDERS_PER_ROW;

    private final UiPipeImpl uiPipe;
    private final IShredderUI view;
    private final MDForSymbol marketData;
    private final String symbol;
    private final int levels;
    private final LongMap<LadderBoardRow> priceRows = new LongMap<>();
    private final LadderHTMLTable ladderHTMLKeys = new LadderHTMLTable();
    private final WorkingOrdersForSymbol workingOrdersForSymbol;

    private final List<ShreddedOrder> shreddedOrders = new ArrayList<>();

    private long centeredPrice = 0;
    private long topPrice = Long.MIN_VALUE;
    private boolean initialDisplay = false;

    ShredderBookView(final UiPipeImpl uiPipe, final IShredderUI view, final MDForSymbol marketData, final String symbol, final int levels,
            final WorkingOrdersForSymbol workingOrdersForSymbol) {

        this.uiPipe = uiPipe;
        this.view = view;
        this.marketData = marketData;
        this.symbol = symbol;
        this.levels = levels;
        this.workingOrdersForSymbol = workingOrdersForSymbol;

        ladderHTMLKeys.extendToLevels(levels);

        setCenteredPrice(getCenterPrice());
    }

    void refresh() {
        if (!initialDisplay && null != marketData.getBook() && marketData.getBook().isValid()) {
            setInitialScaling();
            setCenteredPrice(getCenterPrice());
            initialDisplay = true;

        }

        gatherShreddedOrders();
        drawPriceLevels();
        drawAggregateOrders();
        drawShreddedOrders();
    }

    private void drawPriceLevels() {
        for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
            final long price = priceNode.key;
            final LadderHTMLRow htmlRowKeys = priceNode.getValue().htmlKeys;
            final LadderBoardRow priceRow = priceRows.get(price);
            uiPipe.txt(htmlRowKeys.bookPriceKey, priceRow.formattedPrice);

            uiPipe.data(htmlRowKeys.bookBidKey, DataKey.PRICE, price);
            uiPipe.data(htmlRowKeys.bookAskKey, DataKey.PRICE, price);
            uiPipe.data(htmlRowKeys.bookOrderKey, DataKey.PRICE, price);
        }
    }

    private void drawShreddedOrders() {
        wipeDisplayedOrders();

        for (ShreddedOrder shreddedOrder : shreddedOrders) {
            final String orderCellKey = String.format("order_%s_%s", shreddedOrder.level, shreddedOrder.queuePosition);

            uiPipe.cls(orderCellKey, CSSClass.BLANK_ORDER, false);
            uiPipe.cls(orderCellKey, CSSClass.ORDER, true);

            final double widthInPercent = Math.min((double) shreddedOrder.quantity / scalingFactor * 100, MAX_VISUAL_ORDER_SIZE);
            uiPipe.width(orderCellKey, widthInPercent);

            if (widthInPercent > 0.75 * Math.floor(Math.log10(shreddedOrder.quantity) + 1)) {
                uiPipe.txt(orderCellKey, shreddedOrder.quantity);
            } else {
                uiPipe.txt(orderCellKey, "\u00a0");
            }

            uiPipe.cls(orderCellKey, shreddedOrder.getOppositeCSSCClass(), false);
            uiPipe.cls(orderCellKey, shreddedOrder.getCorrespondingCSSClass(), true);

            uiPipe.cls(orderCellKey, CSSClass.OUR_ORDER, shreddedOrder.isOurs);
        }
    }

    private boolean isOurOrder(IBookOrder order) {
        boolean isOurs = false;

        for (WorkingOrderUpdateFromServer workingOrderUpdateFromServer : workingOrdersForSymbol.ordersByPrice.get(order.getPrice())) {
            WorkingOrderUpdate ourOrder = workingOrderUpdateFromServer.workingOrderUpdate;

            boolean sameSide = sameSide(order.getSide(), ourOrder.getSide());
            boolean sameSize = ourOrder.getTotalQuantity() - ourOrder.getFilledQuantity() == order.getRemainingQty();

            isOurs = sameSide && sameSize;
        }

        return isOurs;
    }

    private boolean sameSide(BookSide bookSide, Side side) {
        return (bookSide == BookSide.BID && side == Side.BID) || (bookSide == BookSide.ASK && side == Side.OFFER);
    }

    private void wipeDisplayedOrders() {
        for (int level = 0; level < levels; level++) {
            for (int queuePosition = 0; queuePosition < ordersPerRow; queuePosition++) {
                final String orderCellKey = String.format("order_%s_%s", level, queuePosition);
                uiPipe.cls(orderCellKey, CSSClass.BLANK_ORDER, true);
                uiPipe.cls(orderCellKey, CSSClass.ORDER, false);
            }
        }
    }

    private void gatherShreddedOrders() {
        shreddedOrders.clear();

        final IBook<IBookLevelWithOrders> book = marketData.getLevel3Book();
        if (null != book && (book.getStatus() == BookMarketState.CONTINUOUS || book.getStatus() == BookMarketState.CLOSED)) {
            final int centerLevel = levels / 2;
            topPrice = book.getTickTable().addTicks(this.centeredPrice, centerLevel);

            long price = topPrice;
            for (int level = 0; level < levels; level++) {
                if (book.getBidLevel(price) != null) {
                    final IBookOrder order = book.getBidLevel(price).getFirstOrder();
                    gatherShreddedOrdersAtLevel(order, level);
                } else if (book.getAskLevel(price) != null) {
                    final IBookOrder order = book.getAskLevel(price).getFirstOrder();
                    gatherShreddedOrdersAtLevel(order, level);
                }
                price = book.getTickTable().addTicks(price, -1);
            }
        }
    }

    private void gatherShreddedOrdersAtLevel(IBookOrder order, int level) {
        int queuePosition = 0;

        while (null != order) {
            shreddedOrders.add(new ShreddedOrder(queuePosition, level, order.getRemainingQty(), isOurOrder(order), order.getSide()));
            queuePosition++;
            order = order.next();
        }

        if (queuePosition > ordersPerRow) {
            ordersPerRow = queuePosition + queuePosition / 2;
            view.draw(levels, ordersPerRow);
        }
    }

    private void drawAggregateOrders() {
        if (null != marketData && null != marketData.getBook()) {
            switch (marketData.getBook().getStatus()) {
                case CONTINUOUS: {
                    uiPipe.txt(HTML.SYMBOL, symbol);
                    uiPipe.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    uiPipe.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case AUCTION: {
                    uiPipe.txt(HTML.SYMBOL, symbol + " - AUC");
                    uiPipe.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    uiPipe.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case CLOSED: {
                    uiPipe.txt(HTML.SYMBOL, symbol + " - CLSD");
                    uiPipe.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    uiPipe.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                default: {
                    uiPipe.txt(HTML.SYMBOL, symbol + " - ?");
                    uiPipe.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    uiPipe.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);
                    break;
                }
            }
            uiPipe.title(symbol);

            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
                final long price = priceNode.key;
                final LadderBoardRow bookRow = priceNode.getValue();

                final IBookLevel bidLevel = marketData.getBook().getBidLevel(price);
                final IBookLevel askLevel = marketData.getBook().getAskLevel(price);

                if (null != bidLevel) {
                    long bidLevelQty = bidLevel.getQty();
                    bidQty(bookRow.htmlKeys, bidLevelQty);
                    uiPipe.cls(bookRow.htmlKeys.bookSideKey, CSSClass.IMPLIED_BID, 0 < bidLevel.getImpliedQty());
                } else if (null != askLevel) {
                    long askLevelQty = askLevel.getQty();
                    askQty(bookRow.htmlKeys, askLevelQty);
                    uiPipe.cls(bookRow.htmlKeys.bookSideKey, CSSClass.IMPLIED_ASK, 0 < askLevel.getImpliedQty());
                } else {
                    askQty(bookRow.htmlKeys, 0);
                }
            }

            uiPipe.cls(HTML.BOOK_TABLE, CSSClass.AUCTION, BookMarketState.AUCTION == marketData.getBook().getStatus());

            if (BookMarketState.AUCTION == marketData.getBook().getStatus()) {

                final IBookReferencePrice auctionIndicative = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                final LadderBoardRow bookRow = priceRows.get(auctionIndicative.getPrice());

                if (auctionIndicative.isValid() && null != bookRow) {

                    final long auctionQty = auctionIndicative.getQty();

                    bidQty(bookRow.htmlKeys, auctionQty);
                    askQty(bookRow.htmlKeys, auctionQty);

                    uiPipe.cls(bookRow.htmlKeys.bookBidKey, CSSClass.AUCTION, true);
                    uiPipe.cls(bookRow.htmlKeys.bookAskKey, CSSClass.AUCTION, true);
                }
            }
        }
    }

    public void setCenteredPrice(final long newCenterPrice) {
        if (null != marketData && null != marketData.getBook()) {

            this.centeredPrice = this.marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, newCenterPrice);

            final int centerLevel = levels / 2;
            topPrice = marketData.getBook().getTickTable().addTicks(this.centeredPrice, centerLevel);
            priceRows.clear();

            long price = topPrice;
            for (int i = 0; i < levels; ++i) {

                final String formattedPrice = marketData.formatPrice(price);
                final LadderHTMLRow htmlRowKeys = ladderHTMLKeys.getRow(i);
                final LadderBoardRow ladderBookRow = new LadderBoardRow(formattedPrice, htmlRowKeys);

                priceRows.put(price, ladderBookRow);

                price = marketData.getBook().getTickTable().addTicks(price, -1);
            }
        }
    }

    private void setInitialScaling() {
        long maxVolumeOfOrders = 0;
        long volumeAtBid = 0;
        long volumeAtAsk = 0;
        final IBook<?> book = marketData.getBook();
        if (null != book && book.isValid()) {
            IBookLevel bidLevel = book.getBestBid();
            IBookLevel askLevel = book.getBestAsk();
            for (int i = 0; i < 5; i++) {

                if (null != bidLevel) {
                    volumeAtBid = bidLevel.getQty();
                    bidLevel = bidLevel.next();
                }
                if (null != askLevel) {
                    volumeAtAsk = askLevel.getQty();
                    askLevel = askLevel.next();
                }

                maxVolumeOfOrders = Math.max(maxVolumeOfOrders, Math.max(volumeAtBid, volumeAtAsk));
            }

            scalingFactor = maxVolumeOfOrders;
            scalingStep = scalingFactor / 20;
        }
    }

    private long getCenterPrice() {

        final IBook<?> book = marketData.getBook();
        if (null != book && book.isValid()) {

            final IBookLevel bestBid = book.getBestBid();
            final IBookLevel bestAsk = book.getBestAsk();
            final boolean isBookNotAuction = book.getStatus() != BookMarketState.AUCTION;
            final IBookReferencePrice auctionIndicativePrice = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
            final IBookReferencePrice auctionSummaryPrice = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_SUMMARY);
            final IBookReferencePrice yestClose = marketData.getBook().getRefPriceData(ReferencePoint.YESTERDAY_CLOSE);

            final long center;
            if (isBookNotAuction && null != bestBid && null != bestAsk) {
                center = (bestBid.getPrice() + bestAsk.getPrice()) / 2;
            } else if (isBookNotAuction && null != bestBid) {
                center = bestBid.getPrice();
            } else if (isBookNotAuction && null != bestAsk) {
                center = bestAsk.getPrice();
            } else if (auctionIndicativePrice.isValid()) {
                center = auctionIndicativePrice.getPrice();
            } else if (auctionSummaryPrice.isValid()) {
                center = auctionSummaryPrice.getPrice();
            } else if (marketData.getTradeTracker().hasTrade()) {
                center = marketData.getTradeTracker().getLastPrice();
            } else if (yestClose.isValid()) {
                center = yestClose.getPrice();
            } else {
                center = 0;
            }
            return marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, center);
        } else {
            return 0;
        }
    }

    void scrollUp() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final long newCenterPrice = book.getTickTable().addTicks(centeredPrice, 1);
            setCenteredPrice(newCenterPrice);
        }
    }

    void scrollDown() {

        final IBook<?> book = marketData.getBook();
        if (null != marketData.getBook()) {
            final long newCenterPrice = book.getTickTable().subtractTicks(centeredPrice, 1);
            setCenteredPrice(newCenterPrice);
        }
    }

    private void bidQty(final LadderHTMLRow htmlRowKeys, final long qty) {
        uiPipe.txt(htmlRowKeys.bookSideKey, formatMktQty(qty));
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.BID_ACTIVE, 0 < qty);
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.ASK_ACTIVE, false);
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.AUCTION, false);
    }

    private void askQty(final LadderHTMLRow htmlRowKeys, final long qty) {
        uiPipe.txt(htmlRowKeys.bookSideKey, formatMktQty(qty));
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.ASK_ACTIVE, 0 < qty);
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.BID_ACTIVE, false);
        uiPipe.cls(htmlRowKeys.bookSideKey, CSSClass.AUCTION, false);
    }

    void zoomOut() {
        scalingFactor += scalingStep;
    }

    void zoomIn() {
        scalingFactor = scalingFactor - scalingStep >= scalingStep ? scalingFactor - scalingStep : scalingFactor;
    }

    private static String formatMktQty(final long qty) {
        if (qty <= 0) {
            return HTML.EMPTY;
        } else if (LadderBookView.REALLY_BIG_NUMBER_THRESHOLD <= qty) {
            final double d = qty / 1000000d;
            return LadderBookView.BIG_NUMBER_DF.format(d);
        } else {
            return Long.toString(qty);
        }
    }
}

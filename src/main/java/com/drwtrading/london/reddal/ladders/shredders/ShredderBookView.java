package com.drwtrading.london.reddal.ladders.shredders;

import com.drwtrading.london.eeif.nibbler.transport.data.types.Tag;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.reddal.data.DataUtils;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.LadderBoardRow;
import com.drwtrading.london.reddal.ladders.LadderBookView;
import com.drwtrading.london.reddal.ladders.model.BookHTMLRow;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByID;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

class ShredderBookView {

    private static final int MAX_VISUAL_ORDER_SIZE = 20;

    private final NumberFormat BIG_NUMBER_DF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE + 'M', 0, 2);
    private final NumberFormat NUMBER_DF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0, 4);

    private long scalingFactor = 100;
    private long scalingStep = 10;
    private long ordersPerRow = ShredderView.INITAL_ORDERS_PER_ROW;

    private final UiPipeImpl ui;
    private final IShredderUI view;
    private final MDForSymbol marketData;
    private final String symbol;
    private final int levels;
    private final LongMap<LadderBoardRow> priceRows = new LongMap<>();
    private final LadderHTMLTable ladderHTMLKeys = new LadderHTMLTable();
    private final WorkingOrdersByID workingOrders;

    private final List<ShreddedOrder> shreddedOrders = new ArrayList<>();
    EnumMap<CSSClass, Long> highlightSizes = new EnumMap<>(CSSClass.class);
    private int nextHighlightBump;

    private long centeredPrice = 0;
    private long topPrice = Long.MIN_VALUE;
    private long bottomPrice = Long.MAX_VALUE;
    private boolean initialDisplay = false;
    private boolean needToResize = false;
    private final SymbolStackData stackData;

    Integer shreddedRowWidth = 0;

    ShredderBookView(final UiPipeImpl ui, final IShredderUI view, final MDForSymbol marketData, final String symbol, final int levels,
            final WorkingOrdersByID workingOrders, final SymbolStackData stackData) {

        this.ui = ui;
        this.view = view;
        this.marketData = marketData;
        this.symbol = symbol;
        this.levels = levels;
        this.workingOrders = workingOrders;
        this.stackData = stackData;

        ladderHTMLKeys.extendToLevels(levels);
    }

    void refresh() {
        if (!initialDisplay && null != marketData.getBook() && marketData.getBook().isValid()) {
            setInitialScaling();
            setCenteredPrice(getCenterPrice());
            initialDisplay = true;

        }

        gatherShreddedOrders();
        if (needToResize) {
            view.draw(levels, ordersPerRow);
            needToResize = false;
        }
        drawPriceLevels();
        drawAggregateOrders();
        drawLaserLines();
        drawShreddedOrders();
    }

    public void center() {
        if (null != marketData.getBook() && marketData.getBook().isValid()) {

            final long bookCenter = getCenterPrice();
            setCenteredPrice(bookCenter);
        }
    }

    private void drawLaserLines() {

        if (null != marketData.getBook() && marketData.getBook().isValid()) {

            for (final LaserLine laserLine : stackData.getLaserLines()) {
                setLaserLine(laserLine);
            }
        }
    }

    private void setLaserLine(final LaserLine laserLine) {

        final String laserKey = LadderBookView.LASER_LINE_HTML_MAP.get(laserLine.getType());

        if (laserLine.isValid() && 0 < levels) {

            final long laserLinePrice = laserLine.getValue();

            if (topPrice < laserLinePrice) {
                final LadderBoardRow priceRow = priceRows.get(topPrice);
                ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, 0.5);
            } else if (laserLinePrice < bottomPrice) {
                final LadderBoardRow priceRow = priceRows.get(bottomPrice);
                ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, -0.5);
            } else {
                long price = bottomPrice;
                while (price <= topPrice) {
                    final long priceAbove = marketData.getBook().getTickTable().addTicks(price, 1);
                    if (price <= laserLinePrice && laserLinePrice <= priceAbove && priceRows.containsKey(price)) {
                        final long fractionalPrice = laserLinePrice - price;
                        final double tickFraction = 1.0 * fractionalPrice / (priceAbove - price);
                        final LadderBoardRow priceRow = priceRows.get(price);
                        ui.height(laserKey, priceRow.htmlKeys.bookPriceKey, tickFraction);
                        break;
                    }
                    price = priceAbove;
                }
            }

            ui.cls(laserKey, CSSClass.INVISIBLE, false);
        } else {
            ui.cls(laserKey, CSSClass.INVISIBLE, true);
        }
    }

    private void drawPriceLevels() {
        for (int i = 0; i < levels; i++) {
            ui.clickable('#' + HTML.PRICE + i);
        }

        for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
            final long price = priceNode.key;
            final BookHTMLRow htmlRowKeys = priceNode.getValue().htmlKeys;
            final LadderBoardRow priceRow = priceRows.get(price);
            ui.txt(htmlRowKeys.bookPriceKey, priceRow.formattedPrice);

            ui.data(htmlRowKeys.bookBidKey, DataKey.PRICE, price);
            ui.data(htmlRowKeys.bookAskKey, DataKey.PRICE, price);
            ui.data(htmlRowKeys.bookOrderKey, DataKey.PRICE, price);
        }
    }

    void highlightSize(final long size) {
        CSSClass firstEmpty = null;
        boolean found = false;
        for (int i = CSSClass.HIGHLIGHT_ORDER_0.ordinal(); i <= CSSClass.HIGHLIGHT_ORDER_5.ordinal(); i++) {
            final CSSClass byOrdinal = CSSClass.getByOrdinal(i);
            final Long prev = highlightSizes.get(byOrdinal);
            if (null == prev && null == firstEmpty) {
                firstEmpty = byOrdinal;
            } else if (null != prev && prev == size) {
                firstEmpty = null;
                found = true;
                highlightSizes.remove(byOrdinal);
                break;
            }
        }
        if (!found) {
            if (null != firstEmpty) {
                highlightSizes.put(firstEmpty, size);
            } else {
                highlightSizes.put(CSSClass.getByOrdinal(CSSClass.HIGHLIGHT_ORDER_0.ordinal() + (nextHighlightBump++ % 6)), size);
            }
        }
        drawShreddedOrders();
    }

    private void drawShreddedOrders() {
        wipeDisplayedOrders();

        for (final ShreddedOrder shreddedOrder : shreddedOrders) {

            final String orderCellKey = String.format("order_%s_%s", shreddedOrder.level, shreddedOrder.queuePosition);

            ui.cls(orderCellKey, CSSClass.BLANK_ORDER, false);
            ui.cls(orderCellKey, CSSClass.ORDER, true);
            ui.data(orderCellKey, DataKey.VOLUME_IN_FRONT, shreddedOrder.previousQuantity);
            ui.data(orderCellKey, DataKey.QUANTITY, shreddedOrder.quantity);

            final double widthInPercent = Math.min((double) shreddedOrder.quantity / scalingFactor * 100, MAX_VISUAL_ORDER_SIZE);
            ui.width(orderCellKey, widthInPercent);

            if ((widthInPercent * shreddedRowWidth) / 100 > 10 * Math.ceil(Math.log10(shreddedOrder.quantity))) {
                ui.txt(orderCellKey, shreddedOrder.quantity);
            } else {
                ui.txt(orderCellKey, "\u00a0");
            }

            ui.cls(orderCellKey, shreddedOrder.getOppositeCSSCClass(), false);
            ui.cls(orderCellKey, shreddedOrder.getCorrespondingCSSClass(), true);
            for (int i = CSSClass.HIGHLIGHT_ORDER_0.ordinal(); i <= CSSClass.HIGHLIGHT_ORDER_5.ordinal(); i++) {
                final Long size = highlightSizes.get(CSSClass.getByOrdinal(i));
                ui.cls(orderCellKey, CSSClass.getByOrdinal(i), null != size && size == shreddedOrder.quantity);
            }
            ui.cls(orderCellKey, CSSClass.MAYBE_OUR_OURDER, shreddedOrder.isOurs);
            ui.cls(orderCellKey, CSSClass.OUR_ORDER, shreddedOrder.isOurs && shreddedOrder.canOnlyBeOurs);
            final Tag tag = shreddedOrder.tag;
            if (null == tag) {
                ui.data(orderCellKey, DataKey.TAG, "");
            } else {
                ui.data(orderCellKey, DataKey.TAG, tag.name());
            }
            ui.data(orderCellKey, DataKey.ORDER_TYPE, shreddedOrder.orderType);
        }
    }

    void augmentIfOurOrder(final IBookOrder order, final ShreddedOrder shreddedOrder) {

        final SourcedWorkingOrder workingOrder = workingOrders.getWorkingOrder(order.getOrderID());

        shreddedOrder.isOurs = null != workingOrder;

        if (shreddedOrder.isOurs) {
            shreddedOrder.tag = workingOrder.order.getTag();
            shreddedOrder.orderType = workingOrder.order.getOrderType().name();
        }
    }

    private void wipeDisplayedOrders() {
        for (int level = 0; level < levels; level++) {
            for (int queuePosition = 0; queuePosition < ordersPerRow; queuePosition++) {
                final String orderCellKey = "order_" + level + '_' + queuePosition;
                ui.cls(orderCellKey, CSSClass.BLANK_ORDER, true);
                ui.cls(orderCellKey, CSSClass.ORDER, false);
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

    private void gatherShreddedOrdersAtLevel(IBookOrder order, final int level) {

        int queuePosition = 0;
        long previousQuantity = 0;

        while (null != order) {
            final ShreddedOrder shreddedOrder =
                    new ShreddedOrder(queuePosition, level, order.getRemainingQty(), order.getSide(), previousQuantity);
            augmentIfOurOrder(order, shreddedOrder);
            shreddedOrders.add(shreddedOrder);
            queuePosition++;
            previousQuantity += order.getRemainingQty();
            order = order.next();
        }

        if (queuePosition > ordersPerRow) {
            ordersPerRow = queuePosition + queuePosition / 2;
            needToResize = true;
        }
    }

    private void drawAggregateOrders() {
        if (null != marketData && null != marketData.getBook()) {
            switch (marketData.getBook().getStatus()) {
                case CONTINUOUS: {
                    ui.txt(HTML.SYMBOL, symbol);
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case AUCTION: {
                    ui.txt(HTML.SYMBOL, symbol + " - AUC");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                case CLOSED: {
                    ui.txt(HTML.SYMBOL, symbol + " - CLSD");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, true);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, false);
                    break;
                }
                default: {
                    ui.txt(HTML.SYMBOL, symbol + " - ?");
                    ui.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
                    ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);
                    break;
                }
            }
            ui.title(symbol);

            for (final LongMapNode<LadderBoardRow> priceNode : priceRows) {
                final long price = priceNode.key;
                final LadderBoardRow bookRow = priceNode.getValue();

                final IBookLevel bidLevel = marketData.getBook().getBidLevel(price);
                final IBookLevel askLevel = marketData.getBook().getAskLevel(price);

                if (null != bidLevel) {
                    final double bidLevelQty = DataUtils.normalizedQty(bidLevel.getQty());
                    bidQty(bookRow.htmlKeys, bidLevelQty);
                    ui.cls(bookRow.htmlKeys.bookSideKey, CSSClass.IMPLIED_BID, 0 < bidLevel.getImpliedQty());
                } else if (null != askLevel) {
                    final double askLevelQty = DataUtils.normalizedQty(askLevel.getQty());
                    askQty(bookRow.htmlKeys, askLevelQty);
                    ui.cls(bookRow.htmlKeys.bookSideKey, CSSClass.IMPLIED_ASK, 0 < askLevel.getImpliedQty());
                } else {
                    askQty(bookRow.htmlKeys, 0);
                }
            }

            ui.cls(HTML.BOOK_TABLE, CSSClass.AUCTION, BookMarketState.AUCTION == marketData.getBook().getStatus());

            if (BookMarketState.AUCTION == marketData.getBook().getStatus()) {

                final IBookReferencePrice auctionIndicative = marketData.getBook().getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                final LadderBoardRow bookRow = priceRows.get(auctionIndicative.getPrice());

                if (auctionIndicative.isValid() && null != bookRow) {

                    final long auctionQty = auctionIndicative.getQty();

                    bidQty(bookRow.htmlKeys, auctionQty);
                    askQty(bookRow.htmlKeys, auctionQty);

                    ui.cls(bookRow.htmlKeys.bookBidKey, CSSClass.AUCTION, true);
                    ui.cls(bookRow.htmlKeys.bookAskKey, CSSClass.AUCTION, true);
                }
            }
        }
    }

    private void setCenteredPrice(final long newCenterPrice) {
        if (null != marketData && null != marketData.getBook()) {

            this.centeredPrice = this.marketData.getBook().getTickTable().roundAwayToTick(BookSide.BID, newCenterPrice);

            final int centerLevel = levels / 2;
            topPrice = marketData.getBook().getTickTable().addTicks(this.centeredPrice, centerLevel);
            priceRows.clear();

            long price = topPrice;
            for (int i = 0; i < levels; ++i) {

                final String formattedPrice = marketData.formatPrice(price);
                final BookHTMLRow htmlRowKeys = ladderHTMLKeys.getRow(i);
                final LadderBoardRow ladderBookRow = new LadderBoardRow(formattedPrice, htmlRowKeys);

                priceRows.put(price, ladderBookRow);

                bottomPrice = price;
                price = marketData.getBook().getTickTable().addTicks(price, -1);
            }
        }
    }

    private void setInitialScaling() {
        double maxVolumeOfOrders = 0;
        double volumeAtBid = 0;
        double volumeAtAsk = 0;
        final IBook<?> book = marketData.getBook();
        if (null != book && book.isValid()) {
            IBookLevel bidLevel = book.getBestBid();
            IBookLevel askLevel = book.getBestAsk();
            for (int i = 0; i < 5; i++) {

                if (null != bidLevel) {
                    volumeAtBid = DataUtils.normalizedQty(bidLevel.getQty());
                    bidLevel = bidLevel.next();
                }
                if (null != askLevel) {
                    volumeAtAsk = DataUtils.normalizedQty(askLevel.getQty());
                    askLevel = askLevel.next();
                }

                maxVolumeOfOrders = Math.max(maxVolumeOfOrders, Math.max(volumeAtBid, volumeAtAsk));
            }

            scalingFactor = (int)(maxVolumeOfOrders);
            scalingStep = (int)scalingFactor / 20;
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

    private void bidQty(final BookHTMLRow htmlRowKeys, final double qty) {
        ui.txt(htmlRowKeys.bookSideKey, formatMktQty(qty));
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.BID_ACTIVE, 0 < qty);
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.ASK_ACTIVE, false);
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.AUCTION, false);
    }

    private void askQty(final BookHTMLRow htmlRowKeys, final double qty) {
        ui.txt(htmlRowKeys.bookSideKey, formatMktQty(qty));
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.ASK_ACTIVE, 0 < qty);
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.BID_ACTIVE, false);
        ui.cls(htmlRowKeys.bookSideKey, CSSClass.AUCTION, false);
    }

    void zoomOut() {
        scalingFactor += scalingStep;
    }

    void zoomIn() {
        scalingFactor = scalingFactor - scalingStep >= scalingStep ? scalingFactor - scalingStep : scalingFactor;
    }

    private String formatMktQty(final double qty) {
        if (LadderBookView.REALLY_BIG_NUMBER_THRESHOLD <= qty) {
            final double d = qty / 1_000_000d;
            return BIG_NUMBER_DF.format(d);
        } else {
            return NUMBER_DF.format(qty);
        }
    }

}

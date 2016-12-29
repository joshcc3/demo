package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.fastui.html.CSSClass;
import com.drwtrading.london.fastui.html.DataKey;
import com.drwtrading.london.fastui.html.HTML;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.SymbolStackPriceLevel;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LadderStackView implements ILadderBoard {

    private static final Map<String, String> DEFAULT_PREFS;
    private static final Map<String, String> STACK_PREFS;

    private static final String STACK_PREF_PREFIX = "STACK_";

    private static final Set<String> STACK_TYPES;
    private static final Set<String> STACK_ORDER_TYPES;

    static {
        DEFAULT_PREFS = new HashMap<>();
        DEFAULT_PREFS.put(HTML.WORKING_ORDER_TAG, "QUOTER");
        DEFAULT_PREFS.put(HTML.ORDER_TYPE_LEFT, "ONE_SHOT");
        DEFAULT_PREFS.put(HTML.ORDER_TYPE_RIGHT, "AUTO_MANAGE");
        DEFAULT_PREFS.put(HTML.INP_RELOAD, "50");

        STACK_PREFS = new HashMap<>();
        STACK_PREFS.put(HTML.WORKING_ORDER_TAG, STACK_PREF_PREFIX + HTML.WORKING_ORDER_TAG);
        STACK_PREFS.put(HTML.ORDER_TYPE_LEFT, STACK_PREF_PREFIX + HTML.ORDER_TYPE_LEFT);
        STACK_PREFS.put(HTML.ORDER_TYPE_RIGHT, STACK_PREF_PREFIX + HTML.ORDER_TYPE_RIGHT);
        STACK_PREFS.put(HTML.INP_RELOAD, STACK_PREF_PREFIX + HTML.INP_RELOAD);

        STACK_TYPES = new HashSet<>();
        for (final StackType stackType : StackType.values()) {
            STACK_TYPES.add(stackType.name());
        }

        STACK_ORDER_TYPES = new HashSet<>();
        for (final StackOrderType orderType : StackOrderType.values()) {
            STACK_ORDER_TYPES.add(orderType.name());
        }
    }

    private static final DecimalFormat PRICE_FORMAT = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0);

    private final String username;
    private final boolean isTrader;

    private final String symbol;
    private final SymbolStackData stackData;

    private final UiPipeImpl ui;
    private final ILadderUI view;

    private final Map<String, Integer> buttonQties;

    private final int levels;
    private final LadderHTMLTable ladderHTMLKeys;
    private final LongMap<LadderBoardRow> priceRows;

    private final LadderPrefsForSymbolUser ladderPrefsForSymbolUser;

    private final MDForSymbol marketData;

    private int tradingBoxQty;
    private double stackTickSizeBoxValue;

    private long centeredPrice;
    private long topPrice;

    public LadderStackView(final String username, final boolean isTrader, final String symbol, final Map<String, Integer> buttonQties,
            final int levels, final LadderHTMLTable ladderHTMLKeys, final SymbolStackData stackData, final UiPipeImpl ui,
            final ILadderUI view, final LadderPrefsForSymbolUser ladderPrefsForSymbolUser, final MDForSymbol marketData,
            final long centeredPrice) {

        this.username = username;
        this.isTrader = isTrader;

        this.symbol = symbol;
        this.stackData = stackData;

        this.ui = ui;
        this.view = view;

        this.buttonQties = buttonQties;

        this.levels = levels;
        this.ladderHTMLKeys = ladderHTMLKeys;
        this.priceRows = new LongMap<>();

        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;

        this.marketData = marketData;

        this.centeredPrice = centeredPrice;
        setCenteredPrice(centeredPrice);
    }

    @Override
    public void switchedTo() {

        view.trading(isTrader, STACK_TYPES, STACK_ORDER_TYPES, STACK_ORDER_TYPES);

        ui.cls(HTML.STACKS_CONTROL, CSSClass.INVISIBLE, false);

        ui.cls(HTML.AUTO_HEDGE_LEFT, CSSClass.INVISIBLE, true);
        ui.cls(HTML.AUTO_HEDGE_RIGHT, CSSClass.INVISIBLE, true);
        ui.cls(HTML.RANDOM_RELOAD, CSSClass.INVISIBLE, true);

        ui.cls(HTML.ORDER_TYPE_LEFT, CSSClass.FULL_WIDTH, true);
        ui.cls(HTML.ORDER_TYPE_RIGHT, CSSClass.FULL_WIDTH, true);

        for (final Map.Entry<String, Integer> entry : buttonQties.entrySet()) {
            final String display = LadderView.formatClickQty(entry.getValue());
            ui.txt(entry.getKey(), display);
        }

        ui.clickable('#' + HTML.STACK_TICK_SIZE);
        ui.clickable('#' + HTML.STACK_SUBMIT_TICK_SIZE);
        ui.clickable('#' + HTML.STACK_BID_QUOTE_ENABLED);
        ui.clickable('#' + HTML.STACK_BID_PICARD_ENABLED);
        ui.clickable('#' + HTML.STACK_ASK_QUOTE_ENABLED);
        ui.clickable('#' + HTML.STACK_ASK_PICARD_ENABLED);

        if (isTrader) {
            for (int i = 0; i < levels; i++) {
                ui.clickable('#' + HTML.VOLUME + i);
            }
        }

        this.stackTickSizeBoxValue = stackData.getBidPriceOffsetTickSize() / (double) Constants.NORMALISING_FACTOR;
    }

    @Override
    public void timedRefresh() {

    }

    @Override
    public void refresh(final String symbol) {

        ui.txt(HTML.SYMBOL, symbol);
        ui.cls(HTML.SYMBOL, CSSClass.AUCTION, false);
        ui.cls(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);

        ui.txt(HTML.INP_QTY, tradingBoxQty);
        ui.txt(HTML.STACK_TICK_SIZE, stackTickSizeBoxValue);

        for (final PricingMode mode : PricingMode.values()) {
            ui.cls(HTML.PRICING + mode, CSSClass.INVISIBLE, true);
            ui.cls(HTML.PRICING + mode, CSSClass.ACTIVE_MODE, false);
        }

        for (final String pref : STACK_PREFS.keySet()) {
            ui.txt(pref, getPref(pref));
        }

        final String stackTypePref = getPref(HTML.WORKING_ORDER_TAG);
        for (final CSSClass type : CSSClass.STACK_TYPES) {
            ui.cls(HTML.WORKING_ORDER_TAG, type, type.name().equals(stackTypePref));
        }

        final String leftOrderTypePref = getPref(HTML.ORDER_TYPE_LEFT);
        for (final CSSClass type : CSSClass.STACK_ORDER_TYPES) {
            ui.cls(HTML.ORDER_TYPE_LEFT, type, type.name().equals(leftOrderTypePref));
        }

        final String rightOrderTypePref = getPref(HTML.ORDER_TYPE_RIGHT);
        for (final CSSClass type : CSSClass.STACK_ORDER_TYPES) {
            ui.cls(HTML.ORDER_TYPE_RIGHT, type, type.name().equals(rightOrderTypePref));
        }

        final long totalBidQty = stackData.getTotalBidQty();
        ui.cls(HTML.BUY_QTY, CSSClass.INVISIBLE, totalBidQty == 0);
        ui.txt(HTML.BUY_QTY, totalBidQty);
        final long totalAskQty = stackData.getTotalAskQty();
        ui.cls(HTML.SELL_QTY, CSSClass.INVISIBLE, totalAskQty == 0);
        ui.txt(HTML.SELL_QTY, totalAskQty);

        ui.cls(HTML.STACK_BID_QUOTE_ENABLED, CSSClass.ENABLED, stackData.isBidStackEnabled(StackType.QUOTER));
        ui.cls(HTML.STACK_BID_PICARD_ENABLED, CSSClass.ENABLED, stackData.isBidStackEnabled(StackType.PICARD));
        ui.cls(HTML.STACK_ASK_QUOTE_ENABLED, CSSClass.ENABLED, stackData.isAskStackEnabled(StackType.QUOTER));
        ui.cls(HTML.STACK_ASK_PICARD_ENABLED, CSSClass.ENABLED, stackData.isAskStackEnabled(StackType.PICARD));

        for (int i = 0; i < levels; ++i) {

            final long price = topPrice - i;
            final LadderBoardRow boardRow = priceRows.get(price);
            final LadderHTMLRow htmlRow = boardRow.htmlKeys;

            final SymbolStackPriceLevel bidPriceLevel = stackData.getBidPriceLevel(price);
            setQty(htmlRow.orderKey, CSSClass.WORKING_BID, bidPriceLevel);
            ui.data(htmlRow.orderKey, DataKey.PRICE, boardRow.formattedPrice);
            ui.cls(htmlRow.orderKey, CSSClass.STACK_VIEW, true);

            ui.data(htmlRow.bidKey, DataKey.PRICE, boardRow.formattedPrice);
            ui.cls(htmlRow.bidKey, CSSClass.STACK_VIEW, true);

            ui.txt(htmlRow.priceKey, price);
            ui.cls(htmlRow.priceKey, CSSClass.STACK_VIEW, true);

            ui.data(htmlRow.askKey, DataKey.PRICE, boardRow.formattedPrice);
            ui.cls(htmlRow.askKey, CSSClass.STACK_VIEW, true);

            final SymbolStackPriceLevel askPriceLevel = stackData.getAskPriceLevel(price);
            setQty(htmlRow.volumeKey, CSSClass.WORKING_OFFER, askPriceLevel);
            ui.data(htmlRow.volumeKey, DataKey.PRICE, boardRow.formattedPrice);
            ui.cls(htmlRow.volumeKey, CSSClass.STACK_VIEW, true);

            if (0 == price) {
                ui.txt(htmlRow.bidKey, stackData.getFormattedBidPriceOffset());
                ui.txt(htmlRow.askKey, stackData.getFormattedAskPriceOffset());
            } else {
                ui.txt(htmlRow.bidKey, HTML.EMPTY);
                ui.txt(htmlRow.askKey, HTML.EMPTY);
            }
        }
    }

    @Override
    public boolean setPersistencePreference(final String id, final String value) {

        final String stackPrefID = STACK_PREFS.get(id);
        if (null != stackPrefID) {
            ladderPrefsForSymbolUser.set(stackPrefID, value);
            return true;
        } else {
            return false;
        }
    }

    private String getPref(final String id) {
        final String stackPrefID = STACK_PREFS.get(id);
        return ladderPrefsForSymbolUser.get(stackPrefID, DEFAULT_PREFS.get(id));
    }

    private void setQty(final String htmlKey, final CSSClass cssSideClass, final SymbolStackPriceLevel level) {

        if (null != level && 0 < level.getTotalQty()) {
            ui.txt(htmlKey, level.getFormattedTotalQty());
            ui.cls(htmlKey, cssSideClass, true);
            ui.cls(htmlKey, CSSClass.STACK_QTY, true);

            ui.cls(htmlKey, CSSClass.QUOTER, level.isQuoterPresent());
            ui.cls(htmlKey, CSSClass.PICARD, level.isPicardPresent());

            ui.cls(htmlKey, CSSClass.ONE_SHOT, level.isOneShotPresent());
            ui.cls(htmlKey, CSSClass.AUTO_MANAGE, level.isAutoMangePresent());
            ui.cls(htmlKey, CSSClass.REFRESHABLE, level.isRefreshablePresent());
        } else {
            ui.txt(htmlKey, HTML.EMPTY);
            ui.cls(htmlKey, cssSideClass, false);
            ui.cls(htmlKey, CSSClass.STACK_QTY, false);

            ui.cls(htmlKey, CSSClass.QUOTER, false);
            ui.cls(htmlKey, CSSClass.PICARD, false);

            ui.cls(htmlKey, CSSClass.ONE_SHOT, false);
            ui.cls(htmlKey, CSSClass.AUTO_MANAGE, false);
            ui.cls(htmlKey, CSSClass.REFRESHABLE, false);
        }
    }

    @Override
    public void setTradingBoxQty(final int qty) {
        tradingBoxQty = qty;
    }

    @Override
    public void setStackTickSize(final double tickSize) {
        stackTickSizeBoxValue = tickSize;
    }

    @Override
    public void setStackTickSizeToMatchQuote() {

        final IBook<?> book = marketData.getBook();
        if (null != book) {
            final IBookLevel bestBid = book.getBestBid();
            if (null != bestBid) {
                final long tickSize = book.getTickTable().getRawTickLevels().floorEntry(bestBid.getPrice()).getValue();
                stackTickSizeBoxValue = tickSize / (double) Constants.NORMALISING_FACTOR;
            }
        }
    }

    @Override
    public boolean canMoveTowardsCenter() {
        return false;
    }

    @Override
    public void setCenteredPrice(final long newCenterPrice) {

        this.centeredPrice = newCenterPrice;

        topPrice = newCenterPrice + (levels / 2);
        priceRows.clear();

        long price = topPrice;
        for (int i = 0; i < levels; ++i) {

            final String formattedPrice = PRICE_FORMAT.format(price);
            final LadderHTMLRow htmlRowKeys = ladderHTMLKeys.getRow(i);
            final LadderBoardRow ladderBookRow = new LadderBoardRow(formattedPrice, htmlRowKeys);

            priceRows.put(price, ladderBookRow);
            --price;
        }
    }

    @Override
    public long getCenteredPrice() {
        return centeredPrice;
    }

    @Override
    public void center() {

        setCenteredPrice(0);
    }

    @Override
    public boolean moveTowardsCenter() {
        // no auto recenter
        return true;
    }

    @Override
    public void setBestBidCenter() {

        if (stackData.hasBestBid()) {
            setCenteredPrice(stackData.getBestBid());
        }
    }

    @Override
    public void setBestAskCenter() {

        if (stackData.hasBestAsk()) {
            setCenteredPrice(stackData.getBestAsk());
        }
    }

    @Override
    public void scrollUp() {
        setCenteredPrice(centeredPrice + 1);
    }

    @Override
    public void scrollDown() {
        setCenteredPrice(centeredPrice - 1);
    }

    @Override
    public void pageUp() {
        setCenteredPrice(centeredPrice + levels);
    }

    @Override
    public void pageDown() {
        setCenteredPrice(centeredPrice - levels);
    }

    @Override
    public void onClick(final ClientSpeedState clientSpeedState, final String label, final String button, final Map<String, String> data) {

        if ("left".equals(button)) {
            final Integer qtyChange = buttonQties.get(label);
            if (null != qtyChange) {
                tradingBoxQty += qtyChange;
            } else if (HTML.BUTTON_CLR.equals(label)) {
                tradingBoxQty = 0;
            } else if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (null != ladderPrefsForSymbolUser) {
                    submitOrderLeftClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {

                final int price = Integer.valueOf(data.get("price"));
                final LadderHTMLRow htmlRowKeys = priceRows.get(price).htmlKeys;
                if (label.equals(htmlRowKeys.orderKey)) {
                    if (!stackData.clearBidStackPrice(price)) {
                        throw new IllegalStateException("Could not send msg - stack connection down.");
                    }
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + ' ' + htmlRowKeys.orderKey + ' ' + label);
                }
            } else if (label.startsWith(HTML.VOLUME)) {

                final int price = Integer.valueOf(data.get("price"));
                final LadderHTMLRow htmlRowKeys = priceRows.get(price).htmlKeys;
                if (label.equals(htmlRowKeys.volumeKey)) {
                    if (!stackData.clearAskStackPrice(price)) {
                        throw new IllegalStateException("Could not send msg - stack connection down.");
                    }
                } else {
                    System.out.println("Mismatched label: " + data.get("price") + ' ' + htmlRowKeys.orderKey + ' ' + label);
                }
            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                if (!stackData.improveBidStackPriceOffset(stackData.getBidPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                if (!stackData.improveBidStackPriceOffset(-stackData.getBidPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                if (!stackData.improveAskStackPriceOffset(stackData.getAskPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                if (!stackData.improveAskStackPriceOffset(-stackData.getAskPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.START_BUY)) {
                stackData.startBidStrategy();
            } else if (label.equals(HTML.START_SELL)) {
                stackData.startAskStrategy();
            } else if (label.equals(HTML.STOP_BUY)) {
                stackData.stopBidStrategy();
            } else if (label.equals(HTML.STOP_SELL)) {
                stackData.stopAskStrategy();
            } else if (label.equals(HTML.STACK_BID_QUOTE_ENABLED)) {
                stackData.setBidStackEnabled(StackType.QUOTER, true);
            } else if (label.equals(HTML.STACK_BID_PICARD_ENABLED)) {
                stackData.setBidStackEnabled(StackType.PICARD, true);
            } else if (label.equals(HTML.STACK_ASK_QUOTE_ENABLED)) {
                stackData.setAskStackEnabled(StackType.QUOTER, true);
            } else if (label.equals(HTML.STACK_ASK_PICARD_ENABLED)) {
                stackData.setAskStackEnabled(StackType.PICARD, true);
            } else if (label.equals(HTML.STACK_SUBMIT_TICK_SIZE)) {
                final long tickSize = (long) (stackTickSizeBoxValue * Constants.NORMALISING_FACTOR);
                stackData.setStackPriceOffsetTickSize(tickSize);
            }
        } else if ("right".equals(button)) {
            if (label.startsWith(HTML.BID) || label.startsWith(HTML.OFFER)) {
                if (ladderPrefsForSymbolUser != null) {
                    submitOrderRightClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.ORDER)) {
                //                rightClickModify(clientSpeedState, data);
            } else if (label.equals(HTML.STACK_BID_QUOTE_ENABLED)) {
                stackData.setBidStackEnabled(StackType.QUOTER, false);
            } else if (label.equals(HTML.STACK_BID_PICARD_ENABLED)) {
                stackData.setBidStackEnabled(StackType.PICARD, false);
            } else if (label.equals(HTML.STACK_ASK_QUOTE_ENABLED)) {
                stackData.setAskStackEnabled(StackType.QUOTER, false);
            } else if (label.equals(HTML.STACK_ASK_PICARD_ENABLED)) {
                stackData.setAskStackEnabled(StackType.PICARD, false);
            } else if (label.equals(HTML.STACK_SUBMIT_TICK_SIZE)) {
                ui.txt(HTML.STACK_TICK_SIZE, stackData.getBidPriceOffsetTickSize());
            }
        } else if ("middle".equals(button)) {
            //            if (label.startsWith(HTML.ORDER)) {
            //final String price = data.get("price");
            //final String url = String.format("/orders#%s,%s", symbol, price);
            //final Collection<WorkingOrderUpdateFromServer> orders = workingOrdersForSymbol.ordersByPrice.get(Long.valueOf(price));
            //if (!orders.isEmpty()) {
            //    view.popUp(url, "orders", 270, 20 * (1 + orders.size()));
            //}
            //            }
        }
    }

    private void submitOrderLeftClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data) {

        final String orderType = getPref(HTML.ORDER_TYPE_LEFT);
        submitOrderClick(clientSpeedState, label, data, orderType);
    }

    private void submitOrderRightClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data) {

        final String orderType = getPref(HTML.ORDER_TYPE_RIGHT);
        submitOrderClick(clientSpeedState, label, data, orderType);
    }

    private void submitOrderClick(final ClientSpeedState clientSpeedState, final String label, final Map<String, String> data,
            final String orderTypePref) {

        final StackOrderType orderType = StackOrderType.getOrderType(orderTypePref);

        final String stackTypePref = getPref(HTML.WORKING_ORDER_TAG);
        final StackType stackType = StackType.getStackType(stackTypePref);

        final int price = Integer.valueOf(data.get("price"));

        final LadderBoardRow bookRow = priceRows.get(price);

        if (ClientSpeedState.TOO_SLOW == clientSpeedState) {
            throw new IllegalArgumentException("Client too slow [" + clientSpeedState + "].");
        } else if (null == orderType) {
            throw new IllegalArgumentException("No order type [" + orderTypePref + "] provided.");
        } else if (null == stackType) {
            throw new IllegalArgumentException("No stack type [" + stackTypePref + "] provided.");
        } else if (label.equals(bookRow.htmlKeys.bidKey)) {

            if (!stackData.setBidStackQty(stackType, orderType, price, tradingBoxQty)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }

        } else if (label.equals(bookRow.htmlKeys.askKey)) {

            if (!stackData.setAskStackQty(stackType, orderType, price, tradingBoxQty)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }

        } else {
            throw new IllegalArgumentException("Price " + price + " did not match key " + label);
        }

        final int reloadBoxQty = Integer.valueOf(getPref(HTML.INP_RELOAD));
        tradingBoxQty = Math.max(0, reloadBoxQty);
    }

    @Override
    public void cancelAllForSide(final BookSide side) {
        if (BookSide.BID == side) {
            if (!stackData.clearBidStack()) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }
        } else if (BookSide.ASK == side) {
            if (!stackData.clearAskStack()) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }
        }
    }
}
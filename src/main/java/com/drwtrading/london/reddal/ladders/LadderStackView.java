package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.data.LadderMetaData;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.SymbolStackPriceLevel;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.model.HeaderPanel;
import com.drwtrading.london.reddal.ladders.model.LadderViewModel;
import com.drwtrading.london.reddal.ladders.model.LeftHandPanel;
import com.drwtrading.london.reddal.ladders.model.QtyButton;
import com.drwtrading.london.reddal.ladders.model.StackPanel;
import com.drwtrading.london.reddal.ladders.model.StackPanelRow;
import com.drwtrading.london.reddal.stacks.StackIncreaseChildOffsetCmd;
import com.drwtrading.london.reddal.stacks.StackIncreaseParentOffsetCmd;
import com.drwtrading.london.reddal.stacks.StacksSetSiblingsEnableCmd;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LadderStackView implements ILadderBoard {

    private static final String STACK_SOURCE = "StackLadder";

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
            if (StackOrderType.CHILD_ONE_SHOT != orderType) {
                STACK_ORDER_TYPES.add(orderType.name());
            }
        }
    }

    private static final int MODIFY_TIMEOUT_MILLI = 5000;

    private final String username;
    private final boolean isTrader;

    private final String symbol;
    private final SymbolStackData stackData;
    private final LadderMetaData metaData;
    private final Publisher<StackIncreaseParentOffsetCmd> stackParentCmdPublisher;
    private final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher;
    private final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher;

    private final ILadderUI view;

    private final Map<QtyButton, Integer> buttonQties;

    private final int levels;
    private final LadderViewModel ladderModel;

    private final LadderPrefsForSymbolUser ladderPrefsForSymbolUser;

    private final MDForSymbol marketData;

    private int tradingBoxQty;
    private double stackTickSizeBoxValue;
    private double stackAlignmentTickToBPSBoxValue;

    private long centeredPrice;
    private long topPrice;

    private BookSide modifySide;
    private StackType modifyStackType;
    private int modifyFromPrice;
    private long modifyFromPriceSelectedTime;

    LadderStackView(final String username, final boolean isTrader, final String symbol, final Map<QtyButton, Integer> buttonQties,
            final int levels, final LadderViewModel ladderModel, final SymbolStackData stackData, final LadderMetaData metaData,
            final Publisher<StackIncreaseParentOffsetCmd> stackParentCmdPublisher,
            final Publisher<StackIncreaseChildOffsetCmd> increaseChildOffsetCmdPublisher,
            final Publisher<StacksSetSiblingsEnableCmd> disableSiblingsCmdPublisher, final ILadderUI view,
            final LadderPrefsForSymbolUser ladderPrefsForSymbolUser, final MDForSymbol marketData) {

        this.username = username;
        this.isTrader = isTrader;

        this.symbol = symbol;
        this.stackData = stackData;
        this.metaData = metaData;
        this.stackParentCmdPublisher = stackParentCmdPublisher;
        this.increaseChildOffsetCmdPublisher = increaseChildOffsetCmdPublisher;
        this.disableSiblingsCmdPublisher = disableSiblingsCmdPublisher;

        this.view = view;

        this.buttonQties = buttonQties;

        this.levels = levels;
        this.ladderModel = ladderModel;

        this.ladderPrefsForSymbolUser = ladderPrefsForSymbolUser;

        this.marketData = marketData;

        center();
    }

    @Override
    public void switchedTo() {

        view.trading(isTrader, STACK_TYPES, STACK_ORDER_TYPES, STACK_ORDER_TYPES);

        ladderModel.setClass(HTML.LADDER_DIV, CSSClass.STACK_VIEW, true);

        ladderModel.setClass(HTML.STACK_CONFIG_BUTTON, CSSClass.INVISIBLE, false);
        ladderModel.setClass(HTML.STACKS_CONTROL, CSSClass.INVISIBLE, false);

        ladderModel.setClass(HTML.RANDOM_RELOAD, CSSClass.INVISIBLE, true);

        ladderModel.setClass(HTML.ORDER_TYPE_LEFT, CSSClass.FULL_WIDTH, true);
        ladderModel.setClass(HTML.ORDER_TYPE_RIGHT, CSSClass.FULL_WIDTH, true);

        final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();

        for (final Map.Entry<QtyButton, Integer> entry : buttonQties.entrySet()) {
            leftHandPanel.setQtyButton(entry.getKey(), entry.getValue());
        }

        ladderModel.setClickable('#' + HTML.STACK_CONFIG_BUTTON);
        ladderModel.setClickable('#' + HTML.STACK_TICK_SIZE);
        ladderModel.setClickable('#' + HTML.STACK_SUBMIT_TICK_SIZE);
        ladderModel.setClickable('#' + HTML.STACK_BID_QUOTE_ENABLED);
        ladderModel.setClickable('#' + HTML.STACK_BID_PICARD_ENABLED);
        ladderModel.setClickable('#' + HTML.STACK_ASK_QUOTE_ENABLED);
        ladderModel.setClickable('#' + HTML.STACK_ASK_PICARD_ENABLED);

        for (int i = 0; i < levels; i++) {
            ladderModel.setClickable('#' + HTML.STACK_DIVIDER + i);
        }

        this.stackTickSizeBoxValue = stackData.getPriceOffsetTickSize();
        this.stackAlignmentTickToBPSBoxValue = stackData.getStackAlignmentTickToBPS();

        leftHandPanel.setStackTickSize(stackTickSizeBoxValue);
        leftHandPanel.setStackTickToBPS(stackAlignmentTickToBPSBoxValue);
    }

    @Override
    public void timedRefresh() {

        if (null != modifySide && modifyFromPriceSelectedTime < System.currentTimeMillis() - MODIFY_TIMEOUT_MILLI) {
            modifySide = null;
        }
    }

    @Override
    public void refresh(final String symbol) {

        final HeaderPanel headerPanel = ladderModel.getHeaderPanel();
        final LeftHandPanel leftHandPanel = ladderModel.getLeftHandPanel();

        headerPanel.setSymbol(symbol);
        ladderModel.setClass(HTML.SYMBOL, CSSClass.AUCTION, false);
        ladderModel.setClass(HTML.SYMBOL, CSSClass.NO_BOOK_STATE, true);

        leftHandPanel.setClickTradingQty(tradingBoxQty, "---");

        for (final PricingMode mode : PricingMode.values()) {
            ladderModel.setClass(HTML.PRICING + mode, CSSClass.INVISIBLE, true);
            ladderModel.setClass(HTML.PRICING + mode, CSSClass.ACTIVE_MODE, false);
        }

        for (final String pref : STACK_PREFS.keySet()) {
            leftHandPanel.setClickTradingPreference(pref, getPref(pref));
        }

        final String stackTypePref = getPref(HTML.WORKING_ORDER_TAG);
        for (final CSSClass type : CSSClass.STACK_TYPES) {
            ladderModel.setClass(HTML.WORKING_ORDER_TAG, type, type.name().equals(stackTypePref));
        }

        final String leftOrderTypePref = getPref(HTML.ORDER_TYPE_LEFT);
        for (final CSSClass type : CSSClass.STACK_ORDER_TYPES) {
            ladderModel.setClass(HTML.ORDER_TYPE_LEFT, type, type.name().equals(leftOrderTypePref));
        }

        final String rightOrderTypePref = getPref(HTML.ORDER_TYPE_RIGHT);
        for (final CSSClass type : CSSClass.STACK_ORDER_TYPES) {
            ladderModel.setClass(HTML.ORDER_TYPE_RIGHT, type, type.name().equals(rightOrderTypePref));
        }

        final long totalBidQty = stackData.getTotalBidQty();
        headerPanel.setBidQty(totalBidQty);

        final long totalAskQty = stackData.getTotalAskQty();
        headerPanel.setAskQty(totalAskQty);

        ladderModel.setClass(HTML.STACK_BID_QUOTE_ENABLED, CSSClass.ENABLED, stackData.isBidStackEnabled(StackType.QUOTER));
        ladderModel.setClass(HTML.STACK_BID_PICARD_ENABLED, CSSClass.ENABLED, stackData.isBidStackEnabled(StackType.PICARD));
        ladderModel.setClass(HTML.STACK_ASK_QUOTE_ENABLED, CSSClass.ENABLED, stackData.isAskStackEnabled(StackType.QUOTER));
        ladderModel.setClass(HTML.STACK_ASK_PICARD_ENABLED, CSSClass.ENABLED, stackData.isAskStackEnabled(StackType.PICARD));

        final StackPanel stackPanel = ladderModel.getStackPanel();

        final double stackAlignmentTickToBPS = stackData.getStackAlignmentTickToBPS();

        for (int i = 0; i < levels; ++i) {

            final long price = topPrice - i;
            final StackPanelRow stackRow = stackPanel.getRow(i);

            final SymbolStackPriceLevel bidPriceLevel = stackData.getBidPriceLevel(price);
            final SymbolStackPriceLevel askPriceLevel = stackData.getAskPriceLevel(price);

            setQty(stackPanel, stackRow, bidPriceLevel, askPriceLevel);

            ladderModel.setClass(stackRow.htmlData.stackBidQuoteKey, CSSClass.MODIFY_PRICE_SELECTED,
                    BookSide.BID == modifySide && StackType.QUOTER == modifyStackType && modifyFromPrice == price);
            ladderModel.setClass(stackRow.htmlData.stackBidPicardKey, CSSClass.MODIFY_PRICE_SELECTED,
                    BookSide.BID == modifySide && StackType.PICARD == modifyStackType && modifyFromPrice == price);

            ladderModel.setData(stackRow.htmlData.stackBidPicardKey, DataKey.PRICE, stackRow.getPrice());
            ladderModel.setData(stackRow.htmlData.stackBidQuoteKey, DataKey.PRICE, stackRow.getPrice());
            ladderModel.setData(stackRow.htmlData.stackBidOffsetKey, DataKey.PRICE, stackRow.getPrice());

            ladderModel.setClass(stackRow.htmlData.stackAskQuoteKey, CSSClass.MODIFY_PRICE_SELECTED,
                    BookSide.ASK == modifySide && StackType.QUOTER == modifyStackType && modifyFromPrice == price);
            ladderModel.setClass(stackRow.htmlData.stackAskPicardKey, CSSClass.MODIFY_PRICE_SELECTED,
                    BookSide.ASK == modifySide && StackType.PICARD == modifyStackType && modifyFromPrice == price);

            ladderModel.setData(stackRow.htmlData.stackAskPicardKey, DataKey.PRICE, stackRow.getPrice());
            ladderModel.setData(stackRow.htmlData.stackAskQuoteKey, DataKey.PRICE, stackRow.getPrice());
            ladderModel.setData(stackRow.htmlData.stackAskOffsetKey, DataKey.PRICE, stackRow.getPrice());

            if (0 == price) {
                stackPanel.setBidOffset(stackRow, stackData.getFormattedBidPriceOffsetBPS());
                ladderModel.setClass(stackRow.htmlData.stackBidOffsetKey, CSSClass.STACK_OFFSET, true);

                stackPanel.setAskOffset(stackRow, stackData.getFormattedAskPriceOffset());
                ladderModel.setClass(stackRow.htmlData.stackAskOffsetKey, CSSClass.STACK_OFFSET, true);
            } else if (Constants.EPSILON < stackAlignmentTickToBPS) {

                final double bpsOffset = price * stackAlignmentTickToBPS;
                stackPanel.setBidOffset(stackRow, stackData.getFormattedBidPriceOffsetBPS(bpsOffset));
                ladderModel.setClass(stackRow.htmlData.stackBidOffsetKey, CSSClass.STACK_OFFSET, false);

                stackPanel.setAskOffset(stackRow, stackData.getFormattedAskPriceOffsetBPS(bpsOffset));
                ladderModel.setClass(stackRow.htmlData.stackAskOffsetKey, CSSClass.STACK_OFFSET, false);
            } else {

                stackPanel.setBidOffset(stackRow, HTML.EMPTY);
                ladderModel.setClass(stackRow.htmlData.stackBidOffsetKey, CSSClass.STACK_OFFSET, false);

                stackPanel.setBidOffset(stackRow, HTML.EMPTY);
                ladderModel.setClass(stackRow.htmlData.stackAskOffsetKey, CSSClass.STACK_OFFSET, false);
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

    private void setQty(final StackPanel stackPanel, final StackPanelRow row, final SymbolStackPriceLevel bidLevel,
            final SymbolStackPriceLevel askLevel) {

        if (null != bidLevel) {

            final long bidPicardQty = getStackQty(bidLevel, StackType.PICARD);
            final long bidQuoteQty = getStackQty(bidLevel, StackType.QUOTER);

            stackPanel.setBidPicardQty(row, bidPicardQty);
            stackPanel.setBidQuoteQty(row, bidQuoteQty);

            setLevelClasses(row.htmlData.stackBidPicardKey, bidPicardQty, row.htmlData.stackBidQuoteKey, bidQuoteQty, bidLevel);
        } else {

            stackPanel.setBidPicardQty(row, 0);
            stackPanel.setBidQuoteQty(row, 0);
            setLevelClasses(row.htmlData.stackBidPicardKey, 0, row.htmlData.stackBidQuoteKey, 0, null);

        }

        if (null != askLevel) {

            final long askPicardQty = getStackQty(askLevel, StackType.PICARD);
            final long askQuoteQty = getStackQty(askLevel, StackType.QUOTER);

            stackPanel.setAskPicardQty(row, askPicardQty);
            stackPanel.setAskQuoteQty(row, askQuoteQty);

            setLevelClasses(row.htmlData.stackAskPicardKey, askPicardQty, row.htmlData.stackAskQuoteKey, askQuoteQty, askLevel);
        } else {

            stackPanel.setAskPicardQty(row, 0);
            stackPanel.setAskQuoteQty(row, 0);
            setLevelClasses(row.htmlData.stackAskPicardKey, 0, row.htmlData.stackAskQuoteKey, 0, null);
        }
    }

    private static long getStackQty(final SymbolStackPriceLevel stackLevel, final StackType stackType) {

        if (null != stackLevel && stackLevel.isStackPresent(stackType)) {
            final StackLevel picardBidLevel = stackLevel.getStackType(stackType);
            return picardBidLevel.getRemainingQty();
        } else {
            return 0;
        }
    }

    private void setLevelClasses(final String picardKey, final long picardQty, final String quoteKey, final long quoteQty,
            final SymbolStackPriceLevel level) {

        if (null != level) {

            ladderModel.setClass(picardKey, CSSClass.STACK_QTY, 0 < picardQty);
            ladderModel.setClass(picardKey, CSSClass.ONE_SHOT, level.isOrderTypePresent(StackType.PICARD, StackOrderType.ONE_SHOT));
            ladderModel.setClass(picardKey, CSSClass.AUTO_MANAGE, level.isOrderTypePresent(StackType.PICARD, StackOrderType.AUTO_MANAGE));
            ladderModel.setClass(picardKey, CSSClass.REFRESHABLE, level.isOrderTypePresent(StackType.PICARD, StackOrderType.REFRESHABLE));

            ladderModel.setClass(quoteKey, CSSClass.STACK_QTY, 0 < quoteQty);
            ladderModel.setClass(quoteKey, CSSClass.ONE_SHOT, level.isOrderTypePresent(StackType.QUOTER, StackOrderType.ONE_SHOT));
            ladderModel.setClass(quoteKey, CSSClass.AUTO_MANAGE, level.isOrderTypePresent(StackType.QUOTER, StackOrderType.AUTO_MANAGE));
            ladderModel.setClass(quoteKey, CSSClass.REFRESHABLE, level.isOrderTypePresent(StackType.QUOTER, StackOrderType.REFRESHABLE));
        } else {

            ladderModel.setClass(picardKey, CSSClass.STACK_QTY, false);
            ladderModel.setClass(picardKey, CSSClass.ONE_SHOT, false);
            ladderModel.setClass(picardKey, CSSClass.AUTO_MANAGE, false);
            ladderModel.setClass(picardKey, CSSClass.REFRESHABLE, false);

            ladderModel.setClass(quoteKey, CSSClass.STACK_QTY, false);
            ladderModel.setClass(quoteKey, CSSClass.ONE_SHOT, false);
            ladderModel.setClass(quoteKey, CSSClass.AUTO_MANAGE, false);
            ladderModel.setClass(quoteKey, CSSClass.REFRESHABLE, false);
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
    public void setStackAlignmentTickToBPS(final double stackAlignmentTickToBPS) {
        stackAlignmentTickToBPSBoxValue = Math.max(0, stackAlignmentTickToBPS);
    }

    @Override
    public void setStackTickSizeToMatchQuote() {

        final IBook<?> book = marketData.getBook();
        if (null != book) {
            final IBookLevel bestBid = book.getBestBid();
            if (null != bestBid) {
                final long tickSize = book.getTickTable().getRawTickLevels().floorEntry(bestBid.getPrice()).getValue();
                stackTickSizeBoxValue = Math.floor(1000000 * tickSize / (double) bestBid.getPrice()) / 100;
                ladderModel.getLeftHandPanel().setStackTickSize(stackTickSizeBoxValue);
            }
        }
    }

    @Override
    public boolean canMoveTowardsCenter() {
        return false;
    }

    @Override
    public void setCenteredPrice(final long newCenterPrice) {

        centeredPrice = newCenterPrice;
        topPrice = centeredPrice + (levels / 2);
        final StackPanel stackPanel = ladderModel.getStackPanel();
        stackPanel.clearPriceMapping();

        long price = topPrice;

        for (int i = 0; i < levels; ++i) {

            stackPanel.setRowPrice(i, price);
            --price;
        }
    }

    @Override
    public long getCenteredPrice() {

        if (stackData.hasBestBid() && stackData.hasBestAsk()) {

            return (stackData.getBestBid() + stackData.getBestAsk()) / 2;
        } else {
            return 0;
        }
    }

    @Override
    public void center() {

        setCenteredPrice(getCenteredPrice());
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
            final QtyButton qtyButton = QtyButton.getButtonFromHTML(label);
            final Integer qtyChange = buttonQties.get(qtyButton);
            if (null != qtyChange) {
                tradingBoxQty += qtyChange;
            } else if (HTML.BUTTON_CLR.equals(label)) {
                tradingBoxQty = 0;
            } else if (label.startsWith(HTML.STACK_BID_OFFSET) || label.startsWith(HTML.STACK_ASK_OFFSET)) {
                if (null != ladderPrefsForSymbolUser) {
                    submitOrderLeftClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.STACK_BID_PICARD)) {

                final int price = Integer.valueOf(data.get("price"));
                final String expectedLabel = ladderModel.getStackPanel().getRowByPrice(price).htmlData.stackBidPicardKey;
                cancelBidStackOrders(price, label, expectedLabel, StackType.PICARD);

            } else if (label.startsWith(HTML.STACK_BID_QUOTE)) {

                final int price = Integer.valueOf(data.get("price"));
                final String expectedLabel = ladderModel.getStackPanel().getRowByPrice(price).htmlData.stackBidQuoteKey;
                cancelBidStackOrders(price, label, expectedLabel, StackType.QUOTER);

            } else if (label.startsWith(HTML.STACK_ASK_PICARD)) {

                final int price = Integer.valueOf(data.get("price"));
                final String expectedLabel = ladderModel.getStackPanel().getRowByPrice(price).htmlData.stackAskPicardKey;
                cancelAskStackOrders(price, label, expectedLabel, StackType.PICARD);

            } else if (label.startsWith(HTML.STACK_ASK_QUOTE)) {

                final int price = Integer.valueOf(data.get("price"));
                final String expectedLabel = ladderModel.getStackPanel().getRowByPrice(price).htmlData.stackAskQuoteKey;
                cancelAskStackOrders(price, label, expectedLabel, StackType.QUOTER);

            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, 1));
                } else if (!stackData.improveBidStackPriceOffset(stackData.getPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, -1));
                } else if (!stackData.improveBidStackPriceOffset(-stackData.getPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, 1));
                } else if (!stackData.improveAskStackPriceOffset(stackData.getPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    stackParentCmdPublisher.publish(
                            new StackIncreaseParentOffsetCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, -1));
                } else if (!stackData.improveAskStackPriceOffset(-stackData.getPriceOffsetTickSize())) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.START_BUY)) {
                stackData.startBidStrategy();
            } else if (label.equals(HTML.START_SELL)) {
                stackData.startAskStrategy();
            } else if (label.equals(HTML.STOP_BUY)) {
                stackData.stopBidStrategy();
                final StacksSetSiblingsEnableCmd cmd =
                        new StacksSetSiblingsEnableCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, false);
                disableSiblingsCmdPublisher.publish(cmd);
            } else if (label.equals(HTML.STOP_SELL)) {
                stackData.stopAskStrategy();
                final StacksSetSiblingsEnableCmd cmd =
                        new StacksSetSiblingsEnableCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, false);
                disableSiblingsCmdPublisher.publish(cmd);
            } else if (label.equals(HTML.STACK_BID_QUOTE_ENABLED)) {
                stackData.setBidStackEnabled(StackType.QUOTER, true);
            } else if (label.equals(HTML.STACK_BID_PICARD_ENABLED)) {
                stackData.setBidStackEnabled(StackType.PICARD, true);
            } else if (label.equals(HTML.STACK_ASK_QUOTE_ENABLED)) {
                stackData.setAskStackEnabled(StackType.QUOTER, true);
            } else if (label.equals(HTML.STACK_ASK_PICARD_ENABLED)) {
                stackData.setAskStackEnabled(StackType.PICARD, true);
            } else if (label.equals(HTML.STACK_SUBMIT_TICK_SIZE)) {
                stackData.setStackGroupUpdate(stackTickSizeBoxValue, stackAlignmentTickToBPSBoxValue);
                setCenteredPrice(centeredPrice);
            } else if (label.equals(HTML.STACK_CONFIG_BUTTON)) {
                final String url = "/stackConfig#;" + symbol;
                view.popUp(url, "stackConfig:" + symbol, 2400, 300);
            }
        } else if ("right".equals(button)) {
            if (HTML.BUTTON_CLR.equals(label)) {
                setPersistencePreference(HTML.INP_RELOAD, Integer.toString(tradingBoxQty));
            } else if (label.startsWith(HTML.STACK_BID_OFFSET) || label.startsWith(HTML.STACK_ASK_OFFSET)) {
                if (ladderPrefsForSymbolUser != null) {
                    submitOrderRightClick(clientSpeedState, label, data);
                }
            } else if (label.startsWith(HTML.STACK_BID_QUOTE)) {
                rightClickBidModify(clientSpeedState, data, StackType.QUOTER);
            } else if (label.startsWith(HTML.STACK_BID_PICARD)) {
                rightClickBidModify(clientSpeedState, data, StackType.PICARD);
            } else if (label.startsWith(HTML.STACK_ASK_QUOTE)) {
                rightClickAskModify(clientSpeedState, data, StackType.QUOTER);
            } else if (label.startsWith(HTML.STACK_ASK_PICARD)) {
                rightClickAskModify(clientSpeedState, data, StackType.PICARD);
            } else if (label.equals(HTML.BUY_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(STACK_SOURCE, symbol, BookSide.BID, stackData.getPriceOffsetTickSize()));
                } else if (!stackData.adjustBidStackLevels(-1)) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.BUY_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(STACK_SOURCE, symbol, BookSide.BID, -stackData.getPriceOffsetTickSize()));
                } else if (!stackData.adjustBidStackLevels(1)) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_UP)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(STACK_SOURCE, symbol, BookSide.ASK, stackData.getPriceOffsetTickSize()));
                } else if (!stackData.adjustAskStackLevels(1)) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.SELL_OFFSET_DOWN)) {
                if (null != metaData.spreadContractSet && null != metaData.spreadContractSet.parentSymbol) {
                    increaseChildOffsetCmdPublisher.publish(
                            new StackIncreaseChildOffsetCmd(STACK_SOURCE, symbol, BookSide.ASK, -stackData.getPriceOffsetTickSize()));
                } else if (!stackData.adjustAskStackLevels(-1)) {
                    throw new IllegalStateException("Could not send msg - stack connection down.");
                }
            } else if (label.equals(HTML.START_BUY)) {
                stackData.startBidStrategy();
                final StacksSetSiblingsEnableCmd cmd =
                        new StacksSetSiblingsEnableCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.BID, true);
                disableSiblingsCmdPublisher.publish(cmd);
            } else if (label.equals(HTML.START_SELL)) {
                stackData.startAskStrategy();
                final StacksSetSiblingsEnableCmd cmd =
                        new StacksSetSiblingsEnableCmd(STACK_SOURCE, metaData.spreadContractSet.parentSymbol, BookSide.ASK, true);
                disableSiblingsCmdPublisher.publish(cmd);
            } else if (label.equals(HTML.STOP_BUY)) {
                stackData.stopBidStrategy();
            } else if (label.equals(HTML.STOP_SELL)) {
                stackData.stopAskStrategy();
            } else if (label.equals(HTML.STACK_BID_QUOTE_ENABLED)) {
                stackData.setBidStackEnabled(StackType.QUOTER, false);
            } else if (label.equals(HTML.STACK_BID_PICARD_ENABLED)) {
                stackData.setBidStackEnabled(StackType.PICARD, false);
            } else if (label.equals(HTML.STACK_ASK_QUOTE_ENABLED)) {
                stackData.setAskStackEnabled(StackType.QUOTER, false);
            } else if (label.equals(HTML.STACK_ASK_PICARD_ENABLED)) {
                stackData.setAskStackEnabled(StackType.PICARD, false);
            } else if (label.equals(HTML.STACK_TICK_SIZE)) {
                stackTickSizeBoxValue = stackData.getPriceOffsetTickSize();
                ladderModel.getLeftHandPanel().setStackTickSize(stackTickSizeBoxValue);
            } else if (label.equals(HTML.STACK_ALIGNMENT_TICK_TO_BPS)) {
                stackAlignmentTickToBPSBoxValue = stackData.getStackAlignmentTickToBPS();
                ladderModel.getLeftHandPanel().setStackTickToBPS(stackAlignmentTickToBPSBoxValue);
            } else if (label.equals(HTML.BUY_QTY)) {
                stackData.clearBidStack(StackType.QUOTER, StackOrderType.ONE_SHOT);
            } else if (label.equals(HTML.SELL_QTY)) {
                stackData.clearAskStack(StackType.QUOTER, StackOrderType.ONE_SHOT);
            }
        } else if ("middle".equals(button)) {
            //if (label.startsWith(HTML.ORDER)) {
            //final String price = data.get("price");
            //final String url = String.format("/orders#%s,%s", symbol, price);
            //final Collection<WorkingOrderUpdateFromServer> orders = workingOrdersForSymbol.ordersByPrice.get(Long.valueOf(price));
            //if (!orders.isEmpty()) {
            //    view.popUp(url, "orders", 270, 20 * (1 + orders.size()));
            //}
            //}
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

        final StackPanelRow stackRow = ladderModel.getStackPanel().getRowByPrice(price);

        if (ClientSpeedState.TOO_SLOW == clientSpeedState) {
            throw new IllegalArgumentException("Client too slow [" + clientSpeedState + "].");
        } else if (null == orderType) {
            throw new IllegalArgumentException("No order type [" + orderTypePref + "] provided.");
        } else if (null == stackType) {
            throw new IllegalArgumentException("No stack type [" + stackTypePref + "] provided.");
        } else if (label.equals(stackRow.htmlData.stackBidOffsetKey)) {

            if (!stackData.addBidStackQty(stackType, orderType, price, tradingBoxQty)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }

        } else if (label.equals(stackRow.htmlData.stackAskOffsetKey)) {

            if (!stackData.addAskStackQty(stackType, orderType, price, tradingBoxQty)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }

        } else {
            throw new IllegalArgumentException("Price " + price + " did not match key " + label);
        }

        final int reloadBoxQty = Integer.valueOf(getPref(HTML.INP_RELOAD));
        tradingBoxQty = Math.max(0, reloadBoxQty);
    }

    private void rightClickBidModify(final ClientSpeedState clientSpeedState, final Map<String, String> data, final StackType stackType) {

        final int price = Integer.valueOf(data.get("price"));

        if (BookSide.BID == modifySide && stackType == modifyStackType) {

            if (ClientSpeedState.FINE != clientSpeedState) {
                throw new IllegalStateException("Cannot modify stack, client too slow.");
            } else if (modifyFromPrice != price) {
                stackData.moveBidOrders(stackType, modifyFromPrice, price);
            }
            modifySide = null;

        } else {
            final SymbolStackPriceLevel level = stackData.getBidPriceLevel(price);
            if (null != level && null != level.getStackType(stackType)) {

                modifySide = BookSide.BID;
                modifyStackType = stackType;
                modifyFromPrice = price;
                modifyFromPriceSelectedTime = System.currentTimeMillis();

            } else {
                modifySide = null;
            }
        }
    }

    private void rightClickAskModify(final ClientSpeedState clientSpeedState, final Map<String, String> data, final StackType stackType) {

        final int price = Integer.valueOf(data.get("price"));

        if (BookSide.ASK == modifySide && stackType == modifyStackType) {

            if (ClientSpeedState.FINE != clientSpeedState) {
                throw new IllegalStateException("Cannot modify stack, client too slow.");
            } else if (modifyFromPrice != price) {
                stackData.moveAskOrders(stackType, modifyFromPrice, price);
            }
            modifySide = null;

        } else {
            final SymbolStackPriceLevel level = stackData.getAskPriceLevel(price);
            if (null != level && null != level.getStackType(stackType)) {

                modifySide = BookSide.ASK;
                modifyStackType = stackType;
                modifyFromPrice = price;
                modifyFromPriceSelectedTime = System.currentTimeMillis();

            } else {
                modifySide = null;
            }
        }
    }

    private void cancelBidStackOrders(final int price, final String label, final String expectedLabel, final StackType stackType) {

        if (label.equals(expectedLabel)) {
            if (!stackData.clearBidStackPrice(stackType, price)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }
        } else {
            System.out.println("Mismatched label: " + price + ' ' + expectedLabel + ' ' + label);
        }
    }

    private void cancelAskStackOrders(final int price, final String label, final String expectedLabel, final StackType stackType) {

        if (label.equals(expectedLabel)) {
            if (!stackData.clearAskStackPrice(stackType, price)) {
                throw new IllegalStateException("Could not send msg - stack connection down.");
            }
        } else {
            System.out.println("Mismatched label: " + price + ' ' + expectedLabel + ' ' + label);
        }
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

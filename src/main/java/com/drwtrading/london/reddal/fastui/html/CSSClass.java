package com.drwtrading.london.reddal.fastui.html;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum CSSClass {

    INVISIBLE("invisible"),
    FULL_WIDTH("fullWidth"),

    ACTIVE_MODE("active_mode"),
    ENABLED("enabled"),

    SLOW("slow"),
    VERY_SLOW("very-slow"),

    BID_ACTIVE("bid_active"),
    ASK_ACTIVE("offer_active"),

    LAST_BID("last_buy"),
    LAST_ASK("last_sell"),

    IMPLIED_BID("impliedBid"),
    IMPLIED_ASK("impliedOffer"),

    POSITIVE("positive"),
    NEGATIVE("negative"),

    PRICE_TRADED("price_traded"),

    TRADED_UP("traded_up"),
    TRADED_DOWN("traded_down"),
    TRADED_AGAIN("traded_again"),

    CCY("currency"),
    MIC("mic"),

    AUCTION("AUCTION"),
    NO_BOOK_STATE("NO_BOOK_STATE"),

    SPREAD("spread"),
    REVERSE_SPREAD("reverseSpread"),
    BACK("back"),

    RECENTERING("recentering"),

    MODIFY_PRICE_SELECTED("modify_price_selected"),

    WORKING_QTY("working_qty"),
    WORKING_BID("working_bid"),
    WORKING_OFFER("working_offer"),
    WORKING_ORDER_TYPE("working_order_type_"),

    BOOK_VIEW("bookView"),
    STACK_VIEW("stackView"),
    STACK_QTY("stackQty"),
    STACK_OFFSET("stackOffset"),

    EEIF_ORDER_TYPE("eeif_order_type_managed"),

    MKT_CLOSE("MKT_CLOSE"),
    MIDPOINT("MIDPOINT"),
    HIDDEN("HIDDEN"),
    HAM("HAM"),
    HAMON("HAMON"),
    YAMON("YAMON"),
    YODA("YODA"),
    HAM3("HAM3"),
    HAMON3("HAMON3"),
    TRON("TRON"),
    TRON3("TRON3"),
    SNAGGIT("SNAGGIT"),
    MARKET("MARKET"),
    MANUAL("MANUAL"),
    TAKER("TAKER"),
    TICKTAKER("TICKTAKER"),
    HAWK("HAWK"),
    IOC("IOC"),
    GTC("GTC"),
    QUICKDRAW("QUICKDRAW"),

    PICARD("PICARD"),
    QUOTER("QUOTER"),

    EPHEMERAL("EPHEMERAL"),
    ONE_SHOT("ONE_SHOT"),
    AUTO_MANAGE("AUTO_MANAGE"),
    REFRESHABLE("REFRESHABLE"),

    ORDER("order"),
    BLANK_ORDER("blank_order"),
    OUR_ORDER("our_order"),
    MAYBE_OUR_OURDER("maybe_our_order"),
    ASK_ORDER("ask_order"),
    BID_ORDER("bid_order"),

    HIGHLIGHT_ORDER_0("highlight_order_0"),
    HIGHLIGHT_ORDER_1("highlight_order_1"),
    HIGHLIGHT_ORDER_2("highlight_order_2"),
    HIGHLIGHT_ORDER_3("highlight_order_3"),
    HIGHLIGHT_ORDER_4("highlight_order_4"),
    HIGHLIGHT_ORDER_5("highlight_order_5"),

    WORKING_ORDER_TYPE_HIDDEN_TICKTAKER("working_order_type_hidden_ticktaker"),
    WORKING_ORDER_TYPE_TAKER("working_order_type_taker"),
    WORKING_ORDER_TYPE_MANUAL("working_order_type_manual"),
    WORKING_ORDER_TYPE_HAWK("working_order_type_hawk"),
    WORKING_ORDER_TYPE_HEDGER("working_order_type_hedger"),
    WORKING_ORDER_TYPE_SPRINTER("working_order_type_sprinter"),
    WORKING_ORDER_TYPE_QUICKDRAW("working_order_type_quickdraw"),
    WORKING_ORDER_TYPE_MKT_CLOSE("working_order_type_mkt_close"),
    WORKING_ORDER_TYPE_MARKET("working_order_type_market"),
    WORKING_ORDER_TYPE_GTC("working_order_type_gtc"),
    WORKING_ORDER_TYPE_QUOTE("working_order_type_quote"),
    WORKING_ORDER_TYPE_DARK("working_order_type_dark"),
    WORKING_ORDER_TYPE_HIDDEN("working_order_type_hidden"),

    GOING_EX("going_ex"),
    ZOOMED_OUT("zoomed_out");

    public final String cssText;

    private CSSClass(final String cssText) {
        this.cssText = cssText;
    }

    public static final EnumSet<CSSClass> STACK_TYPES;
    public static final EnumSet<CSSClass> STACK_ORDER_TYPES;
    private static final Map<String, CSSClass> CSS_CLASS_BY_NAME;
    static final CSSClass[] CLASSES = values();

    static {
        STACK_TYPES = EnumSet.of(QUOTER, PICARD);
        STACK_ORDER_TYPES = EnumSet.of(EPHEMERAL, ONE_SHOT, AUTO_MANAGE, REFRESHABLE);

        CSS_CLASS_BY_NAME = new HashMap<>();
        for (final CSSClass cssClass : CSSClass.values()) {
            CSS_CLASS_BY_NAME.put(cssClass.name(), cssClass);
        }
    }

    public static CSSClass getCSSClass(final String name) {
        return CSS_CLASS_BY_NAME.get(name);
    }

    public static CSSClass getByOrdinal(int ordinal) {
        return CLASSES[ordinal];
    }
}

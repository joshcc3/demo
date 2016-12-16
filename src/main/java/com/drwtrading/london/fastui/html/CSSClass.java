package com.drwtrading.london.fastui.html;

import java.util.EnumSet;

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

    AUCTION("AUCTION"),
    NO_BOOK_STATE("NO_BOOK_STATE"),

    SPREAD("spread"),
    BACK("back"),

    RECENTERING("recentering"),

    MODIFY_PRICE_SELECTED("modify_price_selected"),

    WORKING_QTY("working_qty"),
    WORKING_BID("working_bid"),
    WORKING_OFFER("working_offer"),
    WORKING_ORDER_TYPE("working_order_type_"),

    STACK_VIEW("stackView"),
    STACK_QTY("stackQty"),

    EEIF_ORDER_TYPE("eeif_order_type_managed"),

    MKT_CLOSE("MKT_CLOSE"),
    HIDDEN("HIDDEN"),
    HAM("HAM"),
    HAMON("HAMON"),
    YAMON("YAMON"),
    YODA("YODA"),
    HAM3("HAM3"),
    HAMON3("HAMON3"),
    TRON("TRON"),
    TRON3("TRON3"),
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

    ONE_SHOT("ONE_SHOT"),
    AUTO_MANAGE("AUTO_MANAGE"),
    REFRESHABLE("REFRESHABLE"),

    WORKING_ORDER_TYPE_HIDDEN_TICKTAKER("working_order_type_hidden_ticktaker"),
    WORKING_ORDER_TYPE_TICKTAKER("working_order_type_ticktaker"),
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
    WORKING_ORDER_TYPE_LITTERBOX_HEDGE("working_order_type_litterbox_hedge"),
    WORKING_ORDER_TYPE_STRING("working_order_type_string"),
    WORKING_ORDER_TYPE_ICEBERG("working_order_type_iceberg"),
    WORKING_ORDER_TYPE_ICEBERG_RANDOM("working_order_type_iceberg_random"),
    WORKING_ORDER_TYPE_ICEBERG_TIP("working_order_type_iceberg_tip"),
    WORKING_ORDER_TYPE_AUTOSPREADER("working_order_type_autospreader"),
    WORKING_ORDER_TYPE_AUTOSPREADER_QUOTE("working_order_type_autospreader_quote"),
    WORKING_ORDER_TYPE_AUTOSPREAD_TAKER("working_order_type_autospread_taker"),
    WORKING_ORDER_TYPE_HAWKEYE("working_order_type_hawkeye");

    public final String cssText;

    private CSSClass(final String cssText) {
        this.cssText = cssText;
    }

    public static final EnumSet<CSSClass> STACK_TYPES;
    public static final EnumSet<CSSClass> STACK_ORDER_TYPES;

    static {
        STACK_TYPES = EnumSet.of(QUOTER, PICARD);
        STACK_ORDER_TYPES = EnumSet.of(ONE_SHOT, AUTO_MANAGE, REFRESHABLE);
    }
}

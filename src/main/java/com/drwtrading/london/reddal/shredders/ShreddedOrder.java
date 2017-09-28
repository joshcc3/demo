package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.fastui.html.CSSClass;

public class ShreddedOrder {

    String tag;
    String orderType;
    int queuePosition;
    int level;
    long quantity;
    boolean isOurs;
    final int previousQuantity;
    BookSide side;

    ShreddedOrder(final int queuePosition, final int level, final long quantity, final BookSide side, final int previousQuantity) {
        this.queuePosition = queuePosition;
        this.level = level;
        this.quantity = quantity;
        this.side = side;
        this.previousQuantity = previousQuantity;
    }

    CSSClass getCorrespondingCSSClass() {
        if (side == BookSide.BID) {
            return CSSClass.BID_ORDER;
        } else {
            return CSSClass.ASK_ORDER;
        }
    }

    CSSClass getOppositeCSSCClass() {
        if (side == BookSide.BID) {
            return CSSClass.ASK_ORDER;
        } else {
            return CSSClass.BID_ORDER;
        }
    }
}

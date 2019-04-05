package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.util.Objects;

public class PicardDistanceData implements Comparable<PicardDistanceData> {

    public final String symbol;

    public final boolean isValid;
    public final BookSide side;
    public final double bpsFromTouch;

    PicardDistanceData(final String symbol, final boolean isValid, final BookSide side, final double bpsFromTouch) {

        this.symbol = symbol;

        this.isValid = isValid;
        this.side = side;
        this.bpsFromTouch = bpsFromTouch;
    }

    @Override
    public int compareTo(final PicardDistanceData o) {

        if (this.isValid == o.isValid) {

            final int result = Double.compare(this.bpsFromTouch, o.bpsFromTouch);
            if (0 == result) {
                return this.symbol.compareTo(o.symbol);
            } else {
                return result;
            }

        } else if (this.isValid) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final PicardDistanceData that = (PicardDistanceData) o;
            return isValid == that.isValid && side == that.side && Objects.equals(symbol, that.symbol) &&
                    Math.abs(that.bpsFromTouch - bpsFromTouch) < Constants.EPSILON;
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(symbol, side);
    }
}

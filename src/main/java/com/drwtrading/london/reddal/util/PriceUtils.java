package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.prices.NormalizedPrice;
import com.drwtrading.london.prices.PriceFormat;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.prices.tickbands.TickSizeTracker;
import com.drwtrading.london.protocols.photon.marketdata.DecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedBandedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.PriceStructure;
import com.drwtrading.london.protocols.photon.marketdata.Side;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NavigableMap;

import static com.drwtrading.london.protocols.photon.marketdata.Side.BID;

public class PriceUtils implements PriceOperations {

    private final PriceOperationsUtil theOperationsToUse;

    public PriceUtils(final ITickTable tickTable) {
        theOperationsToUse = new TickLevelUtils(tickTable);
    }

    private PriceUtils(final PriceStructure priceStructure) {
        if (priceStructure.getTickStructure() instanceof NormalizedBandedDecimalTickStructure) {
            theOperationsToUse = new TickBandUtils((NormalizedBandedDecimalTickStructure) priceStructure.getTickStructure());
        } else {
            theOperationsToUse = new TickUtils(priceStructure);
        }
    }

    @Override
    public long tradablePrice(final long price, final Side side) {
        return theOperationsToUse.conservativeTradablePrice(price, side);
    }

    @Override
    public long nTicksAway(final long price, final int n, final Direction direction) {
        return theOperationsToUse.nTicksAway(price, n, direction);
    }

    public static PriceOperations from(final PriceStructure priceStructure) {
        return new PriceUtils(priceStructure);
    }

    public static PriceOperations from(final InstrumentDefinitionEvent instrumentDefinitionEvent) {
        return new PriceUtils(instrumentDefinitionEvent.getPriceStructure());
    }

    public static enum Direction {
        Add,
        Subtract
    }

    public static interface PriceOperationsUtil {

        long conservativeTradablePrice(long price, Side side);

        long nTicksAway(long price, long n, Direction direction);

    }

    protected static class TickUtils implements PriceUtils.PriceOperationsUtil {

        private final PriceStructure priceStructure;

        public TickUtils(final PriceStructure priceStructure) {
            this.priceStructure = priceStructure;
        }

        public long tickIncrement() {
            return priceStructure.getTickIncrement();
        }

        private static long roundConservativelyForNormalizedDecimalTickStructure(final BigDecimal price, final Side side,
                final PriceStructure priceStructure) {
            final PriceFormat priceFormat = PriceFormats.from(priceStructure.getTickStructure());
            final NormalizedDecimalTickStructure normalizedDecimalTickStructure =
                    (NormalizedDecimalTickStructure) priceStructure.getTickStructure();
            return priceFormat.normalizedPriceToProtocols(NormalizedPrice.from(
                    round(price, priceFormat.protocolsPriceToNormalized(priceStructure.getTickIncrement()).toBigDecimal(),
                            normalizedDecimalTickStructure.getDecimalPlaces(), side)));
        }

        @Override
        public long conservativeTradablePrice(final long nonTradeablePrice, final Side side) {
            if (priceStructure.getTickStructure() instanceof DecimalTickStructure) {
                return roundToTradablePrice(nonTradeablePrice, priceStructure.getTickIncrement(), 1, side);
            } else if (priceStructure.getTickStructure() instanceof NormalizedDecimalTickStructure) {
                return roundConservativelyForNormalizedDecimalTickStructure(
                        PriceFormats.from(priceStructure.getTickStructure()).toBigDecimal(new NormalizedPrice(nonTradeablePrice)), side,
                        priceStructure);
            } else {
                throw new IllegalArgumentException(
                        "Handle this other type of instrument definition" + priceStructure.getTickStructure().typeEnum());
            }
        }

        @Override
        public long nTicksAway(final long price, final long n, final Direction direction) {
            return direction == Direction.Add ? price + (n * tickIncrement()) : price - (n * tickIncrement());
        }

        private static long roundToTradablePrice(final long price, final long tickIncrement, final long modTickMultiplier,
                final Side side) {
            final long tickIncPostMod = tickIncrement * modTickMultiplier;
            long fractionalAmount = price % tickIncPostMod;
            fractionalAmount = fractionalAmount >= 0 ? fractionalAmount : fractionalAmount + tickIncPostMod;
            final long flooredPrice = price - fractionalAmount;
            if (fractionalAmount == 0 || side == BID) {
                return flooredPrice;
            } else {
                return flooredPrice + tickIncPostMod;
            }
        }

        public static BigDecimal round(final BigDecimal price, final BigDecimal roundingIncrement, final int decimalPrecisionForRounding,
                final Side side) {
            if (decimalPrecisionForRounding < 0) {
                throw new IllegalArgumentException(
                        "Rounding.round() cannot support negative decimal precision for rounding: " + decimalPrecisionForRounding +
                                ", price " + price);
            }
            BigDecimal answer = BigDecimal.valueOf(roundToTradablePrice(price.movePointRight(decimalPrecisionForRounding).setScale(0,
                            side == BID ? RoundingMode.FLOOR : RoundingMode.CEILING).longValueExact(),
                    roundingIncrement.movePointRight(decimalPrecisionForRounding).longValueExact(), 1, side)).movePointLeft(
                    decimalPrecisionForRounding);
            if (decimalPrecisionForRounding > 0) {
                answer = answer.setScale(decimalPrecisionForRounding);
            }
            return answer;
        }
    }

    protected static class TickBandUtils implements PriceUtils.PriceOperationsUtil {

        private final TickSizeTracker tracker;

        public TickBandUtils(final NormalizedBandedDecimalTickStructure tickStructure) {
            this.tracker = new TickSizeTracker(tickStructure.getBands());
        }

        @Override
        public long conservativeTradablePrice(final long value, final Side side) {
            final long rounded = tracker.roundDown(value);
            if (side == BID) {
                return rounded;
            } else {
                return value == rounded ? value : tracker.priceAbove(value);
            }
        }

        @Override
        public long nTicksAway(long price, final long n, final Direction direction) {
            for (int i = 0; i < n; i++) {
                price = direction == Direction.Add ? tracker.priceAbove(price) : tracker.priceBelow(price);
            }
            return price;
        }
    }

    protected static class TickLevelUtils implements PriceUtils.PriceOperationsUtil {

        private final ITickTable tickTable;

        TickLevelUtils(final ITickTable tickTable) {
            this.tickTable = tickTable;
        }

        @Override
        public long conservativeTradablePrice(final long price, final Side side) {

            final NavigableMap<Long, Long> tickLevels = tickTable.getRawTickLevels();
            if (Side.BID == side) {
                final Map.Entry<Long, Long> tickLevel = tickLevels.floorEntry(price);
                final long tickSize;
                if (null == tickLevel) {
                    tickSize = tickLevels.firstEntry().getValue();
                } else {
                    tickSize = tickLevel.getValue();
                }
                return tickSize * (price / tickSize);
            } else {
                final Map.Entry<Long, Long> tickLevel = tickLevels.floorEntry(price);
                final long tickSize;
                if (null == tickLevel) {
                    tickSize = tickLevels.lastEntry().getValue();
                } else {
                    tickSize = tickLevel.getValue();
                }
                return tickSize * (long) Math.ceil(price / (double) tickSize);
            }
        }

        @Override
        public long nTicksAway(final long price, final long n, final Direction direction) {

            if (Direction.Add == direction) {
                return tickTable.addTicks(price, n);
            } else {
                return tickTable.addTicks(price, -n);
            }
        }
    }
}
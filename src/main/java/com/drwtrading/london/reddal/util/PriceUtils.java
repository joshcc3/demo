package com.drwtrading.london.reddal.util;

import com.drwtrading.london.prices.NormalizedPrice;
import com.drwtrading.london.prices.PriceFormat;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.prices.tickbands.TickSizeTracker;
import com.drwtrading.london.protocols.photon.marketdata.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.drwtrading.london.protocols.photon.marketdata.Side.BID;
import static com.drwtrading.london.protocols.photon.marketdata.Side.OFFER;


public class PriceUtils implements PriceOperations {

    private final PriceOperationsUtil theOperationsToUse;

    private PriceUtils(PriceStructure priceStructure) {
        if (priceStructure.getTickStructure() instanceof NormalizedBandedDecimalTickStructure) {
            theOperationsToUse = new TickBandUtils((NormalizedBandedDecimalTickStructure) priceStructure.getTickStructure());
        } else {
            theOperationsToUse = new TickUtils(priceStructure);
        }
    }

    @Override
    public long tickIncrement(long nearbyPrice, Side side) {
        return theOperationsToUse.tickIncrement(nearbyPrice, side);
    }


    @Override
    public long tradeablePrice(BigDecimal price, Side side) {
        return theOperationsToUse.tradeablePrice(price, side);
    }

    @Override
    public long tradeablePrice(String price, Side side) {
        return theOperationsToUse.tradeablePrice(new BigDecimal(price), side);
    }

    @Override
    public long tradeablePrice(long price, Side side) {
        return theOperationsToUse.conservativeTradeablePrice(price, side);
    }

    @Override
    public long nTicksAway(long price, int n, Direction direction) {
        return theOperationsToUse.nTicksAway(price, n, direction);
    }


    public static PriceOperations from(PriceStructure priceStructure) {
        return new PriceUtils(priceStructure);
    }

    public static PriceOperations from(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        return new PriceUtils(instrumentDefinitionEvent.getPriceStructure());
    }

    public static enum Direction {
        Add, Subtract
    }

    public static interface PriceOperationsUtil {
        long tickIncrement(long nearbyPrice, Side side);

        long tradeablePrice(BigDecimal price, Side side);

        long conservativeTradeablePrice(long price, Side side);

        long nTicksAway(long price, long n, Direction direction);

    }

    protected static class TickUtils implements PriceUtils.PriceOperationsUtil {

        private final PriceStructure priceStructure;

        public TickUtils(final PriceStructure priceStructure) {
            this.priceStructure = priceStructure;
        }

        @Override
        public long tickIncrement(long nearbyPrice, Side side) {
            return priceStructure.getTickIncrement();
        }

        @Override
        public long tradeablePrice(BigDecimal price, Side side) {
            if (priceStructure.getTickStructure() instanceof DecimalTickStructure) {
                DecimalTickStructure decimalTickStructure = (DecimalTickStructure) priceStructure.getTickStructure();
                int pointPosition = decimalTickStructure.getPointPosition();
                long nonTradeablePrice = price.movePointRight(pointPosition).setScale(0, side == BID ? RoundingMode.FLOOR : RoundingMode.CEILING).longValueExact();
                return roundToTradeablePrice(nonTradeablePrice, priceStructure.getTickIncrement(), 1, side);
            } else if (priceStructure.getTickStructure() instanceof NormalizedDecimalTickStructure) {
                return roundConservativelyForNormalizedDecimalTickStructure(price, side, priceStructure);
            } else {
                throw new IllegalArgumentException("Handle this other type of instrument definition" + priceStructure.getClass());
            }
        }

        private long roundConservativelyForNormalizedDecimalTickStructure(BigDecimal price, Side side, PriceStructure priceStructure) {
            final PriceFormat priceFormat = PriceFormats.from(priceStructure.getTickStructure());
            final NormalizedDecimalTickStructure normalizedDecimalTickStructure = (NormalizedDecimalTickStructure) priceStructure.getTickStructure();
            return priceFormat.normalizedPriceToProtocols(NormalizedPrice.from(round(price, priceFormat.protocolsPriceToNormalized(priceStructure.getTickIncrement()).toBigDecimal(), normalizedDecimalTickStructure.getDecimalPlaces(), side)));
        }

        @Override
        public long conservativeTradeablePrice(long nonTradeablePrice, Side side) {
            if (priceStructure.getTickStructure() instanceof DecimalTickStructure) {
                return roundToTradeablePrice(nonTradeablePrice, priceStructure.getTickIncrement(), 1, side);
            } else if (priceStructure.getTickStructure() instanceof NormalizedDecimalTickStructure) {
                return roundConservativelyForNormalizedDecimalTickStructure(PriceFormats.from(priceStructure.getTickStructure()).toBigDecimal(new NormalizedPrice(nonTradeablePrice)), side, priceStructure);
            } else {
                throw new IllegalArgumentException("Handle this other type of instrument definition" + priceStructure.getClass());
            }
        }

        @Override
        public long nTicksAway(long price, long n, Direction direction) {
            return direction == Direction.Add ? price + (n * tickIncrement(price, BID)) : price - (n * tickIncrement(price, OFFER));
        }

        private long roundToTradeablePrice(long price, long tickIncrement, long modTickMultiplier, Side side) {
            final long tickIncPostMod = tickIncrement * modTickMultiplier;
            long fractionalAmount = price % tickIncPostMod;
            fractionalAmount = fractionalAmount >= 0 ? fractionalAmount : fractionalAmount + tickIncPostMod;
            long flooredPrice = price - fractionalAmount;
            if (fractionalAmount == 0 || side == BID) {
                return flooredPrice;
            } else {
                return flooredPrice + tickIncPostMod;
            }
        }

        public BigDecimal round(BigDecimal price, BigDecimal roundingIncrement, int decimalPrecisionForRounding, Side side) {
            if (decimalPrecisionForRounding < 0) {
                throw new IllegalArgumentException("Rounding.round() cannot support negative decimal precision for rounding: " + decimalPrecisionForRounding + ", price " + price);
            }
            BigDecimal answer = BigDecimal.valueOf(roundToTradeablePrice(price.movePointRight(decimalPrecisionForRounding).setScale(0, side == BID ? RoundingMode.FLOOR : RoundingMode.CEILING).longValueExact(), roundingIncrement.movePointRight(decimalPrecisionForRounding).longValueExact(), 1, side)).movePointLeft(decimalPrecisionForRounding);
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
        public long tradeablePrice(BigDecimal price, Side side) {
            final long value = price.movePointRight(PriceFormats.NORMAL_POINT_POSITION).setScale(0, side == BID ? RoundingMode.FLOOR : RoundingMode.CEILING).longValueExact();
            return conservativeTradeablePrice(value, side);
        }

        @Override
        public long conservativeTradeablePrice(long value, Side side) {
            long rounded = tracker.roundDown(value);
            if (side == BID) {
                return rounded;
            } else {
                return value == rounded ? value : tracker.priceAbove(value);
            }
        }

        @Override
        public long nTicksAway(long price, long n, Direction direction) {
            for (int i = 0; i < n; i++) {
                price = direction == Direction.Add ? tracker.priceAbove(price) : tracker.priceBelow(price);
            }
            return price;
        }

        public long tickIncrement(long nearbyPrice, Side side) {
            long rounded = tracker.roundDown(nearbyPrice);
            if (side == BID) {
                return rounded - tracker.priceBelow(rounded);
            } else {
                return tracker.priceAbove(rounded) - rounded;
            }
        }

    }
}
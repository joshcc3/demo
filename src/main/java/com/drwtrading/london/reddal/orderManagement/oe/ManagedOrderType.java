package com.drwtrading.london.reddal.orderManagement.oe;

import drw.eeif.eeifoe.BookParameters;
import drw.eeif.eeifoe.OrderParameters;
import drw.eeif.eeifoe.OrderSide;
import drw.eeif.eeifoe.PegPriceToTheoOnSubmit;
import drw.eeif.eeifoe.PegToPrice;
import drw.eeif.eeifoe.PegToTheo;
import drw.eeif.eeifoe.PredictionParameters;
import drw.eeif.eeifoe.QuotingParameters;
import drw.eeif.eeifoe.TakingParameters;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum ManagedOrderType {

    // Quote better by 1, no taking

    HAM {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    NO_TAKING, new QuotingParameters(true, 1, BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },
    HAM3 {
        @Override
        public OrderParameters getOrder(final long price, int qty, final OrderSide orderSide) {
            if (getQty(qty) == 0) {
                return HAM.getOrder(price, qty, orderSide);
            }
            qty = getQty(qty);
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    NO_TAKING, new QuotingParameters(true, 1, BETTER_BY_ONE, 1, 0, 0, qty / THREE, THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, THREE);
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },

    // Quote better by 0, no taking

    HAMON {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    NO_TAKING, new QuotingParameters(true, 1, NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },

    HAMON3 {
        @Override
        public OrderParameters getOrder(final long price, int qty, final OrderSide orderSide) {
            if (getQty(qty) == 0) {
                return HAMON.getOrder(price, qty, orderSide);
            }
            qty = getQty(qty);
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    NO_TAKING, new QuotingParameters(true, 1, NO_BETTERMENT, 1, 0, 0, qty / THREE, THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, THREE);
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },

    // Quote better by 1, take better than quote by one

    TRON {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    TAKE_BETTER_BY_ONE, new QuotingParameters(true, 1, BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },
    TRON3 {
        @Override
        public OrderParameters getOrder(final long price, int qty, final OrderSide orderSide) {
            if (getQty(qty) == 0) {
                return TRON.getOrder(price, qty, orderSide);
            }
            qty = getQty(qty);
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)), ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    TAKE_BETTER_BY_ONE, new QuotingParameters(true, 1, BETTER_BY_ONE, 1, 0, 0, qty / THREE, THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, THREE);
        }
    },

    // Raw taker at price
    SNAGGIT {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToPrice(price), new BookParameters(false, true, false, false, false, false, 0),
                    new TakingParameters(true, 0, 1_00_00, 25, false, 0), NO_QUOTING, new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return false;
        }
    };

    private static final BookParameters ALLOW_ALL_EXCEPT_STATE_TRANSITION = new BookParameters(true, true, false, true, true, false, 0);
    private static final TakingParameters TAKE_BETTER_BY_ONE = new TakingParameters(true, 0, 100, 2, true, 1);
    private static final TakingParameters NO_TAKING = new TakingParameters(false, 0, 0, 0, false, 0);
    private static final QuotingParameters NO_QUOTING = new QuotingParameters(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
    private static final int NO_BETTERMENT = 0;
    private static final int BETTER_BY_ONE = 1;
    private static final int THREE = 3;

    public static final Set<ManagedOrderType> EQUITY_TYPES =
            EnumSet.of(ManagedOrderType.HAM, ManagedOrderType.HAM3, ManagedOrderType.HAMON, ManagedOrderType.HAMON3, ManagedOrderType.TRON,
                    ManagedOrderType.TRON3);
    public static final Set<ManagedOrderType> FUTURE_TYPES = EnumSet.of(ManagedOrderType.SNAGGIT);

    private static final Map<String, ManagedOrderType> TYPES = new HashMap<>();

    static {

        for (final ManagedOrderType orderType : ManagedOrderType.values()) {
            TYPES.put(orderType.name(), orderType);
        }
    }

    public static ManagedOrderType getOrderType(final String name) {
        return TYPES.get(name);
    }

    public static int divisible(final int qty, final int i) {
        return qty - (qty % i);
    }

    public int getQty(final int qty) {
        return qty;
    }

    public abstract OrderParameters getOrder(final long price, final int qty, OrderSide orderSide);

    public abstract boolean requiresLean();

}

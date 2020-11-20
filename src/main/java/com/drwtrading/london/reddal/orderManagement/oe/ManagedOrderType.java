package com.drwtrading.london.reddal.orderManagement.oe;

import drw.eeif.eeifoe.BookParameters;
import drw.eeif.eeifoe.BookPegLevel;
import drw.eeif.eeifoe.OrderParameters;
import drw.eeif.eeifoe.OrderSide;
import drw.eeif.eeifoe.PegPriceToTheoOnSubmit;
import drw.eeif.eeifoe.PegToBook;
import drw.eeif.eeifoe.PegToPrice;
import drw.eeif.eeifoe.PegToTheo;
import drw.eeif.eeifoe.PredictionParameters;
import drw.eeif.eeifoe.QuotingParameters;
import drw.eeif.eeifoe.TakingParameters;

public enum ManagedOrderType {

    // Quote better by 1, no taking

    HAM {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4, false), new PredictionParameters(false));
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
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / Constants.THREE, Constants.THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, Constants.THREE);
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
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4, false), new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },

    YAMON {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4, false), new PredictionParameters(true));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }
    },

    YODA {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToBook(BookPegLevel.MID, 50), Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.NO_TAKING, new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4, false),
                    new PredictionParameters(true));
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
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty / Constants.THREE, Constants.THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, Constants.THREE);
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
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.TAKE_BETTER_BY_ONE,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4, false), new PredictionParameters(false));
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
            return new OrderParameters(new PegToTheo(101, 5, 10, new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION, Constants.TAKE_BETTER_BY_ONE,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / Constants.THREE, Constants.THREE, 0, 4, false),
                    new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return true;
        }

        @Override
        public int getQty(final int qty) {
            return divisible(qty, Constants.THREE);
        }
    },

    // Raw taker at price
    SNAGGIT {
        @Override
        public OrderParameters getOrder(final long price, final int qty, final OrderSide orderSide) {
            return new OrderParameters(new PegToPrice(price), new BookParameters(false, true, false, false, false, false, 0),
                    new TakingParameters(true, 0, 1_00_00, 25, false, 0), Constants.NO_QUOTING, new PredictionParameters(false));
        }

        @Override
        public boolean requiresLean() {
            return false;
        }
    };

    public static int divisible(final int qty, final int i) {
        return qty - (qty % i);
    }

    public abstract OrderParameters getOrder(final long price, final int qty, OrderSide orderSide);

    public abstract boolean requiresLean();

    public int getQty(final int qty) {
        return qty;
    }

    private static class Constants {

        public static final BookParameters ALLOW_ALL_EXCEPT_STATE_TRANSITION = new BookParameters(true, true, false, true, true, false, 0);
        public static final TakingParameters TAKE_BETTER_BY_ONE = new TakingParameters(true, 0, 100, 2, true, 1);
        public static final TakingParameters NO_TAKING = new TakingParameters(false, 0, 0, 0, false, 0);
        public static final QuotingParameters NO_QUOTING = new QuotingParameters(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        public static final int NO_BETTERMENT = 0;
        public static final int BETTER_BY_ONE = 1;
        public static final int THREE = 3;
    }

}

package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.photons.eeifoe.BookParameters;
import com.drwtrading.london.photons.eeifoe.ManagedOrder;
import com.drwtrading.london.photons.eeifoe.PegPriceToTheoOnSubmit;
import com.drwtrading.london.photons.eeifoe.QuotingParameters;
import com.drwtrading.london.photons.eeifoe.TakingParameters;
import com.drwtrading.london.photons.eeifoe.TheoPrice;

public enum ManagedOrderType {

    // Quote better by 1, no taking

    HAM {
        @Override
        public ManagedOrder getOrder(final long price, final int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAM3 {
        @Override
        public ManagedOrder getOrder(final long price, int qty) {
            if (qty < 3) {
                return HAM.getOrder(price, qty);
            }
            qty -= qty % 3;
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },

    // Quote better by 0, no taking

    HAMON {
        @Override
        public ManagedOrder getOrder(final long price, final int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAMON3 {
        @Override
        public ManagedOrder getOrder(final long price, int qty) {
            if (qty < 3) {
                return HAMON.getOrder(price, qty);
            }
            qty -= qty % 3;
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.NO_TAKING,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },

    // Quote better by 1, take better than quote by one

    TRON {
        @Override
        public ManagedOrder getOrder(final long price, final int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.TAKE_BETTER_BY_ONE,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    TRON3 {
        @Override
        public ManagedOrder getOrder(final long price, int qty) {
            if (qty < 3) {
                return TRON.getOrder(price, qty);
            }
            qty -= qty % 3;
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.ALLOW_ALL_EXCEPT_STATE_TRANSITION,
                    Constants.TAKE_BETTER_BY_ONE,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },

    ;


    public abstract ManagedOrder getOrder(final long price, final int qty);

    private static class Constants {
        public static final BookParameters ALLOW_ALL_EXCEPT_STATE_TRANSITION = new BookParameters(true, true, false, true, true);
        public static final TakingParameters TAKE_BETTER_BY_ONE = new TakingParameters(true, 0, 100, 20, true, 1);
        public static final TakingParameters NO_TAKING = new TakingParameters(false, 0, 0, 0, false, 0);
        public static final int NO_BETTERMENT = 0;
        public static final int BETTER_BY_ONE = 1;
    }

}

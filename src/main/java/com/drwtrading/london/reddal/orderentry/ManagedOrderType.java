package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.photons.eeifoe.BookParameters;
import com.drwtrading.london.photons.eeifoe.ManagedOrder;
import com.drwtrading.london.photons.eeifoe.PegPriceToTheoOnSubmit;
import com.drwtrading.london.photons.eeifoe.QuotingParameters;
import com.drwtrading.london.photons.eeifoe.TakingParameters;
import com.drwtrading.london.photons.eeifoe.TheoPrice;

public enum ManagedOrderType {

    HAM {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.NO_TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAMON {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.NO_TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAM3 {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.NO_TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },
    HAMON3 {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.NO_TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.NO_BETTERMENT, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },
    TRON {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    TRON3 {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, Constants.BETTER_BY_ONE, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },

    ;


    public abstract ManagedOrder getOrder(final long price, final int qty);

    private static class Constants {
        public static final BookParameters BOOK_PARAMETERS = new BookParameters(true, true, false, true, true);
        public static final TakingParameters TAKING_PARAMETERS = new TakingParameters(true, 0, 100, 20);
        public static final TakingParameters NO_TAKING_PARAMETERS = new TakingParameters(false, 0, 0, 0);
        public static final int NO_BETTERMENT = 0;
        public static final int BETTER_BY_ONE = 1;
    }
}

package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.photons.eeifoe.BookParameters;
import com.drwtrading.london.photons.eeifoe.ManagedOrder;
import com.drwtrading.london.photons.eeifoe.PegPriceToTheoOnSubmit;
import com.drwtrading.london.photons.eeifoe.QuotingParameters;
import com.drwtrading.london.photons.eeifoe.TakingParameters;
import com.drwtrading.london.photons.eeifoe.TheoPrice;

public enum ManagedOrderType {
    NEW_HAWK {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(
                    new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    new TakingParameters(false, 0, 0, 0),
                    new QuotingParameters(true, 0, 0, 1, 0, 0, qty, 1, 0, 4)
            );
        }
    },
    NEW_TAKER {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(
                    new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(false, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            );
        }
    },
    HAM {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, 1, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAMON {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, 0, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    HAM3 {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, 1, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },
    HAMON3 {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    Constants.BOOK_PARAMETERS,
                    Constants.TAKING_PARAMETERS,
                    new QuotingParameters(true, 1, 0, 1, 0, 0, qty / 3, 3, 0, 4));
        }
    },
    ;


    public abstract ManagedOrder getOrder(final long price, final int qty);

    private static class Constants {
        public static final BookParameters BOOK_PARAMETERS = new BookParameters(true, true, false, true, true);
        public static final TakingParameters TAKING_PARAMETERS = new TakingParameters(true, 0, 100, 20);
    }
}

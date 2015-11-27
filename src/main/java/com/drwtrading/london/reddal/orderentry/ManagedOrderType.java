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
                    new BookParameters(true, true, false, true, true),
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
                    new BookParameters(true, true, false, true, true),
                    new TakingParameters(true, 0, 100, 20),
                    new QuotingParameters(false, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            );
        }
    },
    JAM {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    new BookParameters(true, true, false, true, true),
                    new TakingParameters(true, 0, 100, 20),
                    new QuotingParameters(true, 1, 1, 1, 0, 0, qty, 1, 0, 4));
        }
    },
    JAMON {
        @Override
        public ManagedOrder getOrder(long price, int qty) {
            return new ManagedOrder(new TheoPrice(101, 5, 10, "PRICER", new PegPriceToTheoOnSubmit(price)),
                    new BookParameters(true, true, false, true, true),
                    new TakingParameters(true, 0, 100, 20),
                    new QuotingParameters(true, 1, 0, 1, 0, 0, qty, 1, 0, 4));
        }
    };

    public abstract ManagedOrder getOrder(final long price, final int qty);
}

package com.drwtrading.london.reddal.autopull.rules;

import java.util.function.BiPredicate;

public enum MktConditionConditional implements BiPredicate<Integer, Integer> {

    GT {
        @Override
        public boolean test(final Integer integer, final Integer integer2) {
            return integer > integer2;
        }
    },
    LT {
        @Override
        public boolean test(final Integer integer, final Integer integer2) {
            return integer < integer2;
        }
    }
}

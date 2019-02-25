package com.drwtrading.london.reddal.ladders.impliedGenerator;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.nibbler.transport.data.types.TheoTypes;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;

class ImpliedTheoGenerator {

    private final IClock clock;
    private final FutureConstant future;

    private final LadderPresenter ladderPresenter;
    private final ShredderPresenter shredderPresenter;

    private final TheoValue theoValue;

    private TheoValue frontTheo;
    private TheoValue backTheo;

    ImpliedTheoGenerator(final IClock clock, final String symbol, final FutureConstant future, final LadderPresenter ladderPresenter,
            final ShredderPresenter shredderPresenter) {

        this.clock = clock;
        this.future = future;

        this.ladderPresenter = ladderPresenter;
        this.shredderPresenter = shredderPresenter;

        this.theoValue = new TheoValue(0, symbol);

        this.frontTheo = null;
        this.backTheo = null;

        updateTheo();
    }

    void frontMonthUpdated(final TheoValue theoValue) {

        this.frontTheo = theoValue;
        updateTheo();
    }

    void backMonthUpdated(final TheoValue theoValue) {

        this.backTheo = theoValue;
        updateTheo();
    }

    private void updateTheo() {

        if (null != frontTheo && null != backTheo && frontTheo.isValid() && backTheo.isValid()) {

            final long nanoSinceMidnight = Math.max(frontTheo.getNanoSinceMidnightUTC(), backTheo.getNanoSinceMidnightUTC());
            final double afterHours = Math.max(frontTheo.getAfterHoursPct(), backTheo.getAfterHoursPct());
            final double rawAfterHours = Math.max(frontTheo.getRawAfterHoursPct(), backTheo.getRawAfterHoursPct());
            final long theoValue;
            final long originalValue;
            if (future.isReverseSpread) {
                theoValue = backTheo.getTheoreticalValue() - frontTheo.getTheoreticalValue();
                originalValue = backTheo.getOriginalValue() - frontTheo.getOriginalValue();
            } else {
                theoValue = frontTheo.getTheoreticalValue() - backTheo.getTheoreticalValue();
                originalValue = frontTheo.getOriginalValue() - backTheo.getOriginalValue();
            }

            this.theoValue.set(nanoSinceMidnight, true, TheoTypes.IMPLIED_RATE, theoValue, afterHours, rawAfterHours, 0d, originalValue);

        } else {

            final long nanoSinceMidnight = clock.getReferenceNanoSinceMidnightUTC();
            theoValue.set(nanoSinceMidnight, false, TheoTypes.IMPLIED_RATE, 0L, 100d, 100d, 0d, 0L);
        }

        ladderPresenter.setTheo(theoValue);
        shredderPresenter.setTheo(theoValue);
    }
}

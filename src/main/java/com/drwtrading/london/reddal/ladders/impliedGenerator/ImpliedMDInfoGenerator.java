package com.drwtrading.london.reddal.ladders.impliedGenerator;

import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;
import com.drwtrading.london.reddal.symbols.SearchResult;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class ImpliedMDInfoGenerator {

    private static final Set<FutureConstant> IMPLIED_FUTURES = EnumSet.of(FutureConstant.FEXD);

    private final IClock clock;
    private final IMDSubscriber depthBookSubscriber;

    private final LadderPresenter ladderPresenter;
    private final ShredderPresenter shredderPresenter;

    private final Set<String> handledSymbols;

    public ImpliedMDInfoGenerator(final IClock clock, final IMDSubscriber depthBookSubscriber, final LadderPresenter ladderPresenter,
            final ShredderPresenter shredderPresenter) {

        this.clock = clock;
        this.depthBookSubscriber = depthBookSubscriber;

        this.ladderPresenter = ladderPresenter;
        this.shredderPresenter = shredderPresenter;

        this.handledSymbols = new HashSet<>();
    }

    public void addInstrument(final SearchResult searchResult) {

        if (InstType.FUTURE_SPREAD == searchResult.instType && handledSymbols.add(searchResult.symbol)) {

            final String[] frontBack = searchResult.symbol.split("-");
            final String frontMonth = frontBack[0];
            final FutureConstant future = FutureConstant.getFutureFromSymbol(frontMonth);

            if (IMPLIED_FUTURES.contains(future)) {

                final ImpliedMDGenerator theoGenerator =
                        new ImpliedMDGenerator(clock, searchResult.symbol, future, ladderPresenter, shredderPresenter);

                final String backMonth = frontBack[1];

                depthBookSubscriber.subscribeForMDCallbacks(frontMonth, md -> theoGenerator.frontMonthUpdated(md.getBook()));
                depthBookSubscriber.subscribeForMDCallbacks(backMonth, md -> theoGenerator.backMonthUpdated(md.getBook()));
            }
        }
    }
}

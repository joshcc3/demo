package com.drwtrading.london.reddal.ladders.impliedGenerator;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;
import com.drwtrading.london.reddal.symbols.SearchResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImpliedTheoInfoGenerator {

    private static final Set<FutureConstant> IMPLIED_FUTURES = EnumSet.of(FutureConstant.FEXD);

    private final IClock clock;

    private final LadderPresenter ladderPresenter;
    private final ShredderPresenter shredderPresenter;

    private final Set<String> handledSymbols;
    private final Map<String, ArrayList<ImpliedTheoGenerator>> frontMonthListeners;
    private final Map<String, ArrayList<ImpliedTheoGenerator>> backMonthListeners;

    public ImpliedTheoInfoGenerator(final IClock clock, final LadderPresenter ladderPresenter, final ShredderPresenter shredderPresenter) {

        this.clock = clock;
        this.ladderPresenter = ladderPresenter;
        this.shredderPresenter = shredderPresenter;

        this.handledSymbols = new HashSet<>();
        this.frontMonthListeners = new HashMap<>();
        this.backMonthListeners = new HashMap<>();
    }

    public void addInstrument(final SearchResult inst) {

        if (InstType.FUTURE_SPREAD == inst.instType && handledSymbols.add(inst.symbol)) {

            final String[] frontBack = inst.symbol.split("-");
            final String frontMonth = frontBack[0];
            final FutureConstant future = FutureConstant.getFutureFromSymbol(frontMonth);

            if (IMPLIED_FUTURES.contains(future)) {

                final ImpliedTheoGenerator theoGenerator =
                        new ImpliedTheoGenerator(clock, inst.symbol, future, ladderPresenter, shredderPresenter);

                final List<ImpliedTheoGenerator> frontListeners = MapUtils.getMappedArrayList(frontMonthListeners, frontMonth);
                frontListeners.add(theoGenerator);

                final String backMonth = frontBack[1];
                final List<ImpliedTheoGenerator> backListeners = MapUtils.getMappedArrayList(backMonthListeners, backMonth);
                backListeners.add(theoGenerator);
            }
        }
    }

    public void setTheoValue(final TheoValue theoValue) {

        final String symbol = theoValue.getSymbol();

        final List<ImpliedTheoGenerator> frontListeners = frontMonthListeners.get(symbol);
        if (null != frontListeners) {
            frontListeners.forEach(listener -> listener.frontMonthUpdated(theoValue));
        }

        final List<ImpliedTheoGenerator> backListeners = backMonthListeners.get(symbol);
        if (null != backListeners) {
            backListeners.forEach(listener -> listener.backMonthUpdated(theoValue));
        }
    }
}

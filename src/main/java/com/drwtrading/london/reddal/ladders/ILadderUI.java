package com.drwtrading.london.reddal.ladders;

import java.util.Collection;

public interface ILadderUI {

    void draw(final int levels);

    void trading(final boolean tradingEnabled, final Collection<String> workingOrderTags, final Collection<String> orderTypesLeft,
            final Collection<String> orderTypesRight);

    void goToSymbol(final String symbol);

    void goToUrl(final String url);

    void popUp(final String url, final String name, final int width, final int height);

    void launchBasket(final String symbol);

    void replace(final String from, final String to);

    void setDescription(String desc);
}

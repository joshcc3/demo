package com.drwtrading.london.reddal.ladders;

import java.util.Collection;

public interface ILadderUI {

    public void draw(final int levels);

    public void trading(final boolean tradingEnabled, final Collection<String> workingOrderTags, final Collection<String> orderTypes);

    public void goToSymbol(final String symbol);

    public void goToUrl(final String url);

    public void popUp(final String url, final String name, final int width, final int height);

    public void launchBasket(final String symbol);

    public void replace(final String from, final String to);
}

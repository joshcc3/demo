package com.drwtrading.london.reddal.ladders;

import java.util.Collection;

public interface View {

    void draw(int levels);

    void trading(boolean tradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight);

    void selecta(boolean enabled);

    void goToSymbol(String symbol);

    void goToUrl(String url);

    void popUp(String url, String name, int width, int height);

    void launchBasket(String symbol);

    void replace(String from, String to);
}

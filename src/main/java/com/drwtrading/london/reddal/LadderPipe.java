package com.drwtrading.london.reddal;

import java.util.Collection;

public interface LadderPipe {

    char DATA_SEPARATOR = '\0';
    char COMMAND_SEPARATOR = '\1';

    String DRAW_CMD = "draw";
    String TRADING_CMD = "trading";
    String TXT_CMD = "txt";
    String CLS_CMD = "cls";
    String DATA_CMD = "data";
    String HEIGHT_CMD = "height";

    // Attaches {dataKey:value} pair to element #key
    void data(String key, String dataKey, Object value);

    // Toggles class cssClass on element #key
    void cls(String key, String cssClass, boolean enabled);

    // Sets text of element #key to value
    void txt(String key, Object value);

    // Draws a fresh ladder
    void draw(int levels, String symbol);

    // Display click-trading
    void trading(boolean clickTradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight);

    // Moves top of element #moveId to the center of #refId, offset by heightFraction * height of #refId (positive is up)
    void height(String moveId, String refId, double heightFraction);

    // Clear existing data
    void clear();

    // Flush pending updates
    void flush();

}

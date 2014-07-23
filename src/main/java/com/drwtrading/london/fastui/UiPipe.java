package com.drwtrading.london.fastui;

import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.Map;

public interface UiPipe {

    char DATA_SEPARATOR = '\0';
    char COMMAND_SEPARATOR = '\1';

    String CLEAR_CMD = "clear";
    String TXT_CMD = "txt";
    String CLS_CMD = "cls";
    String DATA_CMD = "data";
    String HEIGHT_CMD = "height";
    String EVAL_CMD = "eval";
    String CLICKABLE_CMD = "clickable";
    String SCROLLABLE_CMD = "scrollable";
    String TITLE_CMD = "title";

    // Gives a publisher that passes through eval .. useful for passing to the WebSocketViews framework for
    // tasks that are not handled by fastUI
    Publisher<WebSocketOutboundData> evalPublisher();

    // Attaches {dataKey:value} pair to element #key
    void data(String key, String dataKey, Object value);

    // Toggles class cssClass on element #key
    void cls(String key, String cssClass, boolean enabled);

    // Sets text of element #key to value
    void txt(String key, Object value);

    // Moves top of element #moveId to the center of #refId, offset by heightFraction * height of #refId (positive is up)
    void height(String moveId, String refId, double heightFraction);

    // Evals the javascript string sent. Because this cannot be batched, it flushes any pending data and then sends the
    // string immedialy.
    void eval(String eval);

    // Make clicking #id send a 'click,id,{data}' message down the websocket
    void clickable(String id);

    // Make scrolling on #id send a 'scroll,[up|down],id,{data}' message down the websocket
    void scrollable(String id);

    // Set the title of the window
    void title(String title);

    // Clear existing data
    void clear();

    // Flush pending updates
    void flush();

    // Add a handler for inbound events
    void setHandler(UiEventHandler eventHandler);

    // Inbound event occurred, parse it and pass it out to handlers
    void onInbound(String data);

    public interface UiEventHandler {
        public void onClick(String id, Map<String,String> data);
        public void onDblClick(String id, Map<String,String> data);
        public void onUpdate(String id, Map<String,String> data);
        public void onScroll(String direction);
        public void onKeyDown(int keyCode);
        public void onIncoming(String[] args);
    }

}

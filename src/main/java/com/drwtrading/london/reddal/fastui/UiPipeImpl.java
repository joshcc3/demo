package com.drwtrading.london.reddal.fastui;

import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UiPipeImpl {

    static final char DATA_SEPARATOR = '\0';
    static final char COMMAND_SEPARATOR = '\1';

    private static final String CLEAR_CMD = "clear";
    private static final String TXT_CMD = "txt";
    private static final String CLS_CMD = "cls";
    private static final String DATA_CMD = "data";
    private static final String HEIGHT_CMD = "height";
    private static final String WIDTH_CMD = "width";
    private static final String EVAL_CMD = "eval";
    private static final String CLICKABLE_CMD = "clickable";
    private static final String SCROLLABLE_CMD = "scrollable";
    private static final String TITLE_CMD = "title";

    private final Joiner commandJoiner = Joiner.on(COMMAND_SEPARATOR);
    private final Publisher<WebSocketOutboundData> pipe;

    private final KeyedBatcher text = new KeyedBatcher(TXT_CMD);
    private final ClassBatcher classes = new ClassBatcher(CLS_CMD);
    private final DataBatcher<DataKey> data = new DataBatcher<>(DATA_CMD, DataKey.class);
    private final KeyedBatcher height = new KeyedBatcher(HEIGHT_CMD);
    private final KeyedBatcher width = new KeyedBatcher(WIDTH_CMD);

    private final ListBatcher clickable = new ListBatcher(CLICKABLE_CMD);
    private final ListBatcher scrollable = new ListBatcher(SCROLLABLE_CMD);
    private final StringBatcher titleBatcher = new StringBatcher(TITLE_CMD);

    private UiEventHandler inboundHandler;

    public UiPipeImpl(final Publisher<WebSocketOutboundData> pipe) {
        this.pipe = pipe;
    }

    // Attaches {dataKey:value} pair to element #key
    public void data(final String key, final DataKey dataKey, final Object value) {
        data.put(key, dataKey, value.toString());
    }

    // Toggles class cssClass on element #key
    public void cls(final String key, final CSSClass cssClass, final boolean enabled) {
        classes.put(key, cssClass, enabled);
    }

    // Sets text of element #key to value
    public void txt(final String key, final Object value) {
        text.put(key, value.toString());
    }

    // Moves top of element #moveId to the center of #refId, offset by heightFraction * height of #refId (positive is up)
    public void height(final String moveId, final String refId, final double heightFraction) {
        final String value = cmd(refId, String.format("%.1f", heightFraction));
        height.put(moveId, value);
    }

    public void width(final String moveId, final double widthPct) {
        this.width.put(moveId, String.format("%.2f%%", widthPct));
    }

    public void eval(final String eval) {
        flush();
        send(cmd(EVAL_CMD, eval));
    }

    public void clickable(final String id) {
        clickable.put(id);
    }

    public void scrollable(final String id) {
        scrollable.put(id);
    }

    public void title(final String title) {
        titleBatcher.put(title);
    }

    // Buffer interface

    public void clear() {
        text.clear();
        classes.clear();
        data.clear();
        height.clear();
        width.clear();
        clickable.clear();
        scrollable.clear();
        titleBatcher.clear();
        send(cmd(CLEAR_CMD));
    }

    public void flush() {
        final List<String> commands = new ArrayList<>();
        text.flushPendingIntoCommandList(commands);
        data.flushPendingIntoCommandList(commands);
        classes.flushPendingIntoCommandList(commands);
        height.flushPendingIntoCommandList(commands);
        width.flushPendingIntoCommandList(commands);
        clickable.flushPendingIntoCommandList(commands);
        scrollable.flushPendingIntoCommandList(commands);
        titleBatcher.flushPendingIntoCommandList(commands);
        if (!commands.isEmpty()) {
            send(commandJoiner.join(commands));
        }
    }

    public void setHandler(final UiEventHandler eventHandler) {
        this.inboundHandler = eventHandler;
    }

    public void onInbound(final String data) {

        if (null != inboundHandler) {
            final String[] args = data.split("\0");
            final String cmd = args[0];
            if ("heartbeat".equals(cmd)) {
                final long sentTimeMillis = Long.valueOf(args[1]);
                inboundHandler.onHeartbeat(sentTimeMillis);
            } else if ("click".equals(cmd)) {
                inboundHandler.onClick(args[1], getDataArg(args));
            } else if ("scroll".equals(cmd)) {
                inboundHandler.onScroll(args[1]);
            } else if ("dblclick".equals(cmd)) {
                inboundHandler.onDblClick(args[1], getDataArg(args));
            } else if ("update".equals(cmd)) {
                inboundHandler.onUpdate(args[1], getDataArg(args));
            } else if ("keydown".equals(cmd)) {
                inboundHandler.onKeyDown(Integer.parseInt(args[1]));
            }
        }
    }

    public void send(final String cmd) {
        pipe.publish(new WebSocketOutboundData(cmd));
    }

    public static String cmd(final Object... args) {
        return Joiner.on(DATA_SEPARATOR).join(args);
    }

    public static Map<String, String> getDataArg(final String[] args) {
        final Map<String, String> data = new HashMap<>();
        final int arg = 2;
        if (args.length > arg) {
            final String[] unpacked = args[arg].split("\2");
            for (int i = 0; i < unpacked.length - 1; i += 2) {
                data.put(unpacked[i], unpacked[i + 1]);
            }
        }
        return data;
    }
}

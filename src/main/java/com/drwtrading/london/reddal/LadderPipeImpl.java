package com.drwtrading.london.reddal;

import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LadderPipeImpl implements LadderPipe {

    private final Publisher<WebSocketOutboundData> pipe;

    private final Map<String, String> textById = new HashMap<String, String>();
    private final Map<String, String> pendingTextById = new HashMap<String, String>();

    public final Map<String, String> classesById = new HashMap<String, String>();
    public final Map<String, String> pendingClassesById = new HashMap<String, String>();

    public final Map<String, String> dataById = new HashMap<String, String>();
    public final Map<String, String> pendingDataById = new HashMap<String, String>();

    public final Map<String, String> heightById = new HashMap<String, String>();
    public final Map<String, String> pendingHeightById = new HashMap<String, String>();

    public LadderPipeImpl(Publisher<WebSocketOutboundData> pipe) {
        this.pipe = pipe;
    }

    // Ladder pipe interface

    // Attaches {dataKey:value} pair to element #key
    @Override
    public void data(String key, String dataKey, Object value) {
        String jointKey = cmd(key, dataKey);
        String dataValue = value.toString();
        if (!dataValue.equals(dataById.put(jointKey, dataValue))) {
            pendingDataById.put(jointKey, dataValue);
        }
    }

    // Toggles class cssClass on element #key
    @Override
    public void cls(String key, String cssClass, boolean enabled) {
        String jointKey = cmd(key, cssClass);
        String toggled = enabled ? "true" : "false";
        if (!toggled.equals(classesById.get(jointKey))) {
            pendingClassesById.put(jointKey, toggled);
        } else {
            pendingClassesById.remove(jointKey);
        }
    }

    // Sets text of element #key to value
    @Override
    public void txt(String key, Object value) {
        String string = value.toString();
        if (!string.equals(textById.put(key, string))) {
            pendingTextById.put(key, string);
        }
    }

    // Draws a fresh ladder
    @Override
    public void draw(int levels, String symbol) {
        send(cmd(DRAW_CMD, levels, symbol));
        clear();
    }

    @Override
    public void trading(boolean clickTradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight) {
        send(cmd(TRADING_CMD, clickTradingEnabled ? "true" : "false", orderTypesLeft.size(), cmd(orderTypesLeft.toArray()), orderTypesRight.size(), cmd(orderTypesRight.toArray())));
    }

    // Moves top of element #moveId to the center of #refId, offset by heightFraction * height of #refId (positive is up)
    @Override
    public void height(String moveId, String refId, double heightFraction) {
        String value = cmd(refId, String.format("%.1f", heightFraction));
        if (!value.equals(heightById.put(moveId, value))) {
            pendingHeightById.put(moveId, value);
        }
    }

    // Buffer interface

    @Override
    public void clear() {
        textById.clear();
        pendingTextById.clear();
        classesById.clear();
        pendingClassesById.clear();
        dataById.clear();
        pendingDataById.clear();
        heightById.clear();
        pendingHeightById.clear();
    }

    @Override
    public void flush() {

        List<String> commands = new ArrayList<String>();

        if (pendingTextById.size() > 0) {
            commands.add(updatePending(TXT_CMD, pendingTextById, textById));
        }

        if (pendingDataById.size() > 0) {
            commands.add(updatePending(DATA_CMD, pendingDataById, dataById));
        }

        if (pendingClassesById.size() > 0) {
            commands.add(updatePending(CLS_CMD, pendingClassesById, classesById));
        }

        if (pendingHeightById.size() > 0) {
            commands.add(updatePending(HEIGHT_CMD, pendingHeightById, heightById));
        }

        if (commands.size() > 0) {
            send(Joiner.on(COMMAND_SEPARATOR).join(commands));
        }

    }

    private String updatePending(String command, Map<String, String> pending, Map<String, String> existing) {
        List<String> updates = new ArrayList<String>();
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            updates.add(entry.getKey());
            updates.add(entry.getValue());
        }
        existing.putAll(pending);
        pending.clear();
        return cmd(command, cmd(updates.toArray()));
    }

    private void send(String cmd) {
        pipe.publish(new WebSocketOutboundData(cmd));
    }

    public static String cmd(Object... args) {
        return Joiner.on(DATA_SEPARATOR).join(args);
    }

}

package com.drwtrading.london.fastui;

import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastList;

public class UiPipeImpl implements UiPipe {

    public final Joiner commandJoiner = Joiner.on(COMMAND_SEPARATOR);
    private final Publisher<WebSocketOutboundData> pipe;

    public static class KeyedBatcher {

        public final Map<String, String> values = new HashMap<String, String>();
        public final Map<String, String> pendingValues = new HashMap<String, String>();
        private String command;

        public KeyedBatcher(String command) {
            this.command = command;
        }

        public void put(String key, String value) {
            if (!value.equals(values.get(key))) {
                pendingValues.put(key, value);
            } else {
                pendingValues.remove(key);
            }
        }

        public void flushPendingIntoCommandList(List<String> commands) {
            if (pendingValues.size() == 0) {
                return;
            }
            commands.add(getCommand());
            values.putAll(pendingValues);
            pendingValues.clear();
        }

        public String getCommand() {
            List<String> updates = new ArrayList<String>();
            for (Map.Entry<String, String> entry : pendingValues.entrySet()) {
                updates.add(entry.getKey());
                updates.add(entry.getValue());
            }
            return cmd(this.command, cmd(updates.toArray()));
        }

        public void clear() {
            values.clear();
            pendingValues.clear();
        }

    }

    public static class ListBatcher {

        public final List<String> pendingValues = newFastList();
        private String command;

        public ListBatcher(String command) {
            this.command = command;
        }

        public void put(String value) {
            pendingValues.add(value);
        }

        public void flushPendingIntoCommandList(List<String> commands) {
            if (pendingValues.size() == 0) {
                return;
            }
            commands.add(getCommand());
            pendingValues.clear();
        }

        public String getCommand() {
            return cmd(this.command, cmd(pendingValues.toArray()));
        }

        public void clear() {
            pendingValues.clear();
        }

    }

    public static class StringBatcher {

        public String pendingValue = "";
        public String value = "";
        private String command;

        public StringBatcher(String command) {
            this.command = command;
        }

        public void put(String value) {
            pendingValue = value;
        }

        public void flushPendingIntoCommandList(List<String> commands) {
            if (!pendingValue.equals(value)) {
                commands.add(cmd(this.command, pendingValue));
                value = pendingValue;
            }
        }

        public void clear() {
            pendingValue = "";
            value = "";
        }

    }

    final KeyedBatcher text = new KeyedBatcher(TXT_CMD);
    final KeyedBatcher classes = new KeyedBatcher(CLS_CMD);
    final KeyedBatcher data = new KeyedBatcher(DATA_CMD);
    final KeyedBatcher height = new KeyedBatcher(HEIGHT_CMD);

    final ListBatcher clickable = new ListBatcher(CLICKABLE_CMD);
    final ListBatcher scrollable = new ListBatcher(SCROLLABLE_CMD);
    final StringBatcher titleBatcher = new StringBatcher(TITLE_CMD);

    UiEventHandler inboundHandler;

    public UiPipeImpl(Publisher<WebSocketOutboundData> pipe) {
        this.pipe = pipe;
    }

    @Override
    public Publisher<WebSocketOutboundData> evalPublisher() {
        return new Publisher<WebSocketOutboundData>() {
            @Override
            public void publish(WebSocketOutboundData msg) {
                eval(msg.getData());
            }
        };
    }

    // Attaches {dataKey:value} pair to element #key
    @Override
    public void data(String key, String dataKey, Object value) {
        data.put(cmd(key, dataKey), value.toString());
    }

    // Toggles class cssClass on element #key
    @Override
    public void cls(String key, String cssClass, boolean enabled) {
        classes.put(key + DATA_SEPARATOR + cssClass, enabled ? "true" : "false");
    }

    // Sets text of element #key to value
    @Override
    public void txt(String key, Object value) {
        text.put(key, value.toString());
    }

    // Moves top of element #moveId to the center of #refId, offset by heightFraction * height of #refId (positive is up)
    @Override
    public void height(String moveId, String refId, double heightFraction) {
        String value = cmd(refId, String.format("%.1f", heightFraction));
        height.put(moveId, value);
    }

    @Override
    public void eval(String eval) {
        flush();
        send(cmd(EVAL_CMD, eval));
    }

    @Override
    public void clickable(String id) {
        clickable.put(id);
    }

    @Override
    public void scrollable(String id) {
        scrollable.put(id);
    }

    @Override
    public void title(String title) {
        titleBatcher.put(title);
    }

    // Buffer interface

    @Override
    public void clear() {
        text.clear();
        classes.clear();
        data.clear();
        height.clear();
        clickable.clear();
        scrollable.clear();
        titleBatcher.clear();
        send(cmd(CLEAR_CMD));
    }

    @Override
    public void flush() {
        List<String> commands = new ArrayList<String>();
        text.flushPendingIntoCommandList(commands);
        data.flushPendingIntoCommandList(commands);
        classes.flushPendingIntoCommandList(commands);
        height.flushPendingIntoCommandList(commands);
        clickable.flushPendingIntoCommandList(commands);
        scrollable.flushPendingIntoCommandList(commands);
        titleBatcher.flushPendingIntoCommandList(commands);
        if (commands.size() > 0) {
            send(commandJoiner.join(commands));
        }
    }

    @Override
    public void setHandler(UiEventHandler eventHandler) {
        this.inboundHandler = eventHandler;
    }

    @Override
    public void onInbound(String data) {
        if (inboundHandler == null) {
            return;
        }
        String[] args = data.split("\0");
        String cmd = args[0];
        if (cmd.equals("click")) {
            inboundHandler.onClick(args[1], getDataArg(args));
        } else if (cmd.equals("scroll")) {
            inboundHandler.onScroll(args[1]);
        } else if (cmd.equals("dblclick")) {
            inboundHandler.onDblClick(args[1], getDataArg(args));
        } else if (cmd.equals("update")) {
            inboundHandler.onUpdate(args[1], getDataArg(args));
        } else if (cmd.equals("keydown")) {
            inboundHandler.onKeyDown(Integer.parseInt(args[1]));
        } else {
            inboundHandler.onIncoming(args);
        }
    }

    public void send(String cmd) {
        pipe.publish(new WebSocketOutboundData(cmd));
    }

    public static String cmd(Object... args) {
        return Joiner.on(DATA_SEPARATOR).join(args);
    }

    public static Map<String, String> getDataArg(String[] args) {
        Map<String, String> data = new HashMap<String, String>();
        int arg = 2;
        if (args.length > arg) {
            String[] unpacked = args[arg].split("\2");
            for (int i = 0; i < unpacked.length - 1; i += 2) {
                data.put(unpacked[i], unpacked[i + 1]);
            }
        }
        return data;
    }


}

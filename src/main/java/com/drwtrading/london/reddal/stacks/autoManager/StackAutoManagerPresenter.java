package com.drwtrading.london.reddal.stacks.autoManager;

import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.symbols.SymbolReferencePrice;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.EnumMap;
import java.util.Map;

public class StackAutoManagerPresenter {

    private static final String WEB_SOURCE = "stackAutoManager";

    private final UILogger uiLogger;
    private final WebSocketViews<IStackAutoManagerView> views;

    private final Map<InstType, IStackInstTypeAutoManager> autoManagers;

    public StackAutoManagerPresenter(final UILogger uiLogger) {

        this.uiLogger = uiLogger;
        this.views = new WebSocketViews<>(IStackAutoManagerView.class, this);

        this.autoManagers = new EnumMap<>(InstType.class);

        for (final InstType instType : InstType.values()) {

            autoManagers.put(instType, StackNoAutoManager.INSTANCE);
        }

        autoManagers.put(InstType.ETF, new StackETFAutoManager());
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            final WebSocketInboundData msg = (WebSocketInboundData) webMsg;

            uiLogger.write(WEB_SOURCE, msg);
            views.invoke(msg);
        }
    }

    public void setConfigClient(final String nibblerName, final StackClientHandler cache) {

        for (final IStackInstTypeAutoManager instAutoManager : autoManagers.values()) {

            instAutoManager.setConfigClient(nibblerName, cache);
        }
    }

    public void configUpdated(final String nibblerName, final StackConfigGroup configGroup) {

        for (final IStackInstTypeAutoManager instAutoManager : autoManagers.values()) {

            instAutoManager.configUpdated(nibblerName, configGroup);
        }
    }

    public void serverConnectionLost(final String nibblerName) {

        for (final IStackInstTypeAutoManager instAutoManager : autoManagers.values()) {

            instAutoManager.serverConnectionLost(nibblerName);
        }
    }

    public void addRefPrice(final SymbolReferencePrice refPrice) {

        final IStackInstTypeAutoManager autoManager = autoManagers.get(refPrice.inst.getInstType());
        autoManager.addRefPrice(refPrice);
    }

    @FromWebSocketView
    public void setModLevels(final String instrumentType, final int bpsEquivalent) {

        final InstType instType = InstType.getInstType(instrumentType);
        final IStackInstTypeAutoManager autoManager = autoManagers.get(instType);
        autoManager.setModLevels(bpsEquivalent);
    }
}

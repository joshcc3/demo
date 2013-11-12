package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.protocols.photon.execution.RemoteOrderManagementCommand;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.util.KeyedPublisher;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.marketdata.service.util.MarketDataEventUtil;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.ladder.LastTrade;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LadderPresenter {

    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;

    Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    Map<Publisher<WebSocketOutboundData>, LadderView> viewBySocket = new HashMap<Publisher<WebSocketOutboundData>, LadderView>();
    Map<String, MarketDataForSymbol> marketDataBySymbol = new MapMaker().makeComputingMap(new Function<String, MarketDataForSymbol>() {
        @Override
        public MarketDataForSymbol apply(String from) {
            return new MarketDataForSymbol(from);
        }
    });
    Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(new Function<String, WorkingOrdersForSymbol>() {
        @Override
        public WorkingOrdersForSymbol apply(java.lang.String from) {
            return new WorkingOrdersForSymbol(from);
        }
    });
    Map<String, ExtraDataForSymbol> dataBySymbol = new MapMaker().makeComputingMap(new Function<String, ExtraDataForSymbol>() {
        @Override
        public ExtraDataForSymbol apply(java.lang.String from) {
            return new ExtraDataForSymbol(from);
        }
    });

    public LadderPresenter(Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer, LadderOptions ladderOptions) {
        this.remoteOrderCommandByServer = remoteOrderCommandByServer;
        this.ladderOptions = ladderOptions;
    }

    @Subscribe
    public void onConnected(WebSocketConnected connected) {
        UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        View view = new WebSocketOutputDispatcher<View>(View.class).wrap(uiPipe.evalPublisher());
        viewBySocket.put(connected.getOutboundChannel(), new LadderView(connected.getClient(), uiPipe, view ,remoteOrderCommandByServer, ladderOptions));
    }

    @Subscribe
    public void onDisconnected(WebSocketDisconnected disconnected) {
        LadderView view = viewBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            viewsBySymbol.remove(view.symbol, view);
        }
    }

    @Subscribe
    public void onMessage(WebSocketInboundData msg) {
        String data = msg.getData();
        String[] args = data.split("\0");
        String cmd = args[0];
        LadderView view = viewBySocket.get(msg.getOutboundChannel());
        if (cmd.equals("ladder-subscribe")) {
            String symbol = args[1];
            int levels = Integer.parseInt(args[2]);
            view.subscribeToSymbol(symbol, levels, marketDataBySymbol.get(symbol), ordersBySymbol.get(symbol), dataBySymbol.get(symbol));
            viewsBySymbol.put(symbol, view);
        } else {
            view.onInbound(args);
        }
    }

    // Data

    @Subscribe
    public void on(Main.WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.value.getSymbol()).on(workingOrderUpdate);
        for (LadderView ladderView : viewsBySymbol.get(workingOrderUpdate.value.getSymbol())) {
            ladderView.onWorkingOrderUpdate(workingOrderUpdate);
        }
    }

    @Subscribe
    public void on(LaserLine laserLine) {
            ExtraDataForSymbol data = dataBySymbol.get(laserLine.getSymbol());
            data.onLaserLine(laserLine);
            for (LadderView ladderView : viewsBySymbol.get(laserLine.getSymbol())) {
                ladderView.onMetadata(laserLine);
            }
    }
    @Subscribe
    public void on(DeskPosition deskPosition) {
            ExtraDataForSymbol data = dataBySymbol.get(deskPosition.getSymbol());
            data.onDeskPosition(deskPosition);
            for (LadderView ladderView : viewsBySymbol.get(deskPosition.getSymbol())) {
                ladderView.onMetadata(deskPosition);
            }
    }

    @Subscribe
    public void on(InfoOnLadder infoOnLadder) {
        ExtraDataForSymbol data = dataBySymbol.get(infoOnLadder.getSymbol());
        data.onInfoOnLadder(infoOnLadder);
        for (LadderView ladderView : viewsBySymbol.get(infoOnLadder.getSymbol())) {
            ladderView.onMetadata(infoOnLadder);
        }
    }

    @Subscribe
    public void on(LadderText ladderText) {
        ExtraDataForSymbol data = dataBySymbol.get(ladderText.getSymbol());
        data.onLadderText(ladderText);
        for (LadderView ladderView : viewsBySymbol.get(ladderText.getSymbol())) {
            ladderView.onMetadata(ladderText);
        }
    }

    @Subscribe
    public void on(LastTrade lastTrade) {
        ExtraDataForSymbol data = dataBySymbol.get(lastTrade.getSymbol());
        data.onLastTrade(lastTrade);
        for (LadderView ladderView : viewsBySymbol.get(lastTrade.getSymbol())) {
            ladderView.onMetadata(lastTrade);
        }
    }

    public Callback<List<MarketDataEvent>> onMarketData() {
        return new Callback<List<MarketDataEvent>>() {
            @Override
            public void onMessage(List<MarketDataEvent> message) {
                for (MarketDataEvent marketDataEvent : message) {
                    String symbol = MarketDataEventUtil.getSymbol(marketDataEvent);
                    if (symbol == null) {
                        for (MarketDataForSymbol marketDataForSymbol : marketDataBySymbol.values()) {
                            marketDataForSymbol.onMarketDataEvent(marketDataEvent);
                        }
                        for (LadderView view : new HashSet<LadderView>(viewsBySymbol.values())) {
                            view.onMarketDataEvent(marketDataEvent);
                        }
                    } else {
                        MarketDataForSymbol marketDataForSymbol = marketDataBySymbol.get(symbol);
                        marketDataForSymbol.onMarketDataEvent(marketDataEvent);
                        for (LadderView view : viewsBySymbol.get(symbol)) {
                            view.onMarketDataEvent(marketDataEvent);
                        }
                    }
                }
            }
        };
    }

    public Runnable flushBatchedData() {
        return new Runnable() {
            @Override
            public void run() {
                for (LadderView ladderView : viewBySocket.values()) {
                    ladderView.flush();
                }
            }
        };
    }


    public interface View {
        public void draw(int levels);
        public void trading(boolean tradingEnabled, Collection<String> orderTypesLeft, Collection<String> orderTypesRight);
    }
}

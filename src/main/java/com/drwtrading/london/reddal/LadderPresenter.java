package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.fastui.UiPipeImpl;
import com.drwtrading.london.photons.indy.EquityIdAndSymbol;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.reddal.data.ExtraDataForSymbol;
import com.drwtrading.london.reddal.data.LadderPrefsForSymbolUser;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.marketdata.service.util.MarketDataEventUtil;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.ladder.LastTrade;
import com.drwtrading.photons.mrphil.Position;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class LadderPresenter {

    private final Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer;
    private final LadderOptions ladderOptions;
    private final Publisher<StatsMsg> statsPublisher;
    private final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher;

    Multimap<String, LadderView> viewsBySymbol = HashMultimap.create();
    Multimap<String, LadderView> viewsByUser = HashMultimap.create();
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
    Map<String, Map<String, LadderPrefsForSymbolUser>> ladderPrefsForUserBySymbol;

    Set<EquityIdAndSymbol> equityIdAndSymbolSet = newFastSet();

    public LadderPresenter(Publisher<Main.RemoteOrderCommandToServer> remoteOrderCommandByServer, LadderOptions ladderOptions, Publisher<StatsMsg> statsPublisher, final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher) {
        this.remoteOrderCommandByServer = remoteOrderCommandByServer;
        this.ladderOptions = ladderOptions;
        this.statsPublisher = statsPublisher;
        this.storeLadderPrefPublisher = storeLadderPrefPublisher;
        ladderPrefsForUserBySymbol = new MapMaker().makeComputingMap(new Function<String, Map<String, LadderPrefsForSymbolUser>>() {
            @Override
            public Map<String, LadderPrefsForSymbolUser> apply(final String symbol) {
                return new MapMaker().makeComputingMap(new Function<String, LadderPrefsForSymbolUser>() {
                    @Override
                    public LadderPrefsForSymbolUser apply(String user) {
                        return new LadderPrefsForSymbolUser(symbol, user, storeLadderPrefPublisher);
                    }
                });
            }
        });
    }

    @Subscribe
    public void onConnected(WebSocketConnected connected) {
        UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        View view = new WebSocketOutputDispatcher<View>(View.class).wrap(uiPipe.evalPublisher());
        LadderView ladderView = new LadderView(connected.getClient(), uiPipe, view, remoteOrderCommandByServer, ladderOptions, statsPublisher);
        viewBySocket.put(connected.getOutboundChannel(), ladderView);
        viewsByUser.put(connected.getClient().getUserName(), ladderView);
    }

    @Subscribe
    public void onDisconnected(WebSocketDisconnected disconnected) {
        LadderView view = viewBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            viewsBySymbol.remove(view.symbol, view);
            viewsByUser.remove(disconnected.getClient().getUserName(), view);
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
            view.subscribeToSymbol(symbol, levels, marketDataBySymbol.get(symbol), ordersBySymbol.get(symbol), dataBySymbol.get(symbol), ladderPrefsForUserBySymbol.get(symbol).get(msg.getClient().getUserName()));
            viewsBySymbol.put(symbol, view);
        } else {
            view.onInbound(args);
        }
    }

    // Data

    @Subscribe
    public void on(Main.WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.value.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
    }

    @Subscribe
    public void on(LaserLine laserLine) {
        ExtraDataForSymbol data = dataBySymbol.get(laserLine.getSymbol());
        data.onLaserLine(laserLine);
    }

    @Subscribe
    public void on(DeskPosition deskPosition) {
        ExtraDataForSymbol data = dataBySymbol.get(deskPosition.getSymbol());
        data.onDeskPosition(deskPosition);
    }

    @Subscribe
    public void on(InfoOnLadder infoOnLadder) {
        ExtraDataForSymbol data = dataBySymbol.get(infoOnLadder.getSymbol());
        data.onInfoOnLadder(infoOnLadder);
    }

    @Subscribe
    public void on(LadderText ladderText) {
        ExtraDataForSymbol data = dataBySymbol.get(ladderText.getSymbol());
        data.onLadderText(ladderText);
    }

    @Subscribe
    public void on(LastTrade lastTrade) {
        ExtraDataForSymbol data = dataBySymbol.get(lastTrade.getSymbol());
        data.onLastTrade(lastTrade);
    }

    @Subscribe
    public void on(Position position) {
        ExtraDataForSymbol data = dataBySymbol.get(position.getSymbol());
        data.onDayPosition(position);

    }

    @Subscribe
    public void on(TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        for (WorkingOrdersForSymbol workingOrdersForSymbol : ordersBySymbol.values()) {
            workingOrdersForSymbol.onTradingStatus(serverTradingStatus);
        }

    }

    @Subscribe
    public void on(LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
        ladderPrefsForUserBySymbol.get(pref.symbol).get(pref.user).on(ladderPrefLoaded);
    }

    @Subscribe
    public void on(EquityIdAndSymbol equityIdAndSymbol) {
        if(!equityIdAndSymbol.isPrimary()) {
            return;
        }
        for (MarketDataForSymbol marketDataForSymbol : marketDataBySymbol.values()) {
            marketDataForSymbol.onEquityIdAndSymbol(equityIdAndSymbol);
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
                    } else {
                        MarketDataForSymbol marketDataForSymbol = marketDataBySymbol.get(symbol);
                        marketDataForSymbol.onMarketDataEvent(marketDataEvent);
                        if(marketDataForSymbol.isin != null) {
                            for (EquityIdAndSymbol equityIdAndSymbol : equityIdAndSymbolSet) {
                                marketDataForSymbol.onEquityIdAndSymbol(equityIdAndSymbol);
                            }
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

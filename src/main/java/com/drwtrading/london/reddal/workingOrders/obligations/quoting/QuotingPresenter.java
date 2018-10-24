package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

public class QuotingPresenter {
//    implements UiManager.UiCallbacks {
//
//    private static final String UI_TITLE = "Quoting Controls.";
//
//    private final UiManager uiManager;
//    private final FastUiOutbound allUI;
//
//    private final Map<String, ETFListing> listings = new HashMap<>();
//    private final Map<String, SymbolOnServer> symbolToServer = new HashMap<>();
//    private final Set<String> knownSymbols = new HashSet<>();
//    private final Map<String, StrategyState> strategyStateByKey = new HashMap<>();
//
//    private final Map<String, String> lastDescriptionByKey = new HashMap<>();
//
//    private final Set<String> wasOnKeys = new HashSet<>();
//    private final Set<String> updatedSymbols;
//
//    private boolean updateAll;
//
//    public QuotingPresenter() {
//
//        this.uiManager = new UiManager(this);
//        this.allUI = uiManager.ui;
//
//        this.updatedSymbols = new HashSet<>();
//        this.updateAll = false;
//    }
//
//    private void enableStaticButtons() {
//
//        uiManager.clickable("everythingOn", (id, data, user) -> {
//            for (final String symbol : symbolToServer.keySet()) {
//                sendToSymbolServer(symbol, new StrategyCommand(symbol, StrategySide.BUY, StrategyType.QUOTING, Command.REPLENISH));
//                sendToSymbolServer(symbol, new StrategyCommand(symbol, StrategySide.BUY, StrategyType.QUOTING, Command.START));
//            }
//        });
//
//        uiManager.clickable("everythingOff", (id, data, user) -> {
//            for (final String symbol : symbolToServer.keySet()) {
//                sendToSymbolServer(symbol, new StrategyCommand(symbol, StrategySide.BUY, StrategyType.QUOTING, Command.STOP));
//            }
//        });
//    }
//
//    public void setTradableListing(final ETFListing listing) {
//
//        if (null == listings.put(listing.getExecutionSymbol(), listing)) {
//
//            final SymbolOnServer symbolOnServer = symbolToServer.get(listing.getExecutionSymbol());
//            if (null != symbolOnServer && symbolOnServer.strategyTypes.contains(StrategyType.QUOTING) &&
//                    knownSymbols.add(listing.getExecutionSymbol())) {
//                addStrategy(listing, null);
//            }
//        }
//    }
//
//    public void setStrategyState(final StrategyState state) {
//
//        if (StrategyType.QUOTING == state.getStrategy()) {
//            strategyStateByKey.put(state.getSymbol(), state);
//            if (state.getState() == State.ON) {
//                wasOnKeys.add(state.getSymbol());
//            }
//            updatedSymbols.add(state.getSymbol());
//        }
//    }
//
//    public void setSymbolOnServer(final SymbolOnServer symbolOnServer) {
//        if (symbolOnServer.strategyTypes.contains(StrategyType.QUOTING)) {
//            final SymbolOnServer existing = symbolToServer.get(symbolOnServer.symbol);
//            if (existing == null) {
//                symbolToServer.put(symbolOnServer.symbol, symbolOnServer);
//                if (listings.containsKey(symbolOnServer.symbol) && knownSymbols.add(symbolOnServer.symbol)) {
//                    addStrategy(listings.get(symbolOnServer.symbol), null);
//                }
//            } else if (!existing.tradingServer.equals(symbolOnServer.tradingServer)) {
//                throw new IllegalStateException(
//                        "Symbol " + symbolOnServer.symbol + " on duplicate servers: " + symbolOnServer.tradingServer + ", " +
//                                existing.tradingServer);
//            }
//        }
//    }
//
//    public void heartbeatMissed(final SelectaHandler.HeartbeatMissed heartbeatMissed) {
//
//        final Iterator<Map.Entry<String, SymbolOnServer>> iterator = symbolToServer.entrySet().iterator();
//
//        while (iterator.hasNext()) {
//
//            final Map.Entry<String, SymbolOnServer> entry = iterator.next();
//
//            if (entry.getValue().tradingServer.equals(heartbeatMissed.server)) {
//                if (listings.containsKey(entry.getKey())) {
//                    clearSymbol(entry.getKey());
//                    knownSymbols.remove(entry.getKey());
//                }
//                iterator.remove();
//            }
//        }
//    }
//
//    private void clearSymbol(final String symbol) {
//
//        for (final Iterator<StrategyState> iterator = strategyStateByKey.values().iterator(); iterator.hasNext(); ) {
//            final StrategyState state = iterator.next();
//            if (state.getSymbol().equals(symbol)) {
//                iterator.remove();
//            }
//        }
//        final ETFListing listing = listings.get(symbol);
//        final String key = listing.getKey();
//        uiManager.view.deleteRow(key);
//    }
//
//    @Override
//    public void onUserConnected(final UIConnectedUser user) {
//
//        user.view.setName(UI_TITLE);
//        for (final ETFListing tradableListing : listings.values()) {
//            addStrategy(tradableListing, user);
//        }
//        enableStaticButtons();
//    }
//
//    @Override
//    public void onUserDisconnected(final UIConnectedUser user) {
//    }
//
//    @Override
//    public void onUserHeartbeat(final UIConnectedUser user) {
//        final UserHeartbeatFromSelecta heartbeat = new UserHeartbeatFromSelecta(user.username);
//
//        final Set<String> servers = new HashSet<>();
//        for (final SymbolOnServer symbol : symbolToServer.values()) {
//
//            if (servers.add(symbol.tradingServer)) {
//                selectaMessageForNibbler.publish(new SelectaMessageForNibbler(symbol.tradingServer, heartbeat));
//            }
//        }
//    }
//
//    @Override
//    public void onUserFiltersUpdated(final UIConnectedUser user) {
//        user.updateFilters();
//    }
//
//    @Subscribe
//    public void on(final WebSocketConnected webSocketConnected) {
//        uiManager.on(webSocketConnected);
//        this.updateAll = true;
//    }
//
//    @Subscribe
//    public void on(final WebSocketDisconnected disconnected) {
//        uiManager.on(disconnected);
//    }
//
//    @Subscribe
//    public void on(final WebSocketInboundData inboundData) {
//        uiManager.on(inboundData);
//    }
//
//    public Runnable heartbeatRunnable() {
//        return () -> allUI.eval("heartbeat()");
//    }
//
//    public Runnable flushRunnable() {
//        return () -> {
//            updateStrategiesForAll();
//            allUI.flush();
//        };
//    }
//
//    private void addStrategy(final ETFListing listing, final UIConnectedUser userName) {
//
//        final String symbol = listing.getExecutionSymbol();
//
//        final String key = listing.getKey();
//
//        final SymbolOnServer symbolOnServer = symbolToServer.get(symbol);
//
//        if (null != symbolOnServer) {
//
//            final FastUiOutbound ui = userName == null ? allUI : userName.ui;
//            final UIView view = userName == null ? uiManager.view : userName.view;
//
//            view.addRow(key);
//            ui.txt(key + "_symbol", symbol);
//            uiManager.clickable(key + "_symbol", (id, data, user1) -> user1.view.launchLadder(symbol));
//            ui.txt(key + "_nibblerName", symbolOnServer.tradingServer);
//            ui.txt(key + "_quote_description", "-- hit f5 --");
//
//            final StrategyCommand startCmd = new StrategyCommand(symbol, StrategySide.BUY, StrategyType.QUOTING, Command.START);
//            final StrategyCommand stopCmd = new StrategyCommand(symbol, StrategySide.BUY, StrategyType.QUOTING, Command.STOP);
//
//            uiManager.clickable(key + "_ON", (id, data, user) -> sendToSymbolServer(symbol, startCmd));
//            uiManager.clickable(key + "_OFF", (id, data, user) -> {
//                sendToSymbolServer(symbol, stopCmd);
//                wasOnKeys.remove(symbol);
//                updatedSymbols.add(symbol);
//            });
//        }
//    }
//
//    private void sendToSymbolServer(final String symbol, final FromSelectaMessage message) {
//
//        final SymbolOnServer tradingServer = symbolToServer.get(symbol);
//        if (null == tradingServer) {
//            throw new IllegalStateException("Missing trading server for symbol: " + symbol);
//        } else {
//            selectaMessageForNibbler.publish(new SelectaMessageForNibbler(tradingServer.tradingServer, message));
//        }
//    }
//
//    private void updateStrategiesForAll() {
//
//        for (final String symbol : symbolToServer.keySet()) {
//
//            final ETFListing listing = listings.get(symbol);
//
//            if (null != listing && (updateAll || updatedSymbols.contains(symbol))) {
//
//                final String key = listing.getKey();
//
//                final String strategyKey = key + "_onOffControls";
//                final StrategyState state = strategyStateByKey.get(symbol);
//
//                if (state != null) {
//                    allUI.cls(strategyKey, "on", state.getState() != State.OFF);
//                    allUI.cls(strategyKey, "off", state.getState() == State.OFF);
//                } else {
//                    allUI.cls(strategyKey, "on", false);
//                    allUI.cls(strategyKey, "off", true);
//                }
//                allUI.cls(strategyKey, "wasOn", wasOnKeys.contains(symbol));
//
//                final StrategyState strategyState = strategyStateByKey.get(symbol);
//
//                final String description;
//                if (null == strategyState) {
//                    description = "--";
//                } else {
//                    description = strategyState.getState() + "(" + strategyState.getDescription() + ')';
//                }
//
//                final String lastDescription = lastDescriptionByKey.get(listing.getKey());
//
//                if (!description.equals(lastDescription)) {
//                    allUI.txt(listing.getKey() + "_quote_description", description);
//                    lastDescriptionByKey.put(listing.getKey() + "_description", description);
//                }
//            }
//        }
//        updatedSymbols.clear();
//        updateAll = false;
//    }
}

package com.drwtrading.london.reddal.data;

import com.drwtrading.eeif.md.utils.MarketDataEventUtil;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.book.impl.levelTwo.LevelTwoBook;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.BasicTickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.SingleBandTickTable;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.Exchange;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.prices.PriceFormat;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.protocols.photon.marketdata.AuctionIndicativePrice;
import com.drwtrading.london.protocols.photon.marketdata.AuctionIndicativeSurplus;
import com.drwtrading.london.protocols.photon.marketdata.AuctionTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BasisTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BestPrice;
import com.drwtrading.london.protocols.photon.marketdata.BlockTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BookConsistencyMarker;
import com.drwtrading.london.protocols.photon.marketdata.BookSnapshot;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.CmeDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.CmeFractionalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.DecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.Level;
import com.drwtrading.london.protocols.photon.marketdata.LiffeThirtySecondsTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketStateEvent;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedBandedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.PriceType;
import com.drwtrading.london.protocols.photon.marketdata.PriceUpdate;
import com.drwtrading.london.protocols.photon.marketdata.ProductBookStateEvent;
import com.drwtrading.london.protocols.photon.marketdata.ProductReset;
import com.drwtrading.london.protocols.photon.marketdata.RequestForCross;
import com.drwtrading.london.protocols.photon.marketdata.RequestForQuote;
import com.drwtrading.london.protocols.photon.marketdata.ServerHeartbeat;
import com.drwtrading.london.protocols.photon.marketdata.SettlementDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TickBand;
import com.drwtrading.london.protocols.photon.marketdata.TickStructure;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolume;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;
import com.drwtrading.london.reddal.util.Mathematics;
import com.drwtrading.london.reddal.util.PriceOperations;
import com.drwtrading.london.reddal.util.PriceUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class MarketDataForSymbol implements IMarketData {

    private static final Set<String> SUPERFEED_EXCHANGES;

    static {
        SUPERFEED_EXCHANGES = new HashSet<>();
        SUPERFEED_EXCHANGES.add("SUPERFEED");
        SUPERFEED_EXCHANGES.add("MEFF");
        SUPERFEED_EXCHANGES.add("IDEM");
        SUPERFEED_EXCHANGES.add("OMX");
    }

    private final String symbol;
    private final boolean isPriceInverted;

    private final Queue<MarketDataEvent> queuedMD;

    private LevelTwoBook book;

    private PriceOperations priceOperations;
    private PriceFormat priceFormat;

    public final TradeTracker tradeTracker;

    private TopOfBook lastImpliedMsg;

    public MarketDataForSymbol(final String symbol) {

        this.symbol = symbol;
        this.isPriceInverted = symbol.startsWith("6R");
        this.queuedMD = new LinkedList<>();

        this.tradeTracker = new TradeTracker();
    }

    @Override
    public void subscribeForMD() {
        // no-op
    }

    @Override
    public void unsubscribeForMD() {
        // no-op
    }

    @Override
    public PriceOperations getPriceOperations() {
        return priceOperations;
    }

    @Override
    public boolean isPriceInverted() {
        return isPriceInverted;
    }

    @Override
    public IBook<?> getBook() {
        return book;
    }

    @Override
    public TradeTracker getTradeTracker() {
        return tradeTracker;
    }

    @Override
    public String formatPrice(final long price) {
        return priceFormat.format(price);
    }

    public void onMarketDataBatch(final List<MarketDataEvent> msg) {

        for (final MarketDataEvent marketDataEvent : msg) {
            onMarketDataEvent(marketDataEvent);
        }
    }

    public void onMarketDataEvent(final MarketDataEvent e) {

        if (e instanceof InstrumentDefinitionEvent) {

            final String eventSymbol = MarketDataEventUtil.getSymbol(e);
            if (symbol.equals(eventSymbol)) {

                e.accept(visitor);
                while (!queuedMD.isEmpty()) {
                    final MarketDataEvent event = queuedMD.poll();
                    event.accept(visitor);
                }
            }
        } else if (null == book) {
            queuedMD.add(e);
        } else {
            final String eventSymbol = MarketDataEventUtil.getSymbol(e);
            if (symbol.equals(eventSymbol)) {
                e.accept(visitor);
            }
        }
    }

    public MarketDataEvent.Visitor<Void> visitor = new MarketDataEvent.Visitor<Void>() {

        private final Queue<PriceUpdate> priceUpdates = new LinkedList<>();

        @Override
        public Void visitTradeUpdate(final TradeUpdate msg) {
            tradeTracker.addTrade(msg);
            return null;
        }

        @Override
        public Void visitTotalTradedVolumeByPrice(final TotalTradedVolumeByPrice msg) {
            tradeTracker.setTotalTraded(msg);
            return null;
        }

        @Override
        public Void visitInstrumentDefinitionEvent(final InstrumentDefinitionEvent msg) {

            final InstrumentID instID = getInstrumentID(msg);
            final InstType instType = getInstType(msg);
            final ITickTable tickTable = getTickTable(msg);
            final MDSource mdSource = getSource(msg);
            final double wpv = Mathematics.getPointValue(instType, symbol, tickTable);

            book = new LevelTwoBook(BookLevelTwoViewerAdaptor.INSTANCE, symbol, -1, instID, instType, tickTable, mdSource, wpv);

            priceOperations = PriceUtils.from(msg);
            priceFormat = PriceFormats.from(msg.getPriceStructure().getTickStructure());

            if (msg.getPriceStructure().getTickStructure() instanceof NormalizedDecimalTickStructure) {
                final NormalizedBandedDecimalTickStructure bandedDecimalTickStructure = new NormalizedBandedDecimalTickStructure(
                        (NormalizedDecimalTickStructure) msg.getPriceStructure().getTickStructure(),
                        new ObjectArrayList<>(new TickBand[]{new TickBand(0, msg.getPriceStructure().getTickIncrement())}));
                priceFormat = PriceFormats.from(bandedDecimalTickStructure);
            }
            return null;
        }

        @Override
        public Void visitSettlementDataEvent(final SettlementDataEvent msg) {
            switch (msg.getSettlementType()) {
                case YESTERDAY: {
                    book.referencePrice(ReferencePoint.YESTERDAY_CLOSE, msg.getSettlementPrice(), 0L);
                    break;
                }
                case MARKET_CLOSE: {
                    book.referencePrice(ReferencePoint.TODAY_CLOSE, msg.getSettlementPrice(), 0L);
                    break;
                }
            }
            return null;
        }

        @Override
        public Void visitProductBookStateEvent(final ProductBookStateEvent msg) {

            switch (msg.getState()) {
                case AUCTION: {
                    book.setStatus(BookMarketState.AUCTION);
                    book.invalidateReferencePrice(ReferencePoint.AUCTION_SUMMARY);
                    break;
                }
                default: {
                    book.setStatus(BookMarketState.CONTINUOUS);
                    break;
                }
            }
            return null;
        }

        @Override
        public Void visitAuctionIndicativePrice(final AuctionIndicativePrice msg) {
            book.referencePrice(ReferencePoint.AUCTION_INDICATIVE, msg.getIndicativePrice(), msg.getQuantity());
            return null;
        }

        @Override
        public Void visitAuctionTradeUpdate(final AuctionTradeUpdate msg) {
            book.referencePrice(ReferencePoint.AUCTION_SUMMARY, msg.getPrice(), msg.getQuantity());
            return null;
        }

        @Override
        public Void visitProductReset(final ProductReset msg) {
            book.setStatus(BookMarketState.CLOSED);
            book.clearBook();
            book.invalidateReferencePrice(ReferencePoint.AUCTION_SUMMARY);
            book.invalidateReferencePrice(ReferencePoint.AUCTION_SUMMARY);
            return null;
        }

        @Override
        public Void visitTopOfBook(final TopOfBook msg) {

            if (msg.getType() == PriceType.IMPLIED) {

                if (null != lastImpliedMsg) {

                    final BestPrice bid = lastImpliedMsg.getBestBid();
                    if (bid.isExists()) {
                        book.setImpliedQty(BookSide.BID, bid.getPrice(), 0);
                    }

                    final BestPrice ask = lastImpliedMsg.getBestOffer();
                    if (ask.isExists()) {
                        book.setImpliedQty(BookSide.ASK, ask.getPrice(), 0);
                    }
                }

                final BestPrice bid = msg.getBestBid();
                if (bid.isExists()) {
                    book.setImpliedQty(BookSide.BID, bid.getPrice(), bid.getQuantity());
                }

                final BestPrice ask = msg.getBestOffer();
                if (ask.isExists()) {
                    book.setImpliedQty(BookSide.ASK, ask.getPrice(), ask.getQuantity());
                }
                lastImpliedMsg = msg;
            }
            return null;
        }

        @Override
        public Void visitBookSnapshot(final BookSnapshot msg) {

            if (msg.getType() == PriceType.DIRECT) {
                final BookSide side = msg.getSide() == Side.BID ? BookSide.BID : BookSide.ASK;
                for (final Level level : msg.getLevels()) {
                    book.setLevel(side, level.getPrice(), level.getQuantity());
                }
            }
            return null;
        }

        @Override
        public Void visitPriceUpdate(final PriceUpdate msg) {

            if (msg.getType() == PriceType.DIRECT) {
                priceUpdates.add(msg);
            }
            return null;
        }

        @Override
        public Void visitBookConsistencyMarker(final BookConsistencyMarker msg) {

            while (!priceUpdates.isEmpty()) {
                final PriceUpdate priceUpdate = priceUpdates.remove();
                final BookSide side = priceUpdate.getSide() == Side.BID ? BookSide.BID : BookSide.ASK;
                final long price = priceUpdate.getPrice();
                final long qty = priceUpdate.getQuantity();
                if (0 < qty) {
                    book.setLevel(side, price, qty);
                } else {
                    book.deleteLevel(side, price);
                }
            }
            return null;
        }

        @Override
        public Void visitMarketStateEvent(final MarketStateEvent msg) {
            return null;
        }

        @Override
        public Void visitTotalTradedVolume(final TotalTradedVolume msg) {
            return null;
        }

        @Override
        public Void visitAuctionIndicativeSurplus(final AuctionIndicativeSurplus msg) {
            return null;
        }

        @Override
        public Void visitBlockTradeUpdate(final BlockTradeUpdate msg) {
            return null;
        }

        @Override
        public Void visitBasisTradeUpdate(final BasisTradeUpdate msg) {
            return null;
        }

        @Override
        public Void visitRequestForQuote(final RequestForQuote msg) {
            return null;
        }

        @Override
        public Void visitRequestForCross(final RequestForCross msg) {
            return null;
        }

        @Override
        public Void visitServerHeartbeat(final ServerHeartbeat msg) {
            return null;
        }
    };

    private static InstrumentID getInstrumentID(final InstrumentDefinitionEvent refData) {
        if (refData.getInstrumentStructure() instanceof CashOutrightStructure) {
            final CashOutrightStructure cash = (CashOutrightStructure) refData.getInstrumentStructure();
            if ("CXE".equals(refData.getExchange())) {
                return new InstrumentID(cash.getIsin(), CCY.getCCY(refData.getPriceStructure().getCurrency().name()), MIC.CHIX);
            } else if ("BXE".equals(refData.getExchange())) {
                return new InstrumentID(cash.getIsin(), CCY.getCCY(refData.getPriceStructure().getCurrency().name()), MIC.BATE);
            } else if (cash.getIsin().length() < 12) {
                // SPREADS
                final String paddedISIN = cash.getIsin() + "000000000000".substring(cash.getIsin().length());
                return new InstrumentID(paddedISIN, CCY.getCCY(refData.getPriceStructure().getCurrency().name()),
                        MIC.getMIC(cash.getMic()));
            } else {
                return new InstrumentID(cash.getIsin(), CCY.getCCY(refData.getPriceStructure().getCurrency().name()),
                        MIC.getMIC(cash.getMic()));
            }
        } else {
            MIC myMic = null;
            final Exchange exchange = Exchange.get(refData.getExchange());
            for (final MIC mic : MIC.values()) {
                if (mic.exchange == exchange) {
                    myMic = mic;
                }
            }
            String isin = refData.getSymbol();
            if (isin.length() < 12) {
                isin = isin + "000000000000".substring(isin.length());
            }
            if (isin.length() > 12) {
                isin = isin.substring(0, 12);
            }
            if (myMic == null) {
                throw new IllegalArgumentException("Cannot find MIC for " + refData);
            } else {
                return new InstrumentID(isin, CCY.getCCY(refData.getPriceStructure().getCurrency().name()), myMic);
            }
        }
    }

    private static InstType getInstType(final InstrumentDefinitionEvent def) {

        switch (def.getInstrumentStructure().typeEnum()) {
            case CASH_OUTRIGHT_STRUCTURE: {
                return InstType.EQUITY;
            }
            case FUTURE_OUTRIGHT_STRUCTURE: {
                return InstType.FUTURE;
            }
            case FUTURE_STRATEGY_STRUCTURE: {
                return InstType.FUTURE_SPREAD;
            }
            case FOREX_PAIR_STRUCTURE: {
                return InstType.FX;
            }
            default: {
                throw new IllegalArgumentException(
                        "Instrument Structure [" + def.getInstrumentStructure().typeEnum() + "] is not supported.");
            }
        }
    }

    private static MDSource getSource(final InstrumentDefinitionEvent def) {

        if ("CXE".equals(def.getExchange()) || "BXE".equals(def.getExchange())) {
            return MDSource.BATS_EUROPE;
        } else if ("LIFFE".equals(def.getExchange()) || "LIFFE-XDP".equals(def.getExchange())) {
            return MDSource.LIFFE;
        } else if ("EUREX".equals(def.getExchange().toUpperCase())) {
            return MDSource.EUREX;
        } else if ("EURONEXT".equals(def.getExchange().toUpperCase())) {
            return MDSource.EURONEXT;
        } else if (SUPERFEED_EXCHANGES.contains(def.getExchange().toUpperCase())) {
            return MDSource.SUPERFEED;
        } else if ("CME".equals(def.getExchange().toUpperCase())) {
            return MDSource.CME;
        } else if ("XETRA".equals(def.getExchange().toUpperCase())) {
            return MDSource.XETRA;
        } else if ("LSE".equals(def.getExchange().toUpperCase())) {
            return MDSource.LSE;
        } else if ("MICEX".equals(def.getExchange().toUpperCase())) {
            return MDSource.MICEX;
        } else if ("FORTS".equals(def.getExchange().toUpperCase())) {
            return MDSource.FORTS;
        } else {
            final MDSource source = MDSource.get(def.getExchange().toUpperCase());
            if (source == null) {
                System.out.println("Cannot find MDSource for " + def);
            }
            return source;
        }
    }

    private static ITickTable getTickTable(final InstrumentDefinitionEvent def) {
        return def.getPriceStructure().getTickStructure().accept(new TickStructure.Visitor<ITickTable>() {
            @Override
            public ITickTable visitNormalizedDecimalTickStructure(final NormalizedDecimalTickStructure msg) {
                return new SingleBandTickTable(def.getPriceStructure().getTickIncrement());
            }

            @Override
            public ITickTable visitNormalizedBandedDecimalTickStructure(final NormalizedBandedDecimalTickStructure msg) {
                final NavigableMap<Long, Long> tickLevels = new TreeMap<>();
                for (final TickBand band : msg.getBands()) {
                    tickLevels.put(band.getMinPrice(), band.getTickSize());
                }
                return new BasicTickTable(tickLevels);
            }

            @Override
            public ITickTable visitDecimalTickStructure(final DecimalTickStructure msg) {
                if (msg.getPointPosition() == PriceFormats.NORMAL_POINT_POSITION) {
                    return new SingleBandTickTable(def.getPriceStructure().getTickIncrement());
                }
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitCmeDecimalTickStructure(final CmeDecimalTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitCmeFractionalTickStructure(final CmeFractionalTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitLiffeThirtySecondsTickStructure(final LiffeThirtySecondsTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }
        });
    }
}

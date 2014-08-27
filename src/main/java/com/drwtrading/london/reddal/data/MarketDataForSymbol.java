package com.drwtrading.london.reddal.data;

import com.drwtrading.frontoffice.book.treemap.TreeMapBookFactory;
import com.drwtrading.london.prices.PriceFormat;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.protocols.photon.marketdata.*;
import com.drwtrading.london.reddal.util.PriceOperations;
import com.drwtrading.london.reddal.util.PriceUtils;
import com.drwtrading.marketdata.service.util.MarketDataEventUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetlang.core.Callback;

import java.util.HashMap;
import java.util.Map;

public class MarketDataForSymbol {

    public final String symbol;

    public InstrumentDefinitionEvent refData = null;
    public TopOfBook topOfBook = null;
    public Book book = null;
    public Map<Long, TotalTradedVolumeByPrice> totalTradedVolumeByPrice = new HashMap<Long, TotalTradedVolumeByPrice>();
    public TradeUpdate lastTrade;
    public AuctionIndicativePrice auctionIndicativePrice;
    public AuctionTradeUpdate auctionTradeUpdate;
    public ProductBookStateEvent bookState;
    public SettlementDataEvent settle;
    public PriceFormat priceFormat;
    public TradeTracker tradeTracker = new TradeTracker();
    public TotalTradedVolume totalTradedVolume;
    public String isin;
    public PriceOperations priceOperations;
    public PriceType preferredPriceType = PriceType.RECONSTRUCTED;

    public MarketDataEvent.Visitor<Void> visitor = new MarketDataEvent.Visitor<Void>() {


        @Override
        public Void visitProductReset(ProductReset msg) {
            topOfBook = null;
            auctionIndicativePrice = null;
            auctionTradeUpdate = null;
            bookState = null;
            book.clear();
            return null;
        }

        @Override
        public Void visitTopOfBook(TopOfBook msg) {
            if (msg.getType() == preferredPriceType) {
                topOfBook = msg;
            }
            return null;
        }

        @Override
        public Void visitBookSnapshot(BookSnapshot msg) {
            return null;
        }

        @Override
        public Void visitPriceUpdate(PriceUpdate msg) {
            return null;
        }

        @Override
        public Void visitBookConsistencyMarker(BookConsistencyMarker msg) {
            return null;
        }

        @Override
        public Void visitBlockTradeUpdate(BlockTradeUpdate msg) {
            return null;
        }

        @Override
        public Void visitBasisTradeUpdate(BasisTradeUpdate msg) {
            return null;
        }

        @Override
        public Void visitTradeUpdate(TradeUpdate msg) {
            lastTrade = msg;
            tradeTracker.onTradeUpdate(msg);
            return null;
        }

        @Override
        public Void visitTotalTradedVolumeByPrice(TotalTradedVolumeByPrice msg) {
            totalTradedVolumeByPrice.put(msg.getPrice(), msg);
            return null;
        }

        @Override
        public Void visitTotalTradedVolume(TotalTradedVolume msg) {
            totalTradedVolume = msg;
            return null;
        }


        @Override
        public Void visitInstrumentDefinitionEvent(InstrumentDefinitionEvent msg) {
            refData = msg;
            if (refData.getExchange().equals("SUPERFEED")) {
                preferredPriceType = PriceType.RECONSTRUCTED;
            } else if (refData.getInstrumentStructure() instanceof FutureOutrightStructure || refData.getInstrumentStructure() instanceof FutureStrategyStructure) {
                preferredPriceType = PriceType.RECONSTRUCTED;
            } else {
                preferredPriceType = PriceType.DIRECT;
            }
            book = new Book(symbol, refData.getPriceStructure().getTickIncrement(), new TreeMapBookFactory());
            priceOperations = PriceUtils.from(msg);
            priceFormat = PriceFormats.from(refData.getPriceStructure().getTickStructure());
            if (refData.getInstrumentStructure() instanceof CashOutrightStructure) {
                CashOutrightStructure cashOutrightStructure = (CashOutrightStructure) refData.getInstrumentStructure();
                isin = cashOutrightStructure.getIsin();
            } else if (refData.getPriceStructure().getTickStructure() instanceof NormalizedDecimalTickStructure) {
                NormalizedBandedDecimalTickStructure bandedDecimalTickStructure = new NormalizedBandedDecimalTickStructure(
                        (NormalizedDecimalTickStructure) refData.getPriceStructure().getTickStructure(),
                        new ObjectArrayList<>(new TickBand[]{new TickBand(Long.MIN_VALUE, refData.getPriceStructure().getTickIncrement())})
                );
                priceFormat = PriceFormats.from(bandedDecimalTickStructure);
            }
            return null;
        }

        @Override
        public Void visitSettlementDataEvent(SettlementDataEvent msg) {
            settle = msg;
            return null;
        }

        @Override
        public Void visitMarketStateEvent(MarketStateEvent msg) {
            return null;
        }

        @Override
        public Void visitProductBookStateEvent(ProductBookStateEvent msg) {
            bookState = msg;
            return null;
        }

        @Override
        public Void visitRequestForQuote(RequestForQuote msg) {
            return null;
        }

        @Override
        public Void visitRequestForCross(RequestForCross msg) {
            return null;
        }

        @Override
        public Void visitAuctionIndicativePrice(AuctionIndicativePrice msg) {
            auctionIndicativePrice = msg;
            return null;
        }

        @Override
        public Void visitAuctionIndicativeSurplus(AuctionIndicativeSurplus msg) {
            return null;
        }

        @Override
        public Void visitAuctionTradeUpdate(AuctionTradeUpdate msg) {
            auctionTradeUpdate = msg;
            return null;
        }

        @Override
        public Void visitServerHeartbeat(ServerHeartbeat msg) {
            return null;
        }
    };

    public MarketDataForSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void onMarketDataEvent(MarketDataEvent e) {
        String eventSymbol = MarketDataEventUtil.getSymbol(e);
        if (eventSymbol == null || eventSymbol.equals(symbol)) {
            if (book != null) {
                // Only DIRECT full book is supported
                if (e instanceof PriceUpdate) {
                    if (((PriceUpdate) e).getType() == preferredPriceType) {
                        book.apply(e);
                    }
                } else if (e instanceof BookSnapshot) {
                    if (((BookSnapshot) e).getType() == preferredPriceType) {
                        book.apply(e);
                    }
                } else {
                    book.apply(e);
                }
            }
            e.accept(visitor);
        }
    }

    public Callback<MarketDataEvent> onMarketDataEventCallback() {
        return new Callback<MarketDataEvent>() {
            @Override
            public void onMessage(MarketDataEvent message) {
                onMarketDataEvent(message);
            }
        };
    }
}

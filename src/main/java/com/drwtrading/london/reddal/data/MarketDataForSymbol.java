package com.drwtrading.london.reddal.data;

import com.drwtrading.frontoffice.book.treemap.TreeMapBookFactory;
import com.drwtrading.london.prices.PriceFormat;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.prices.tickbands.TickSizeTracker;
import com.drwtrading.london.protocols.photon.marketdata.AuctionIndicativePrice;
import com.drwtrading.london.protocols.photon.marketdata.AuctionTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedBandedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.ProductBookStateEvent;
import com.drwtrading.london.protocols.photon.marketdata.SettlementDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.TickBand;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolume;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.marketdata.service.util.MarketDataEventUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MarketDataForSymbol {

    public final String symbol;

    public InstrumentDefinitionEvent refData = null;
    public TopOfBook topOfBook = null;
    public Book book = null;
    public Map<Long, TotalTradedVolumeByPrice> totalTradedVolumeByPrice = new HashMap<Long, TotalTradedVolumeByPrice>();
    public TickSizeTracker tickSizeTracker = null;
    public TradeUpdate lastTrade;
    public AuctionIndicativePrice auctionIndicativePrice;
    public AuctionTradeUpdate auctionTradeUpdate;
    public ProductBookStateEvent bookState;
    public SettlementDataEvent settle;
    public PriceFormat priceFormat;
    public TradeTracker tradeTracker = new TradeTracker();
    public ObjectList<TickBand> tickBands;
    public TotalTradedVolume totalTradedVolume;

    public MarketDataForSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void onMarketDataEvent(MarketDataEvent e) {
        String eventSymbol = MarketDataEventUtil.getSymbol(e);
        if (eventSymbol == null || eventSymbol.equals(symbol)) {
            if (e instanceof InstrumentDefinitionEvent) {
                refData = (InstrumentDefinitionEvent) e;
                book = new Book(symbol, refData.getPriceStructure().getTickIncrement(), new TreeMapBookFactory());
                if (refData.getPriceStructure().getTickStructure() instanceof NormalizedBandedDecimalTickStructure) {
                    tickBands = ((NormalizedBandedDecimalTickStructure) refData.getPriceStructure().getTickStructure()).getBands();
                    tickSizeTracker = new TickSizeTracker(tickBands);
                } else {
                    tickBands = new ObjectArrayList<TickBand>(Arrays.asList(new TickBand(0, refData.getPriceStructure().getTickIncrement())));
                    tickSizeTracker = new TickSizeTracker(tickBands);
                }
                priceFormat = PriceFormats.from(refData.getPriceStructure().getTickStructure());
            } else if (e instanceof TopOfBook) {
                topOfBook = (TopOfBook) e;
            } else if (e instanceof TotalTradedVolumeByPrice) {
                totalTradedVolumeByPrice.put(((TotalTradedVolumeByPrice) e).getPrice(), (TotalTradedVolumeByPrice) e);
            } else if (e instanceof TradeUpdate) {
                lastTrade = (TradeUpdate) e;
                tradeTracker.onTradeUpdate((TradeUpdate) e);
            } else if (e instanceof AuctionIndicativePrice) {
                auctionIndicativePrice = (AuctionIndicativePrice) e;
            } else if (e instanceof AuctionTradeUpdate) {
                auctionTradeUpdate = (AuctionTradeUpdate) e;
            } else if (e instanceof ProductBookStateEvent) {
                bookState = (ProductBookStateEvent) e;
            } else if (e instanceof SettlementDataEvent) {
                settle = (SettlementDataEvent) e;
            } else if (e instanceof TotalTradedVolume) {
                totalTradedVolume = (TotalTradedVolume) e;
            }
            if (book != null) {
                book.apply(e);
            }
        }
    }

}

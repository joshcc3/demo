package com.drwtrading.london.reddal.util;

import com.drwtrading.london.protocols.photon.marketdata.BasisTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BlockTradeUpdate;
import com.drwtrading.london.protocols.photon.marketdata.BookConsistencyMarker;
import com.drwtrading.london.protocols.photon.marketdata.BookSnapshot;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.PriceUpdate;
import com.drwtrading.london.protocols.photon.marketdata.ProductReset;
import com.drwtrading.london.protocols.photon.marketdata.RequestForCross;
import com.drwtrading.london.protocols.photon.marketdata.RequestForQuote;
import com.drwtrading.london.protocols.photon.marketdata.SettlementDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolume;
import com.drwtrading.london.protocols.photon.marketdata.TotalTradedVolumeByPrice;
import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;

public class MarketDataEventUtil {
    public static String getSymbol(MarketDataEvent event) {
        switch (event.typeEnum()) {
            case PRODUCT_RESET:
                return ((ProductReset) event).getSymbol();
            case TOP_OF_BOOK:
                return ((TopOfBook) event).getSymbol();
            case BOOK_SNAPSHOT:
                return ((BookSnapshot) event).getSymbol();
            case PRICE_UPDATE:
                return ((PriceUpdate) event).getSymbol();
            case BOOK_CONSISTENCY_MARKER:
                return ((BookConsistencyMarker) event).getSymbol();
            case TRADE_UPDATE:
                return ((TradeUpdate) event).getSymbol();
            case TOTAL_TRADED_VOLUME:
                return ((TotalTradedVolume) event).getSymbol();
            case BLOCK_TRADE_UPDATE:
                return ((BlockTradeUpdate) event).getSymbol();
            case BASIS_TRADE_UPDATE:
                return ((BasisTradeUpdate) event).getSymbol();
            case INSTRUMENT_DEFINITION_EVENT:
                return ((InstrumentDefinitionEvent) event).getSymbol();
            case SETTLEMENT_DATA_EVENT:
                return ((SettlementDataEvent) event).getSymbol();
            case MARKET_STATE_EVENT:
                return null;
            case REQUEST_FOR_QUOTE:
                return ((RequestForQuote) event).getSymbol();
            case REQUEST_FOR_CROSS:
                return ((RequestForCross) event).getSymbol();
            case TOTAL_TRADED_VOLUME_BY_PRICE:
                return ((TotalTradedVolumeByPrice) event).getSymbol();
            case SERVER_HEARTBEAT:
                return null;
            default:
                throw new IllegalArgumentException("Unknown MarketDataEvent type: " + event);
        }
    }
}

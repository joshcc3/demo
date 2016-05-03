package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.ExchangeInstrumentDefinitionDetails;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentStructure;
import org.jetlang.channels.Channel;

import java.util.HashMap;
import java.util.Map;

public class ChixInstMatcher {

    private final Channel<ChixSymbolPair> publisher;

    private final Map<InstrumentID, String> chixSymbols;
    private final Map<InstrumentID, String> primaryExchSymbols;

    public ChixInstMatcher(final Channel<ChixSymbolPair> publisher) {

        this.publisher = publisher;

        this.chixSymbols = new HashMap<>();
        this.primaryExchSymbols = new HashMap<>();
    }

    @Subscribe
    public void on(final InstrumentDefinitionEvent instDefEvent) {

        if (InstrumentStructure.Type.CASH_OUTRIGHT_STRUCTURE == instDefEvent.getInstrumentStructure().typeEnum()) {

            final String symbol = instDefEvent.getSymbol();

            final CashOutrightStructure cashStructure = ((CashOutrightStructure) instDefEvent.getInstrumentStructure());
            final String isin = cashStructure.getIsin();
            final CCY ccy = CCY.getCCY(instDefEvent.getPriceStructure().getCurrency().name());
            final MIC mic = MIC.getMIC(cashStructure.getMic());

            if (null != symbol && 12 == isin.length() && null != ccy && null != mic) {

                final InstrumentID instID = new InstrumentID(isin, ccy, mic);

                if (ExchangeInstrumentDefinitionDetails.Type.BATS_INSTRUMENT_DEFINITION == instDefEvent.getExchangeInstrumentDefinitionDetails().typeEnum()) {

                    chixSymbols.put(instID, symbol);
                    final String primarySymbol = primaryExchSymbols.get(instID);
                    if (null != primarySymbol) {
                        updateChixPair(primarySymbol, symbol);
                    }
                } else {

                    primaryExchSymbols.put(instID, symbol);
                    final String chixSymbol = chixSymbols.get(instID);
                    if (null != chixSymbol) {
                        updateChixPair(symbol, chixSymbol);
                    }
                }
            }
        }
    }

    private void updateChixPair(final String primarySymbol, final String chixSymbol) {

        final ChixSymbolPair chixSymbolPair = new ChixSymbolPair(primarySymbol, chixSymbol);
        publisher.publish(chixSymbolPair);
    }
}

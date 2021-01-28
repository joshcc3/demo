package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.indy.transport.data.Source;
import com.drwtrading.london.reddal.symbols.SymbolIndyData;
import drw.eeif.photons.mrchill.Position;

import java.text.DecimalFormat;

public class InstrumentMetaData {

    public final InstrumentID instrumentID;

    private Source indyDefSource;
    private String indyDefName;

    private long mrChillNetPosition;
    private String formattedMrChillNetPosition;

    private long mrChillVolume;
    private String formattedMrChillVolume;

    public InstrumentMetaData(final InstrumentID instrumentID) {
        this.instrumentID = instrumentID;
    }

    public void setSymbolIndyData(final SymbolIndyData symbolIndyData) {
        this.indyDefName = symbolIndyData.description;
        this.indyDefSource = symbolIndyData.source;
    }

    public Source getIndyDefSource() {
        return indyDefSource;
    }

    public String getIndyDefName() {
        return indyDefName;
    }

    public String getDescription() {
        return getIndyDefName();
    }

    public void setMrPhilPosition(final DecimalFormat formatter, final Position position) {
        final long netPosition = position.getDayBuy() - position.getDaySell();
        final long volume = position.getDayBuy() + position.getDaySell();

        if (null == formattedMrChillNetPosition || mrChillNetPosition != netPosition) {

            this.mrChillNetPosition = netPosition;
            this.formattedMrChillNetPosition = DataUtils.formatPosition(formatter, mrChillNetPosition);

            this.mrChillVolume = volume;
            this.formattedMrChillVolume = DataUtils.formatPosition(formatter, mrChillVolume);
        }
    }

    public long getMrChillNetPosition() {
        return mrChillNetPosition;
    }

    public String getFormattedMrChillNetPosition() {
        return formattedMrChillNetPosition;
    }

    public long getMrChillVolume() {
        return mrChillVolume;
    }

    public String getFormattedMrChillVolume() {
        return formattedMrChillVolume;
    }
}

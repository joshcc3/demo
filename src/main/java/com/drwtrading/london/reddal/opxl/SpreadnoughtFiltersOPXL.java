package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.stacks.family.StackChildFilter;
import com.drwtrading.london.reddal.stacks.family.StackFamilyPresenter;

import java.nio.file.Path;
import java.util.Collection;

public class SpreadnoughtFiltersOPXL extends AFiltersOPXL {

    public static final String FAMILY_NAME = "Spreadnought";
    private static final String TOPIC_PREFIX = "eeif(spreader_datapublish)";

    private final StackFamilyPresenter stackFamilyPresenter;

    public SpreadnoughtFiltersOPXL(final SelectIO opxlSelectIO, final SelectIO callbackSelectIO,
            final IResourceMonitor<ReddalComponents> monitor, final Path logPath, final StackFamilyPresenter stackFamilyPresenter) {

        super(opxlSelectIO, callbackSelectIO, monitor, ReddalComponents.OPXL_SPREAD_STACK_MANAGER_FILTERS, TOPIC_PREFIX, logPath);
        this.stackFamilyPresenter = stackFamilyPresenter;
    }

    @Override
    protected void handleUpdate(final Collection<StackChildFilter> prevValue, final Collection<StackChildFilter> values) {

        stackFamilyPresenter.setAsylumFilters(InstType.DR, FAMILY_NAME, values);
    }

    //
    //    private void process(final String quoteSymbol, final MDSource quoteSource, final InstrumentID quoteInstID,
    //            final InstrumentID leanInstID) {
    //
    //        final StackMetadata metadata = new StackMetadata(quoteSymbol, new TreeMap<>());
    //        metadata.data.put("Quote Venue", quoteSource.name());
    //        metadata.data.put("Quote Exch", quoteInstID.mic.exchange.name());
    //        metadata.data.put("Quote Ccy", quoteInstID.ccy.name());
    //        metadata.data.put("Lean Exch", leanInstID.mic.exchange.name());
    //        metadata.data.put("Lean Ccy", leanInstID.ccy.name());
    //        final Map<String, String> data = dataBySymbol.get(quoteSymbol);
    //        if (null != data) {
    //            data.forEach((key, value) -> {
    //                if (!metadata.data.containsKey(key) && !"Symbol".equals(key) && !"".equals(value.trim())) {
    //                    metadata.data.put(key, value);
    //                }
    //            });
    //        }
    //        if (!metadata.equals(publishedMetaData.put(metadata.symbol, metadata))) {
    //            metaDataPublisher.publish(metadata);
    //        }
    //    }
}

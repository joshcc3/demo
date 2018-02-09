package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.opxl.AOpxlReader;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

public class OpxlFXCalcUpdater extends AOpxlReader<Map<CCY, EnumMap<CCY, Double>>> {

    private static final String TOPIC_PREFIX = "eeif(fx_prices_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String FROM_CCY_COL = "From";
    private static final String TO_CCY_COL = "To";
    private static final String RATE_CCY_COL = "Most Recent Fix";

    private final FXCalc<PicardFXCalcComponents> fxCalc;
    private boolean awaitingData = true;

    public OpxlFXCalcUpdater(final FXCalc<PicardFXCalcComponents> fxCalc, final SelectIO selectIO,
            final IResourceMonitor<ReddalComponents> monitor, final Path path) {
        super(selectIO, monitor, ReddalComponents.OPXL_FX_CALC, getTopic(), path);
        this.fxCalc = fxCalc;
    }

    private static String getTopic() {

        final SimpleDateFormat sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT);
        final Calendar cal = DateTimeUtil.getCalendar();
        while (Calendar.SATURDAY == cal.get(Calendar.DAY_OF_WEEK) || Calendar.SUNDAY == cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        final String dateString = sdf.format(cal.getTime());
        return TOPIC_PREFIX + dateString + TOPIC_SUFFIX;
    }

    @Override
    protected boolean isConnectionWanted() {
        return awaitingData;
    }

    @Override
    protected Map<CCY, EnumMap<CCY, Double>> parseTable(final Object[][] opxlTable) {
        final Map<CCY, EnumMap<CCY, Double>> fxRates = new EnumMap<>(CCY.class);

        if (0 < opxlTable.length) {

            final int fromCcyCol = findColumn(opxlTable[0], FROM_CCY_COL);
            final int toCcyCol = findColumn(opxlTable[0], TO_CCY_COL);
            final int rateCol = findColumn(opxlTable[0], RATE_CCY_COL);

            for (int i = 1; i < opxlTable.length; ++i) {

                final Object[] row = opxlTable[i];

                if (testColsPresent(row, fromCcyCol, toCcyCol, rateCol)) {

                    final CCY fromCcy = CCY.getCCY(row[fromCcyCol].toString());
                    final CCY toCcy = CCY.getCCY(row[toCcyCol].toString());
                    final double fxRate = Double.parseDouble(row[rateCol].toString());

                    final Map<CCY, Double> rates = MapUtils.getMappedEnumMap(fxRates, fromCcy, CCY.class);
                    rates.put(toCcy, fxRate);
                }
            }
        }

        if (fxRates.isEmpty()) {
            monitor.logError(ReddalComponents.OPXL_FX_CALC, "No fx rates in [" + super.topic + "].");
            return null;
        } else {
            awaitingData = false;
            monitor.setOK(ReddalComponents.OPXL_FX_CALC);
            return fxRates;
        }
    }

    @Override
    protected void handleError(final ReddalComponents component, final String msg) {
        monitor.logError(component, msg);
    }

    @Override
    protected void handleError(final ReddalComponents component, final String msg, final Throwable t) {
        monitor.logError(component, msg, t);
    }

    @Override
    protected void handleUpdate(final Map<CCY, EnumMap<CCY, Double>> fxRates) {

        for (final Map.Entry<CCY, ? extends Map<CCY, Double>> rates : fxRates.entrySet()) {

            final CCY fromCcy = rates.getKey();

            for (final Map.Entry<CCY, Double> rate : rates.getValue().entrySet()) {
                final CCY toCcy = rate.getKey();
                final double fxRate = rate.getValue();
                fxCalc.setRate(fromCcy, toCcy, 0, true, fxRate, fxRate);
            }
        }
    }

    private static boolean testColsPresent(final Object[] row, final int... wantedCols) {

        for (int i = 0; i < wantedCols.length; ++i) {
            final int col = wantedCols[i];
            if (null == row[col] || "".equals(row[col].toString())) {
                return false;
            }
        }
        return true;
    }
}

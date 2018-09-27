package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.OPXLComponents;
import org.jetlang.channels.Publisher;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

public class OpxlExDateSubscriber extends AOpxlLoggingReader<OPXLComponents, ISINsGoingEx> {

    private static final String TOPIC_PREFIX = "eeif(isin_going_ex_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String ISIN_COL = "ISIN";

    private final Publisher<ISINsGoingEx> publisher;

    public OpxlExDateSubscriber(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Path logDir,
            final Publisher<ISINsGoingEx> publisher) {

        super(selectIO, selectIO, monitor, OPXLComponents.OPXL_ISINS_GOING_EX, getTopic(selectIO), logDir);

        this.publisher = publisher;
    }

    private static String getTopic(final IClock clock) {

        final SimpleDateFormat sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT);
        final String todayDate = sdf.format(clock.nowMilliUTC());
        return TOPIC_PREFIX + todayDate + TOPIC_SUFFIX;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected ISINsGoingEx parseTable(final Object[][] opxlTable) {

        final Object[] headerRow = opxlTable[0];

        final int isinCol = findColumn(headerRow, ISIN_COL);

        final Set<String> result = new HashSet<>();

        for (int i = 1; i < opxlTable.length; ++i) {

            final Object[] row = opxlTable[i];

            if (testColsPresent(row, isinCol)) {

                final String isin = row[isinCol].toString();
                result.add(isin);
            }
        }
        return new ISINsGoingEx(result);
    }

    @Override
    protected void handleUpdate(final ISINsGoingEx prevValue, final ISINsGoingEx values) {
        publisher.publish(values);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg) {
        monitor.logError(component, msg);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg, final Throwable t) {
        monitor.logError(component, msg, t);
    }
}

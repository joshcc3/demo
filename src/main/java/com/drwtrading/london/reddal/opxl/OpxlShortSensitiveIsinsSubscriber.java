package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.OpxlUtils;
import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.SelectIOChannel;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class OpxlShortSensitiveIsinsSubscriber extends AOpxlLoggingReader<OPXLComponents, Set<String>> {

    private static final String TOPIC_PREFIX = ".eeif(etf_sensitive_short_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String ISIN_COL = "ISIN";

    private final SelectIOChannel<Set<String>> publisher;

    public OpxlShortSensitiveIsinsSubscriber(final SelectIO selectIO, final IFuseBox<OPXLComponents> monitor, final Path logDir,
            final SelectIOChannel<Set<String>> publisher) {
        super(selectIO, selectIO, monitor, OPXLComponents.OPXL_SHORT_SENSITIVE,
                OpxlUtils.getTopic("prod", TOPIC_PREFIX, new Date(), TOPIC_SUFFIX), logDir);
        System.out.println("\tListening to " + OpxlUtils.getTopic("prod", TOPIC_PREFIX, new Date(), TOPIC_SUFFIX));
        this.publisher = publisher;

    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Set<String> parseTable(final Object[][] opxlTable) {
        final Object[] headerRow = opxlTable[0];

        final int isinCol = findColumn(headerRow, ISIN_COL);

        final Set<String> result = new HashSet<>();

        for (int i = 1; i < opxlTable.length; ++i) {

            final Object[] row = opxlTable[i];

            if (testColsPresent(row, isinCol)) {

                final String isin = row[isinCol].toString();
                if (validateIsin(isin)) {
                    result.add(isin);
                } else {
                    logErrorOnSelectIO("Invalid isin [" + isin + ']');
                }
            }
        }

        return result;
    }

    private static boolean validateIsin(final String isin) {
        boolean validISIN = 12 == isin.length();
        for (int i = 0; i < isin.length(); i++) {
            final char ch = isin.charAt(i);
            validISIN &= Character.isDigit(ch) || ch <= 'Z' && ch >= 'A';
        }
        return validISIN;
    }

    @Override
    protected void handleUpdate(final Set<String> prevValue, final Set<String> values) {
        publisher.publish(values);
    }

}

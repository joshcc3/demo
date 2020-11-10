package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.OpxlClient;
import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.application.Environment;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.SelectIOChannel;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class OPXLPicardFilterReader extends AOpxlLoggingReader<OPXLComponents, Set<String>> {

    private static final String TOPIC_SUFFIX = "(etf_picard_blacklist)";

    private static final String SYMBOL_HEADER = "Symbol";

    private final SelectIOChannel<Set<String>> symbolsPublisher;

    public OPXLPicardFilterReader(final OpxlClient<OPXLComponents> opxlClient, final SelectIO callbackSelectIO,
            final IFuseBox<OPXLComponents> monitor, final Environment env, final String group, final Path logDir,
            final SelectIOChannel<Set<String>> symbolsPublisher) {

        super(opxlClient, callbackSelectIO, monitor, OPXLComponents.OPXL_PICARD_BLACK_LIST, env.name() + '.' + group + TOPIC_SUFFIX,
                logDir);

        this.symbolsPublisher = symbolsPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Set<String> parseTable(final Object[][] opxlTable) {

        if (0 < opxlTable.length) {
            final int symbolCol = findColumn(opxlTable[0], SYMBOL_HEADER);

            final Set<String> result = new HashSet<>();

            for (int i = 1; i < opxlTable.length; ++i) {

                final Object[] row = opxlTable[i];

                if (testColsPresent(row, symbolCol)) {

                    final String symbol = row[symbolCol].toString();
                    result.add(symbol);
                }
            }
            return result;
        } else {
            return null;
        }
    }

    @Override
    protected void handleUpdate(final Set<String> prevValue, final Set<String> values) {

        symbolsPublisher.publish(values);
    }
}

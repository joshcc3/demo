package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.stacks.family.StackChildFilter;
import org.jetlang.channels.Publisher;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StackManagerFiltersOPXL extends AOpxlReader<Collection<StackChildFilter>> {

    private static final String TOPIC_PREFIX = "eeif(etf_filters_";
    private static final String TOPIC_SUFFIX = ")";

    private static final String SYMBOL_COL = "Symbol";

    private final Publisher<StackChildFilter> filtersPublisher;

    public StackManagerFiltersOPXL(final SelectIO selectIO, final IResourceMonitor<ReddalComponents> monitor,
            final Publisher<StackChildFilter> filtersPublisher, final Path logPath) {

        super(selectIO, monitor, ReddalComponents.OPXL_ETF_STACK_MANAGER_FILTERS, getTopic(selectIO), logPath);
        this.filtersPublisher = filtersPublisher;
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
    protected Collection<StackChildFilter> parseTable(final Object[][] opxlTable) {

        if (0 < opxlTable.length) {

            final Object[] headerRow = opxlTable[0];

            final int symbolCol = findColumn(headerRow, SYMBOL_COL);

            final LongMap<String> filterGroups = new LongMap<>();

            for (int i = 0; i < headerRow.length; ++i) {

                final String colName = headerRow[i].toString().trim();
                if (symbolCol != i && !colName.isEmpty()) {
                    filterGroups.put(i, colName);
                }
            }

            final Map<String, StackChildFilter> filters = new HashMap<>();

            boolean allOK = true;

            for (int i = 1; i < opxlTable.length; ++i) {

                final Object[] row = opxlTable[i];

                if (isColsPresent(row, symbolCol)) {

                    final String symbol = row[symbolCol].toString();

                    for (int col = 0; col < row.length; ++col) {

                        final String filterGroup = filterGroups.get(col);
                        final String filterName = row[col].toString().trim();
                        if (null != filterGroup && !filterName.isEmpty()) {

                            final StackChildFilter filter = getFilter(filters, filterName, filterGroup);
                            filter.addSymbol(symbol);

                            if (!filter.groupName.equals(filterGroup)) {

                                logErrorOnSelectIO(
                                        "Filter name [" + filterName + "] is used in two groups, [" + filter.groupName + "] and [" +
                                                filterGroup + "].");
                                allOK = false;
                            }
                        }
                    }
                }
            }
            if (allOK) {
                return filters.values();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static StackChildFilter getFilter(final Map<String, StackChildFilter> filters, final String filterName,
            final String filterGroup) {

        final StackChildFilter existingFilter = filters.get(filterName);
        if (null == existingFilter) {
            final StackChildFilter result = new StackChildFilter(filterGroup, filterName);
            filters.put(filterName, result);
            return result;
        } else {
            return existingFilter;
        }
    }

    @Override
    protected void handleUpdate(final Collection<StackChildFilter> filters) {

        for (final StackChildFilter filter : filters) {
            filtersPublisher.publish(filter);
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
}

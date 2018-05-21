package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.stacks.family.StackChildFilter;
import com.drwtrading.london.reddal.stacks.family.StackFamilyPresenter;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;

public class EtfStackFiltersOPXL extends AFiltersOPXL {

    private static final String TOPIC_PREFIX = "eeif(etf_filters_";
    private static final String TOPIC_SUFFIX = ")";

    private final StackFamilyPresenter stackFamilyPresenter;

    public EtfStackFiltersOPXL(final SelectIO selectIO, final IResourceMonitor<ReddalComponents> monitor, final Path logPath,
            final StackFamilyPresenter stackFamilyPresenter) {

        super(selectIO, monitor, ReddalComponents.OPXL_ETF_STACK_MANAGER_FILTERS, getTopic(selectIO), logPath);
        this.stackFamilyPresenter = stackFamilyPresenter;
    }

    private static String getTopic(final IClock clock) {

        final SimpleDateFormat sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.DATE_FILE_FORMAT);
        final String todayDate = sdf.format(clock.nowMilliUTC());
        return TOPIC_PREFIX + todayDate + TOPIC_SUFFIX;
    }

    @Override
    protected void handleUpdate(final Collection<StackChildFilter> filters) {

        stackFamilyPresenter.setFamiliesFilters(InstType.ETF, filters);
    }
}

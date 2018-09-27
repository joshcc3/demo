package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.stacks.family.StackChildFilter;
import com.drwtrading.london.reddal.stacks.family.StackFamilyPresenter;

import java.nio.file.Path;
import java.util.Collection;

public class SpreadnoughtFiltersOPXL extends AFiltersOPXL {

    public static final String FAMILY_NAME = "Spreadnought";
    private static final String TOPIC_PREFIX = "eeif(spreader_datapublish)";

    private final StackFamilyPresenter stackFamilyPresenter;

    public SpreadnoughtFiltersOPXL(final SelectIO opxlSelectIO, final SelectIO callbackSelectIO,
            final IResourceMonitor<OPXLComponents> monitor, final Path logPath, final StackFamilyPresenter stackFamilyPresenter) {

        super(opxlSelectIO, callbackSelectIO, monitor, OPXLComponents.OPXL_SPREAD_STACK_MANAGER_FILTERS, TOPIC_PREFIX, logPath);
        this.stackFamilyPresenter = stackFamilyPresenter;
    }

    @Override
    protected void handleUpdate(final Collection<StackChildFilter> prevValue, final Collection<StackChildFilter> values) {

        stackFamilyPresenter.setAsylumFilters(InstType.DR, FAMILY_NAME, values);
    }
}

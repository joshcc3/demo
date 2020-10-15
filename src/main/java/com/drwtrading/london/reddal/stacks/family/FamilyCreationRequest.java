package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;

import java.util.List;
import java.util.Set;

public class FamilyCreationRequest {

    public final InstrumentID instID;
    public final String familyName;
    public final Set<String> children;

    public FamilyCreationRequest(final InstrumentID isin, final String familyName, final Set<String> children) {
        this.instID = isin;

        this.familyName = familyName;
        this.children = children;
    }
}

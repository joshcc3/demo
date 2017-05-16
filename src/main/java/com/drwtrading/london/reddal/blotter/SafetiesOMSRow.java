package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerOMSEnabledState;

class SafetiesOMSRow {

    final int id;

    final String source;
    final long remoteOMSID;
    final String omsName;

    boolean isEnabled;
    String stateText;

    SafetiesOMSRow(final int id, final String source, final NibblerOMSEnabledState omsEnabledState) {

        this.id = id;
        this.source = source;
        this.remoteOMSID = omsEnabledState.getOMSID();
        this.omsName = omsEnabledState.getOmsName();

        update(omsEnabledState);
    }

    void update(final NibblerOMSEnabledState omsEnabledState) {

        this.isEnabled = omsEnabledState.isEnabled();
        this.stateText = omsEnabledState.getStateText();
    }
}

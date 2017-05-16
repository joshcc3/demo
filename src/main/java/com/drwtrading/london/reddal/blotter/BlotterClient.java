package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.blotter.INibblerBlotterListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.safeties.INibblerSafetiesListener;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerDoubleSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerOMSEnabledState;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerSafety;

public class BlotterClient implements INibblerBlotterListener, INibblerSafetiesListener, INibblerTransportConnectionListener {

    private final String source;

    private final MsgBlotterPresenter msgBlotter;
    private final SafetiesBlotterPresenter safetiesBlotter;

    public BlotterClient(final String source, final MsgBlotterPresenter msgBlotter, final SafetiesBlotterPresenter safetiesBlotter) {

        this.source = source;

        this.msgBlotter = msgBlotter;
        this.safetiesBlotter = safetiesBlotter;

        connectionLost(null);
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {

        msgBlotter.setNibblerConnected(source, true);
        safetiesBlotter.setNibblerConnected(source, true);
        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {

        msgBlotter.setNibblerConnected(source, false);
        safetiesBlotter.setNibblerConnected(source, false);
    }

    @Override
    public boolean addBlotterLine(final BlotterLine blotterLine) {

        msgBlotter.addLine(source, blotterLine);
        return true;
    }

    @Override
    public boolean addOMSEnabledState(final NibblerOMSEnabledState omsEnabledState) {
        safetiesBlotter.addOMS(source, omsEnabledState);
        return true;
    }

    @Override
    public boolean updateOMSEnabledState(final NibblerOMSEnabledState omsEnabledState) {
        safetiesBlotter.updateOMSEnabledState(source, omsEnabledState);
        return true;
    }

    @Override
    public boolean addSafety(final ANibblerSafety<?> safety) {
        safetiesBlotter.setSafety(source, safety);
        return true;
    }

    @Override
    public boolean updateSafety(final NibblerSafety safety) {
        safetiesBlotter.updateSafety(source, safety);
        return true;
    }

    @Override
    public boolean updateSafety(final NibblerDoubleSafety safety) {
        safetiesBlotter.updateSafety(source, safety);
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }
}

package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.blotter.INibblerBlotterListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.safeties.INibblerSafetiesListener;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterSymbolLine;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerDoubleSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerOMSEnabledState;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerSafety;
import com.drwtrading.london.reddal.orderManagement.NibblerTransportConnected;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.ui.WorkingOrdersPresenter;
import org.jetlang.channels.Publisher;

public class BlotterClient implements INibblerBlotterListener, INibblerSafetiesListener, INibblerTransportConnectionListener {

    private final String source;

    private final MsgBlotterPresenter msgBlotter;
    private final SafetiesBlotterPresenter safetiesBlotter;
    private final WorkingOrdersPresenter workingOrderPresenter;
    private final IWorkingOrdersCallback obligationsCallback;
    private final IWorkingOrdersCallback bestWorkingOrderMaintainer;

    private final Publisher<NibblerTransportConnected> connectedNibblerChannel;

    private final NibblerTransportConnected nibblerConnected;
    private final NibblerTransportConnected nibblerDisconnected;

    public BlotterClient(final String source, final MsgBlotterPresenter msgBlotter, final SafetiesBlotterPresenter safetiesBlotter,
            final WorkingOrdersPresenter workingOrderPresenter, final IWorkingOrdersCallback obligationsCallback,
            final IWorkingOrdersCallback bestWorkingOrderMaintainer, final Publisher<NibblerTransportConnected> connectedNibblerChannel,
            final String nibblerName) {

        this.source = source;

        this.msgBlotter = msgBlotter;
        this.safetiesBlotter = safetiesBlotter;
        this.workingOrderPresenter = workingOrderPresenter;
        this.obligationsCallback = obligationsCallback;
        this.bestWorkingOrderMaintainer = bestWorkingOrderMaintainer;

        this.connectedNibblerChannel = connectedNibblerChannel;
        this.nibblerConnected = new NibblerTransportConnected(nibblerName, true);
        this.nibblerDisconnected = new NibblerTransportConnected(nibblerName, false);

        connectionLost(null);
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {

        msgBlotter.setNibblerConnected(source, true);
        safetiesBlotter.setNibblerConnected(source, true);
        workingOrderPresenter.setNibblerConnectionEstablished(source, true);

        connectedNibblerChannel.publish(nibblerConnected);

        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {

        msgBlotter.setNibblerConnected(source, false);
        safetiesBlotter.setNibblerConnected(source, false);
        workingOrderPresenter.setNibblerConnectionEstablished(source, false);

        obligationsCallback.setNibblerDisconnected(source);
        bestWorkingOrderMaintainer.setNibblerDisconnected(source);

        connectedNibblerChannel.publish(nibblerDisconnected);
    }

    @Override
    public boolean addBlotterLine(final BlotterLine blotterLine) {

        msgBlotter.addLine(source, blotterLine.nanoSinceMidnightUTC, blotterLine.text);
        return true;
    }

    @Override
    public boolean addBlotterSymbolLine(final BlotterSymbolLine blotterLine) {

        msgBlotter.addLine(source, blotterLine.nanoSinceMidnightUTC, blotterLine.symbol + ": " + blotterLine.text);
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

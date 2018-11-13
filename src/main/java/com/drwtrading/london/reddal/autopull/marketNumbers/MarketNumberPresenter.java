package com.drwtrading.london.reddal.autopull.marketNumbers;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.StopAllForMarketNumberCmd;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.photons.eeif.configuration.MarketNumbers;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MarketNumberPresenter {

    private static final int PULL_WINDOW_MILLIS = 4 * 1000;
    private static final int WARN_WINDOW_MILLIS = 60 * 1000;

    private final SelectIO selectIO;

    private final UILogger uiLogger;
    private final Publisher<IOrderCmd> cmdsPublisher;

    private final WebSocketViews<IMarketNumberView> views;

    private final SimpleDateFormat sdf;

    private final Map<String, Long> pendingMarketNumbers;
    private final Map<String, Long> activeNumbers;
    private final HashSet<String> acknowledgedNumbers;

    public MarketNumberPresenter(final SelectIO selectIO, final UILogger uiLogger, final Publisher<IOrderCmd> cmdsPublisher) {

        this.selectIO = selectIO;

        this.uiLogger = uiLogger;
        this.cmdsPublisher = cmdsPublisher;

        this.views = new WebSocketViews<>(IMarketNumberView.class, this);

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.pendingMarketNumbers = new HashMap<>();
        this.activeNumbers = new LinkedHashMap<>();
        this.acknowledgedNumbers = new HashSet<>();
    }

    public void setMarketNumber(final MarketNumbers marketNumber) {

        final long now = selectIO.nowMilliUTC();
        final long mktNumberMilliSinceUTC = selectIO.getMillisAtMidnightUTC() + marketNumber.getMilliSinceMidnight();

        if (now < mktNumberMilliSinceUTC) {

            final Long prevTime = pendingMarketNumbers.put(marketNumber.getDescription(), mktNumberMilliSinceUTC);

            if (null == prevTime || mktNumberMilliSinceUTC < prevTime) {

                final long warnMilliSinceUTC = mktNumberMilliSinceUTC - WARN_WINDOW_MILLIS;
                selectIO.addClockActionMilliSinceUTC(warnMilliSinceUTC, () -> check(marketNumber.getDescription()));
            }
        } else {
            pendingMarketNumbers.remove(marketNumber.getDescription());
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            onInboundData((WebSocketInboundData) webMsg);
        }
    }

    private void onConnected(final WebSocketConnected connected) {

        final IMarketNumberView view = views.register(connected);
        refresh(view);
    }

    private void onDisconnected(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    private void onInboundData(final WebSocketInboundData data) {
        views.invoke(data);
    }

    @FromWebSocketView
    public void ack(final WebSocketInboundData data) {

        uiLogger.write("MarketNumberPresenter", data);

        acknowledgedNumbers.addAll(activeNumbers.keySet());
        activeNumbers.clear();

        views.all().clear();
    }

    private void refresh(final IMarketNumberView view) {

        view.clear();

        for (final Map.Entry<String, Long> pendingMarketNumber : activeNumbers.entrySet()) {

            final String description = pendingMarketNumber.getKey();

            final long numberMilliSinceUTC = pendingMarketNumber.getValue();
            final String time = sdf.format(numberMilliSinceUTC);

            view.add(description, time);
        }
    }

    private long check(final String description) {

        final Long numberMilliSinceUTC = pendingMarketNumbers.get(description);

        if (null == numberMilliSinceUTC) {

            return -1;

        } else {

            final long nowMilliSinceUTC = selectIO.nowMilliUTC();

            final long millisUntilEvent = numberMilliSinceUTC - nowMilliSinceUTC;

            if (isWithinWarningWindow(millisUntilEvent)) {

                activeNumbers.put(description, numberMilliSinceUTC);

                refresh(views.all());

                return 1 + millisUntilEvent - PULL_WINDOW_MILLIS;

            } else if (isWithinPullWindow(millisUntilEvent)) {

                pendingMarketNumbers.remove(description);
                activeNumbers.remove(description);
                final boolean isAcknowledged = acknowledgedNumbers.remove(description);

                final StopAllForMarketNumberCmd stopCmd = new StopAllForMarketNumberCmd(isAcknowledged, description);
                cmdsPublisher.publish(stopCmd);

                refresh(views.all());

                return -1;

            } else {
                return millisUntilEvent - WARN_WINDOW_MILLIS;
            }
        }
    }

    private static boolean isWithinWarningWindow(final long millisUntilEvent) {

        return PULL_WINDOW_MILLIS < millisUntilEvent && millisUntilEvent <= WARN_WINDOW_MILLIS;
    }

    private static boolean isWithinPullWindow(final long millisUntilEvent) {

        return 0 <= millisUntilEvent && millisUntilEvent <= PULL_WINDOW_MILLIS;
    }
}
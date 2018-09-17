package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;

public class LadderViewModel {

    private final UiPipeImpl ui;

    private final StringBuilder cmdSB;

    private final HeaderPanel headerPanel;
    private final LeftHandPanel leftHandPanel;
    private final BookPanel bookPanel;
    private final StackPanel stackPanel;

    public LadderViewModel(final UiPipeImpl ui) {

        this.ui = ui;

        this.cmdSB = new StringBuilder();

        this.headerPanel = new HeaderPanel(ui);
        this.leftHandPanel = new LeftHandPanel(ui);
        this.bookPanel = new BookPanel(ui);
        this.stackPanel = new StackPanel(ui);
    }

    public void extendToLevels(final int levels) {

        bookPanel.extendToLevels(levels);
        stackPanel.extendToLevels(levels);
    }

    public void inboundFromUI(final String data) {
        ui.onInbound(data);
    }

    public void clear() {

        ui.clear();
        bookPanel.clear();
        stackPanel.clear();
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public LeftHandPanel getLeftHandPanel() {
        return leftHandPanel;
    }

    public BookPanel getBookPanel() {
        return bookPanel;
    }

    public StackPanel getStackPanel() {
        return stackPanel;
    }

    public void setErrorText(final String issue) {
        ui.txt(HTML.CLICK_TRADING_ISSUES, issue);
    }

    public void setClass(final String htmlKey, final CSSClass cssClass, final boolean enabled) {
        ui.cls(htmlKey, cssClass, enabled);
    }

    public void setClickable(final String htmlKey) {
        ui.clickable(htmlKey);
    }

    public void setScrollable(final String htmlKey) {
        ui.scrollable(htmlKey);
    }

    public void setData(final String htmlKey, final DataKey dataKey, final Object value) {
        ui.data(htmlKey, dataKey, value);
    }

    public void setHeight(final String laserKey, final String bookPriceKey, final double v) {
        ui.height(laserKey, bookPriceKey, v);
    }

    public void flush() {
        ui.flush();
    }

    public void sendHeartbeat(final long lastHeartbeatSentMillis, final long heartbeatSeqNo) {
        final String heartbeat = UiPipeImpl.cmd(cmdSB, "heartbeat", lastHeartbeatSentMillis, heartbeatSeqNo);
        ui.send(heartbeat);
    }
}

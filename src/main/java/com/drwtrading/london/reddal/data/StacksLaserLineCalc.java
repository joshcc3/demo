package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.picard.IPicardSpotter;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class StacksLaserLineCalc {

    private static final int BID_PULLBACK_MULT = -1;
    private static final int ASK_PULLBACK_MULT = 1;

    private final IPicardSpotter picardSpotter;

    private final LaserLineValue navLine;
    private final LaserLineValue theoLine;
    private final LaserLineValue spreadnoughtLine;

    private final LaserLineValue bidTheo;
    private final LaserLineValue askTheo;

    private final Map<LaserLineType, LaserLineValue> laserLines;

    private TheoValue theoValue;
    private SpreadnoughtTheo spreadnoughtTheo;
    private StackGroup bidStackGroup;
    private StackGroup askStackGroup;

    StacksLaserLineCalc(final String symbol, final IPicardSpotter picardSpotter) {

        this.picardSpotter = picardSpotter;

        this.navLine = new LaserLineValue(symbol, LaserLineType.NAV);
        this.theoLine = new LaserLineValue(symbol, LaserLineType.GREEN);
        this.spreadnoughtLine = new LaserLineValue(symbol, LaserLineType.WHITE);

        this.bidTheo = new LaserLineValue(symbol, LaserLineType.BID);
        this.askTheo = new LaserLineValue(symbol, LaserLineType.ASK);

        this.laserLines = new EnumMap<>(LaserLineType.class);

        this.laserLines.put(theoLine.getType(), theoLine);
        this.laserLines.put(spreadnoughtLine.getType(), spreadnoughtLine);
        this.laserLines.put(bidTheo.getType(), bidTheo);
        this.laserLines.put(askTheo.getType(), askTheo);
    }

    void overrideLaserLine(final LaserLineValue laserLine) {

        final LaserLineValue overriddenLaserLine = laserLines.get(laserLine.getType());
        overriddenLaserLine.set(laserLine);
    }

    void setTheoValue(final TheoValue theoValue) {

        this.theoValue = theoValue;

        if (null == spreadnoughtTheo) {
            setTheoValue(navLine, theoValue.isValid(), theoValue.getTheoreticalValue());
            setTheoValue(theoLine, theoValue.isValid(), theoValue.getTheoreticalValue());

            updateLaserFromTheo(bidTheo, bidStackGroup, BID_PULLBACK_MULT);
            updateLaserFromTheo(askTheo, askStackGroup, ASK_PULLBACK_MULT);
        }
    }

    TheoValue getTheoValue() {
        return theoValue;
    }

    void setSpreadnoughtTheo(final SpreadnoughtTheo theo) {

        this.spreadnoughtTheo = theo;

        final boolean isValid = theo.isBidValid() && theo.isAskValid();
        final long mid = (theo.getBidValue() + theo.getAskValue()) / 2;
        setTheoValue(navLine, isValid, mid);
        setTheoValue(spreadnoughtLine, isValid, mid);
        setTheoValue(theoLine, false, mid);

        updateLaserLine(bidTheo, spreadnoughtTheo.isBidValid(), spreadnoughtTheo.getBidValue(), bidStackGroup, BID_PULLBACK_MULT);
        updateLaserLine(askTheo, spreadnoughtTheo.isAskValid(), spreadnoughtTheo.getAskValue(), askStackGroup, ASK_PULLBACK_MULT);
    }

    private static void setTheoValue(final LaserLineValue theoLine, final boolean isValid, final long theoValue) {

        if (isValid) {
            theoLine.setValue(theoValue);
        } else {
            theoLine.setInvalid();
        }
    }

    void setBidStackGroup(final StackGroup stackGroup) {

        this.bidStackGroup = stackGroup;
        updateLaserLine(BookSide.BID, bidTheo, bidStackGroup, BID_PULLBACK_MULT);
    }

    void setAskStackGroup(final StackGroup stackGroup) {

        this.askStackGroup = stackGroup;
        updateLaserLine(BookSide.ASK, askTheo, askStackGroup, ASK_PULLBACK_MULT);
    }

    private void updateLaserLine(final BookSide side, final LaserLineValue laserLine, final StackGroup stackGroup,
            final long pullbackDirection) {

        if (null == spreadnoughtTheo) {
            updateLaserFromTheo(laserLine, stackGroup, pullbackDirection);
        } else if (BookSide.BID == side) {
            updateLaserLine(bidTheo, spreadnoughtTheo.isBidValid(), spreadnoughtTheo.getBidValue(), bidStackGroup, BID_PULLBACK_MULT);
        } else {
            updateLaserLine(askTheo, spreadnoughtTheo.isAskValid(), spreadnoughtTheo.getAskValue(), askStackGroup, ASK_PULLBACK_MULT);
        }
    }

    private void updateLaserFromTheo(final LaserLineValue laserLine, final StackGroup stackGroup, final long pullbackDirection) {

        if (null != theoValue && theoValue.isValid()) {
            updateLaserLine(laserLine, true, theoValue.getTheoreticalValue(), stackGroup, pullbackDirection);
        } else {
            updateLaserLine(laserLine, false, 0L, stackGroup, pullbackDirection);
        }
    }

    private void updateLaserLine(final LaserLineValue laserLine, final boolean isTheoValid, final long theo, final StackGroup stackGroup,
            final long pullbackDirection) {

        if (null != stackGroup && isTheoValid) {

            final Stack quoteStack = stackGroup.getStack(StackType.QUOTER);
            final Stack picardStack = stackGroup.getStack(StackType.PICARD);

            final StackLevel quoteStackLevel = quoteStack.getFirstLevel();
            final StackLevel picardStackLevel = picardStack.getFirstLevel();

            if (null != quoteStackLevel || null != picardStackLevel) {

                final double pullBackTicks;

                if (null == quoteStackLevel) {

                    pullBackTicks = picardStackLevel.getPullbackTicks();

                } else if (null == picardStackLevel) {

                    pullBackTicks = quoteStackLevel.getPullbackTicks();

                } else if (quoteStack.isEnabled() == picardStack.isEnabled()) {

                    pullBackTicks = Math.min(quoteStackLevel.getPullbackTicks(), picardStackLevel.getPullbackTicks());

                } else if (quoteStack.isEnabled()) {

                    pullBackTicks = quoteStackLevel.getPullbackTicks();

                } else {

                    pullBackTicks = picardStackLevel.getPullbackTicks();
                }

                final double theoPriceMult = 1 +
                        (stackGroup.getPriceOffsetBPS() + pullbackDirection * stackGroup.getStackAlignmentTickToBPS() * pullBackTicks) /
                                10000d;

                final long theoPrice = (long) (theoPriceMult * theo);
                laserLine.setValue(theoPrice);

            } else {
                laserLine.setInvalid();
            }
        } else {
            laserLine.setInvalid();
        }

        picardSpotter.setLaserLine(laserLine);
    }

    LaserLineValue getNavLaserLine() {
        return navLine;
    }

    LaserLineValue getTheoLaserLine() {
        return theoLine;
    }

    Collection<LaserLineValue> getLaserLines() {
        return laserLines.values();
    }
}

package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.additiveTransport.data.AdditiveOffset;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.icepie.transport.data.LaserLineType;
import com.drwtrading.london.reddal.picard.IPicardSpotter;
import com.drwtrading.london.reddal.premium.IPremiumCalc;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class StacksLaserLineCalc {

    private static final int BID_PULLBACK_MULT = -1;
    private static final int ASK_PULLBACK_MULT = 1;

    private final IPicardSpotter picardSpotter;
    private final IPremiumCalc premiumCalc;

    private final LaserLine navLine;
    private final LaserLine theoLine;
    private final LaserLine spreadnoughtLine;

    private final LaserLine bidTheo;
    private final LaserLine askTheo;

    private final Map<LaserLineType, LaserLine> laserLines;

    private TheoValue theoValue;

    private boolean isAdditiveOffsetRequired;
    private AdditiveOffset additiveOffset;

    private SpreadnoughtTheo spreadnoughtTheo;
    private StackGroup bidStackGroup;
    private StackGroup askStackGroup;

    StacksLaserLineCalc(final String symbol, final IPicardSpotter picardSpotter, final IPremiumCalc premiumCalc) {

        this.picardSpotter = picardSpotter;
        this.premiumCalc = premiumCalc;

        this.navLine = new LaserLine(symbol, LaserLineType.NAV);
        this.theoLine = new LaserLine(symbol, LaserLineType.GREEN);
        this.spreadnoughtLine = new LaserLine(symbol, LaserLineType.WHITE);

        this.bidTheo = new LaserLine(symbol, LaserLineType.BID);
        this.askTheo = new LaserLine(symbol, LaserLineType.ASK);

        this.laserLines = new EnumMap<>(LaserLineType.class);

        this.laserLines.put(theoLine.getType(), theoLine);
        this.laserLines.put(spreadnoughtLine.getType(), spreadnoughtLine);
        this.laserLines.put(bidTheo.getType(), bidTheo);
        this.laserLines.put(askTheo.getType(), askTheo);

        this.isAdditiveOffsetRequired = true;
    }

    void overrideLaserLine(final LaserLine laserLine) {

        final LaserLine overriddenLaserLine = laserLines.get(laserLine.getType());
        overriddenLaserLine.set(laserLine);

        picardSpotter.setLaserLine(laserLine);
    }

    void setTheoValue(final TheoValue theoValue) {

        this.theoValue = theoValue;

        if (null == spreadnoughtTheo) {

            setTheoValue(navLine, theoValue.isValid(), theoValue.getOriginalValue());
            setTheoValue(theoLine, theoValue.isValid(), theoValue.getTheoreticalValue());

            updateLaserFromTheo(BookSide.BID, bidTheo, bidStackGroup, BID_PULLBACK_MULT);
            updateLaserFromTheo(BookSide.ASK, askTheo, askStackGroup, ASK_PULLBACK_MULT);
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

        updateLaserLine(BookSide.BID, bidTheo, spreadnoughtTheo.isBidValid(), spreadnoughtTheo.getBidValue(), bidStackGroup,
                BID_PULLBACK_MULT);
        updateLaserLine(BookSide.ASK, askTheo, spreadnoughtTheo.isAskValid(), spreadnoughtTheo.getAskValue(), askStackGroup,
                ASK_PULLBACK_MULT);

        this.premiumCalc.setTheoMid(theo.getSymbol(), isValid, mid);
    }

    void setAdditiveOffset(final AdditiveOffset additiveOffset) {

        this.additiveOffset = additiveOffset;

        updateLaserLine(BookSide.BID, bidTheo, bidStackGroup, BID_PULLBACK_MULT);
        updateLaserLine(BookSide.ASK, askTheo, askStackGroup, ASK_PULLBACK_MULT);
    }

    private static void setTheoValue(final LaserLine theoLine, final boolean isValid, final long theoValue) {

        if (isValid) {
            theoLine.setValue(theoValue);
        } else {
            theoLine.setInvalid();
        }
    }

    void setStackStrategy(final StackStrategy strategy) {

        this.isAdditiveOffsetRequired = !strategy.getAdditiveSymbol().isEmpty();

        updateLaserLine(BookSide.BID, bidTheo, bidStackGroup, BID_PULLBACK_MULT);
        updateLaserLine(BookSide.ASK, askTheo, askStackGroup, ASK_PULLBACK_MULT);
    }

    void setBidStackGroup(final StackGroup stackGroup) {

        this.bidStackGroup = stackGroup;
        updateLaserLine(BookSide.BID, bidTheo, bidStackGroup, BID_PULLBACK_MULT);
    }

    void setAskStackGroup(final StackGroup stackGroup) {

        this.askStackGroup = stackGroup;
        updateLaserLine(BookSide.ASK, askTheo, askStackGroup, ASK_PULLBACK_MULT);
    }

    private void updateLaserLine(final BookSide side, final LaserLine laserLine, final StackGroup stackGroup,
            final long pullbackDirection) {

        if (null == spreadnoughtTheo) {
            updateLaserFromTheo(side, laserLine, stackGroup, pullbackDirection);
        } else if (BookSide.BID == side) {
            updateLaserLine(side, bidTheo, spreadnoughtTheo.isBidValid(), spreadnoughtTheo.getBidValue(), bidStackGroup, BID_PULLBACK_MULT);
        } else {
            updateLaserLine(side, askTheo, spreadnoughtTheo.isAskValid(), spreadnoughtTheo.getAskValue(), askStackGroup, ASK_PULLBACK_MULT);
        }
    }

    private void updateLaserFromTheo(final BookSide side, final LaserLine laserLine, final StackGroup stackGroup,
            final long pullbackDirection) {

        final boolean isTheoValid;
        final long theo;
        if (null == theoValue) {
            isTheoValid = false;
            theo = 0L;
        } else {
            isTheoValid = theoValue.isValid();
            theo = theoValue.getTheoreticalValue();
        }

        updateLaserLine(side, laserLine, isTheoValid, theo, stackGroup, pullbackDirection);
    }

    private void updateLaserLine(final BookSide side, final LaserLine laserLine, final boolean isTheoValid, final long theo,
            final StackGroup stackGroup, final long pullbackDirection) {

        final boolean isAdditiveOffsetValid;
        final double additiveOffsetBPS;

        if (!isAdditiveOffsetRequired) {

            isAdditiveOffsetValid = true;
            additiveOffsetBPS = 0d;

        } else if (null == additiveOffset) {

            isAdditiveOffsetValid = false;
            additiveOffsetBPS = 0d;

        } else if (BookSide.BID == side) {

            isAdditiveOffsetValid = additiveOffset.isBidValid();
            additiveOffsetBPS = additiveOffset.getBidOffsetBPS();
        } else {

            isAdditiveOffsetValid = additiveOffset.isAskValid();
            additiveOffsetBPS = additiveOffset.getAskOffsetBPS();
        }

        updateLaserLine(laserLine, isTheoValid, theo, stackGroup, pullbackDirection, isAdditiveOffsetValid, additiveOffsetBPS);
    }

    private void updateLaserLine(final LaserLine laserLine, final boolean isTheoValid, final long theo, final StackGroup stackGroup,
            final long pullbackDirection, final boolean isAdditiveOffsetValid, final double additiveOffsetBPS) {

        if (null == stackGroup || !isTheoValid || !isAdditiveOffsetValid) {

            laserLine.setInvalid();

        } else {

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
                        (stackGroup.getPriceOffsetBPS() + pullbackDirection * stackGroup.getStackAlignmentTickToBPS() * pullBackTicks +
                                additiveOffsetBPS) / 10000d;

                final long theoPrice = (long) (theoPriceMult * theo);
                laserLine.setValue(theoPrice);

            } else {
                laserLine.setInvalid();
            }
        }

        picardSpotter.setLaserLine(laserLine);
    }

    LaserLine getNavLaserLine() {
        return navLine;
    }

    LaserLine getTheoLaserLine() {
        return theoLine;
    }

    LaserLine getSpreadnoughtLaserLine() {
        return spreadnoughtLine;
    }

    Collection<LaserLine> getLaserLines() {
        return laserLines.values();
    }
}

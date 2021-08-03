package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.Constants;

public class LadderSmoothRefValues {

    private boolean isValid;
    private double nav;
    private double theo;

    public LadderSmoothRefValues() {
        this.isValid = false;
        this.nav = 1;
        this.theo = 1;
    }

    public void update(final SymbolStackData stackData) {
        final boolean dataPresent = null != stackData && null != stackData.getNavLaserLine() && null != stackData.getTheoLaserLine();
        final boolean isDataValid = dataPresent && stackData.getNavLaserLine().isValid() && stackData.getTheoLaserLine().isValid();

        final double weightOld;
        final double weightNew;
        if (!this.isValid) {
            weightOld = 0;
            weightNew = 1;
        } else if (isDataValid) {
            weightOld = 0.9;
            weightNew = 0.1;
        } else {
            weightOld = 1.0;
            weightNew = 0;
        }

        if (isDataValid) {
            final double nav = stackData.getNavLaserLine().getValue() / (double) Constants.NORMALISING_FACTOR;
            final double theo = stackData.getTheoLaserLine().getValue() / (double) Constants.NORMALISING_FACTOR;
            this.nav = this.nav * weightOld + nav * weightNew;
            this.theo = this.theo * weightOld + theo * weightNew;
        }

        this.isValid = isDataValid;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public double getSmoothNav() {
        return this.nav;
    }

    public double getSmoothTheo() {
        return this.theo;
    }

}

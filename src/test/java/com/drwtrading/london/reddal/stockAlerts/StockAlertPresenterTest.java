package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.ResourceIgnorer;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.eeif.utils.time.ManualClock;
import com.drwtrading.london.reddal.FXFuse;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.util.UILogger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StockAlertPresenterTest {

    private final FXCalc<FXFuse> fxCalc = new FXCalc<>(new ResourceIgnorer<>(), FXFuse.FX_TIMEOUT, MDSource.INTERNAL);

    private final IClock clock = new ManualClock(1);
    private final UILogger logger = Mockito.mock(UILogger.class);

    @Test
    public void addRfqTest() {
        final StockAlertPresenter presenter = new StockAlertPresenter(clock, fxCalc, logger);
        fxCalc.setRate(CCY.EUR, CCY.EUR, 10001, true, 1, 1);

        final RfqAlert alert = new RfqAlert(123, "FESXU8 RFQ", 100_000 * Constants.NORMALISING_FACTOR, 5, CCY.EUR, false);
        final StockAlert stockAlert = presenter.getStockAlertFromRfq(alert);

        Assert.assertEquals(stockAlert.type, "RFQ");
        Assert.assertTrue(stockAlert.msg.contains("notional: 500,000"));
    }

    @Test
    public void addEtfRfqTest() {
        final StockAlertPresenter presenter = new StockAlertPresenter(clock, fxCalc, logger);
        fxCalc.setRate(CCY.EUR, CCY.EUR, 10001, true, 1, 1);

        final RfqAlert alert = new RfqAlert(123, "BNKEFP RFQ", 100_000 * Constants.NORMALISING_FACTOR, 5, CCY.EUR, true);
        final StockAlert stockAlert = presenter.getStockAlertFromRfq(alert);

        Assert.assertEquals(stockAlert.type, "ETF_RFQ");
        Assert.assertTrue(stockAlert.msg.contains("notional: 500,000"));
    }

    @Test
    public void addBigRfqTest() {
        final StockAlertPresenter presenter = new StockAlertPresenter(clock, fxCalc, logger);
        fxCalc.setRate(CCY.EUR, CCY.EUR, 10001, true, 1, 1);

        final RfqAlert alert = new RfqAlert(123, "BNKEFP RFQ", 100_000 * Constants.NORMALISING_FACTOR, 1000, CCY.EUR, true);
        final StockAlert stockAlert = presenter.getStockAlertFromRfq(alert);

        Assert.assertEquals(stockAlert.type, "BIG_ETF_RFQ");
        Assert.assertTrue(stockAlert.msg.contains("notional: 100,000,000"));
    }

    @Test
    public void addRfqFxTest() {
        final StockAlertPresenter presenter = new StockAlertPresenter(clock, fxCalc, logger);
        fxCalc.setRate(CCY.USD, CCY.EUR, 10001, true, 0.5, 0.5);

        final RfqAlert alert = new RfqAlert(123, "BNKEFP RFQ", 10_000 * Constants.NORMALISING_FACTOR, 10, CCY.USD, true);
        final StockAlert stockAlert = presenter.getStockAlertFromRfq(alert);

        Assert.assertEquals(stockAlert.type, "ETF_RFQ");
        Assert.assertTrue(stockAlert.msg.contains("notional: 50,000"));
    }

}
package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.jetlang.PublisherStub;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;

import static org.testng.Assert.*;

public class RemoteOrderServerRouterTest {


    public static final String NIB1 = "N1";
    public static final String NIB2 = "N2";
    private RemoteOrderServerRouter router;
    private NibblerTransportOrderEntry mock1;
    private NibblerTransportOrderEntry mock2;
    private String symbol1;

    @BeforeMethod
    public void setUp() {
        router = new RemoteOrderServerRouter(
                new String[]{NIB1, NIB2}
        );
        mock1 = Mockito.mock(NibblerTransportOrderEntry.class);
        mock2 = Mockito.mock(NibblerTransportOrderEntry.class);
        router.addNibbler(NIB1, mock1);
        router.addNibbler(NIB2, mock2);
        symbol1 = "S1";
    }

    @Test
    public void testRoutesAnOrder() {
        // GIVEN: One instrument, one nibbler
        router.setInstrumentTradable(symbol1, EnumSet.of(OrderType.LIMIT), NIB1);

        // WHEN: Submit
        SubmitOrderCmd submit = submit(OrderType.LIMIT, symbol1);
        router.submitOrder(submit);

        // THEN: Route to nibbler
        submit.execute(Mockito.verify(mock1));
    }

    @Test
    public void testMaintainPriority() {
        // GIVEN: Nib2 claims instrument first
        router.setInstrumentTradable(symbol1, EnumSet.of(OrderType.LIMIT), NIB2);

        // WHEN: Submit
        SubmitOrderCmd submit = submit(OrderType.LIMIT, symbol1);
        router.submitOrder(submit);

        // THEN: Route to nib2
        submit.execute(Mockito.verify(mock2));

        // GIVEN: Nib1 claims instrument
        router.setInstrumentTradable(symbol1, EnumSet.of(OrderType.LIMIT), NIB1);

        // WHEN: Submit
        submit = submit(OrderType.LIMIT, symbol1);
        router.submitOrder(submit);

        // THEN: Priority changes and now routes to nib1
        submit.execute(Mockito.verify(mock1));
    }

    @Test
    public void testGTC() {
        // GIVEN: Two nibblers accept different order types for one symbol
        router.setInstrumentTradable(symbol1, EnumSet.of(OrderType.LIMIT), NIB1);
        router.setInstrumentTradable(symbol1, EnumSet.of(OrderType.LIMIT, OrderType.GTC), NIB2);

        // WHEN: Submit LIMIT supported by both nibblers
        SubmitOrderCmd submit = submit(OrderType.LIMIT, symbol1);
        router.submitOrder(submit);

        // THEN: Goes to high priority
        submit.execute(Mockito.verify(mock1));

        // WHEN: Submit GTC supported by Nib2
        submit = submit(OrderType.GTC, symbol1);
        router.submitOrder(submit);

        // THEN: Submit GTC to nib2
        submit.execute(Mockito.verify(mock2));

        // AND: Dont submit anything to nib1
        Mockito.verifyNoMoreInteractions(mock1);
    }

    @AfterMethod
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(mock1);
        Mockito.verifyNoMoreInteractions(mock2);
    }

    private SubmitOrderCmd submit(OrderType limit, String symbol) {
        return new SubmitOrderCmd(
                symbol, new PublisherStub<>(), "", BookSide.BID, limit, AlgoType.MANUAL,
                "", 1, 1
        );
    }
}
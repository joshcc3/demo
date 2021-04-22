package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.stack.manager.StackManagerComponents;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.manager.relations.StackOrphanage;
import com.drwtrading.london.eeif.stack.transport.data.symbology.StackTradableSymbol;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.SingleBandTickTable;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.indy.transport.data.BasketType;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.indy.transport.data.IndexCashComponent;
import com.drwtrading.london.indy.transport.data.IndexConstituent;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.indy.transport.data.IndexDefProvider;
import com.drwtrading.london.indy.transport.data.IndexType;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.indy.transport.data.Source;
import com.drwtrading.london.reddal.stacks.StackRunnableInfo;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategySymbolUI;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuoteObligationsEnableCmd;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import org.jetlang.channels.Publisher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

public class StackFamilyViewTest {

    static final boolean IS_SECONDARY_VIEW = false;

    final SelectIO selectIO = Mockito.mock(SelectIO.class);
    final IErrorLogger errorLogger = Mockito.mock(IErrorLogger.class);
    final SpreadContractSetGenerator contractSetGenerator = Mockito.mock(SpreadContractSetGenerator.class);
    final OpxlStrategySymbolUI strategySymbolUI = Mockito.mock(OpxlStrategySymbolUI.class);
    final StackCommunityManager communityManager = Mockito.mock(StackCommunityManager.class);
    final StackCommunity community = StackCommunity.DM;

    @Mock
    IFuseBox<StackManagerComponents> fuseBox;
    @Mock
    TypedChannel<String> symbolsChannel;
    @Mock
    Publisher<QuoteObligationsEnableCmd> quotingObligationsCmds;
    @Mock
    TypedChannel<String> isinChannel;
    @Mock
    TypedChannel<StackRunnableInfo> runnableInfoChan;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(selectIO, errorLogger, strategySymbolUI, contractSetGenerator);
    }

    @Test
    public void clashingRootsTest() {
        final StackFamilyView view =
                new StackFamilyView(selectIO, selectIO, fuseBox, errorLogger, community, contractSetGenerator, IS_SECONDARY_VIEW,
                        strategySymbolUI, quotingObligationsCmds, symbolsChannel, isinChannel, runnableInfoChan);

        final ETFDef etfDef;
        final ETFDef etfDef2;
        final InstrumentID instID = InstrumentID.getFromIsinCcyMic("US" + "00".repeat(5) + ".USD.XLON");
        final String bbgCode = "TEST LN";
        final CCY ccy = CCY.USD;
        final Source source = Source.MARKIT;
        final InstrumentID constId = InstrumentID.getFromIsinCcyMic("0".repeat(11) + "1.USD.XLON");
        final String constBBGCode = "CONST LN";
        final IndexType indexType = IndexType.FIXED_INCOME;
        final double cashUnits = 0;
        etfDef = createETFDef(BasketType.TRACKING, instID, bbgCode, ccy, source, constId, constBBGCode, cashUnits, indexType);

        final InstrumentID instID2 = InstrumentID.getFromIsinCcyMic("US" + "01".repeat(5) + ".USD.ARCX");
        final String bbgCode2 = "TEST UP";
        final CCY ccy2 = CCY.USD;
        final Source source2 = Source.MARKIT;
        final InstrumentID constId2 = InstrumentID.getFromIsinCcyMic("10".repeat(6) + ".USD.ARCX");
        final String constBBGCode2 = "CONST2 LN";
        final IndexType indexType2 = IndexType.EQUITY_DM;
        final double cashUnits2 = 0;
        etfDef2 = createETFDef(BasketType.TRACKING, instID2, bbgCode2, ccy2, source2, constId2, constBBGCode2, cashUnits2, indexType2);

        final ITickTable tickTable = new SingleBandTickTable(1);

        final String orphanage = StackOrphanage.ORPHANAGE;
        final StackUIData orphanageStackData = new StackUIData("ui", orphanage, instID, orphanage, InstType.UNKNOWN, orphanage);
        final FamilyUIData familyUIData = new FamilyUIData(orphanageStackData);
        final StackTradableSymbol tradableData = new StackTradableSymbol(bbgCode, instID);
        final StackTradableSymbol tradableData2 = new StackTradableSymbol(bbgCode2, instID2);

        final Answer<?> answer = (Answer<Object>) invocation -> {
            final Object[] arguments = invocation.getArguments();
            final String sources = (String) arguments[0];
            final String familyName = (String) arguments[1];
            final InstrumentID instrID = (InstrumentID) arguments[2];
            final StackUIData uiData = new StackUIData(sources, familyName, instrID, "", InstType.ETF, "");
            view.addFamilyUIData(new FamilyUIData(uiData));
            return null;
        };
        Mockito.doAnswer(answer).when(communityManager).createFamily(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
                Matchers.any());

        view.setCommunityManager(communityManager);
        view.addTradableSymbol(bbgCode, tradableData);
        view.addTradableSymbol(bbgCode2, tradableData2);
        view.addFamilyUIData(familyUIData);
        view.setSearchResult(new SearchResult(bbgCode, instID, InstType.ETF, "", MDSource.LSE, List.of(), 0, tickTable));
        view.setSearchResult(new SearchResult(bbgCode2, instID2, InstType.ETF, "", MDSource.ARCA, List.of(), 0, tickTable));
        view.updateRelationship(bbgCode, orphanage, 0, 1, 1, 1, 1);
        view.updateRelationship(bbgCode2, orphanage, 0, 1, 1, 1, 1);
        view.autoFamily(etfDef);
        Mockito.verify(communityManager).createFamily("FAMILY_ADMIN_UI_AUTO", "TEST", instID, community.instType, community);
        Mockito.verify(communityManager).setRelationship("FAMILY_ADMIN_UI_AUTO", "TEST", bbgCode);
        view.autoFamily(etfDef2);
        Mockito.verify(communityManager).createFamily("FAMILY_ADMIN_UI_AUTO", "TEST0", instID2, community.instType, community);
        Mockito.verify(communityManager).setRelationship("FAMILY_ADMIN_UI_AUTO", "TEST0", bbgCode2);
        Mockito.verifyNoMoreInteractions(communityManager);
    }

    public static ETFDef createETFDef(final BasketType basketType, final InstrumentID instID, final String bbgCode, final CCY ccy,
            final Source source, final InstrumentID constituentInstID, final String constituentBBGCode, final double cashUnits,
            final IndexType indexType) {

        final Collection<InstrumentDef> instDefs = List.of(new InstrumentDef(instID, InstType.ETF, Source.MARKIT, true, bbgCode, false));

        final List<IndexConstituent> constituents =
                List.of(new IndexConstituent(new InstrumentDef(constituentInstID, InstType.EQUITY, source, true, constituentBBGCode, true),
                        1, 50, 1));
        final List<IndexCashComponent> cashComponents = List.of(new IndexCashComponent(ccy, cashUnits, 50));
        final IndexDef indexDef = new IndexDef("TEST", IndexDefProvider.ISHARES, 0, ccy, source, constituents, cashComponents, indexType);
        return new ETFDef(instDefs, indexDef, basketType);
    }

}

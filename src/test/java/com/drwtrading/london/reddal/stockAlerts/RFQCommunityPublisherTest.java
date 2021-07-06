package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.symbols.RFQCommunityPublisher;
import com.drwtrading.london.reddal.symbols.SearchResult;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class RFQCommunityPublisherTest {

    @Mock
    TypedChannel<String> dmChannel;
    @Mock
    TypedChannel<String> futureChannel;
    @Mock
    TypedChannel<String> emChannel;
    @Mock
    TypedChannel<String> crChannel;
    @Mock
    TypedChannel<String> fiChannel;
    @Mock
    TypedChannel<String> fcChannel;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void firstSearchResultThenCommunityTest() {

        final EnumMap<StackCommunity, TypedChannel<String>> channels = new EnumMap<>(StackCommunity.class);
        channels.put(StackCommunity.DM, dmChannel);
        channels.put(StackCommunity.FUTURE, futureChannel);
        channels.put(StackCommunity.EM, emChannel);
        channels.put(StackCommunity.CR, crChannel);
        channels.put(StackCommunity.FI, fiChannel);
        channels.put(StackCommunity.FC, fcChannel);

        final RFQCommunityPublisher communityPublisher = new RFQCommunityPublisher(channels);
        final ITickTable tickTable = Mockito.mock(ITickTable.class);
        final NavigableMap<Long, Long> mp = new TreeMap<>();
        mp.put(1L, 1L);
        Mockito.when(tickTable.getRawTickLevels()).thenReturn(mp);
        final InstrumentID inst1 = InstrumentID.getFromIsinCcyMic("000000000001.GBP.XLON");
        final InstrumentID inst2 = InstrumentID.getFromIsinCcyMic("000000000002.GBP.XLON");
        final InstrumentID inst3 = InstrumentID.getFromIsinCcyMic("000000000003.GBP.XLON");
        final InstrumentID inst4 = InstrumentID.getFromIsinCcyMic("000000000004.GBP.XLON");
        final InstrumentID inst5 = InstrumentID.getFromIsinCcyMic("000000000005.GBP.XLON");
        final InstrumentID inst6 = InstrumentID.getFromIsinCcyMic("000000000006.GBP.XLON");
        final SearchResult s1 = new SearchResult("1 RFQ", inst1, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);
        final SearchResult s2 = new SearchResult("2 RFQ", inst2, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);
        final SearchResult s3 = new SearchResult("3 RFQ", inst3, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);
        final SearchResult s4 = new SearchResult("4 RFQ", inst4, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);
        final SearchResult s5 = new SearchResult("5 RFQ", inst5, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);
        final SearchResult s6 = new SearchResult("6 RFQ", inst6, InstType.EQUITY, "", MDSource.ALL, List.of(), 1, tickTable);

        communityPublisher.setSearchResult(s1);
        communityPublisher.setSearchResult(s2);
        communityPublisher.setSearchResult(s3);
        communityPublisher.setSearchResult(s4);
        communityPublisher.setSearchResult(s5);
        communityPublisher.setSearchResult(s6);

        communityPublisher.setCommunityForIsin(StackCommunity.DM, inst1.isin);
        communityPublisher.setCommunityForIsin(StackCommunity.EM, inst2.isin);
        communityPublisher.setCommunityForIsin(StackCommunity.FI, inst3.isin);
        communityPublisher.setCommunityForIsin(StackCommunity.FC, inst5.isin);
        communityPublisher.setCommunityForIsin(StackCommunity.CR, inst6.isin);
        communityPublisher.setCommunityForIsin(StackCommunity.FUTURE, inst4.isin);

        final ArgumentCaptor<String> dmCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> emCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> fiCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> fcCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> crCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> futureCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(dmChannel).publish(dmCapture.capture());
        Mockito.verify(emChannel).publish(emCapture.capture());
        Mockito.verify(fiChannel).publish(fiCapture.capture());
        Mockito.verify(fcChannel).publish(fcCapture.capture());
        Mockito.verify(crChannel).publish(crCapture.capture());
        Mockito.verify(futureChannel).publish(futureCapture.capture());

        Mockito.verifyNoMoreInteractions(dmChannel, emChannel, fiChannel, fcChannel, crChannel, futureChannel);

        Assert.assertEquals(dmCapture.getValue(), "1 RFQ");
        Assert.assertEquals(emCapture.getValue(), "2 RFQ");
        Assert.assertEquals(fiCapture.getValue(), "3 RFQ");
        Assert.assertEquals(futureCapture.getValue(), "4 RFQ");

    }

}

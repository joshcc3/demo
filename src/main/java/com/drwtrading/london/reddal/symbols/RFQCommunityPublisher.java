package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.reddal.stacks.family.StackFamilyView;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RFQCommunityPublisher {

    private final EnumMap<StackCommunity, TypedChannel<String>> symbolCommunityChannels;
    private final Map<InstrumentID, Set<String>> searchResultCommunities;
    private final Map<InstrumentID, StackCommunity> instIDCommunities;

    public RFQCommunityPublisher(final EnumMap<StackCommunity, TypedChannel<String>> symbolCommunityChannels) {
        this.symbolCommunityChannels = symbolCommunityChannels;
        this.searchResultCommunities = new HashMap<>();
        this.instIDCommunities = new HashMap<>();
    }

    public void setCommunityForInstrumentID(final StackCommunity community, final InstrumentID instrumentID) {
        this.instIDCommunities.put(instrumentID, community);
        publishSearchResultCommunity(community, searchResultCommunities.get(instrumentID));
    }

    public void setSearchResult(final SearchResult searchResult) {
        final String searchResultSymbol = searchResult.symbol;
        if (searchResultSymbol.endsWith(StackFamilyView.RFQ_SUFFIX)) {
            final StackCommunity community = instIDCommunities.get(searchResult.instID);
            final Set<String> searchResults = searchResultCommunities.computeIfAbsent(searchResult.instID, k -> new HashSet<>());

            publishSingleSearchResultCommunity(community, searchResultSymbol);
            searchResults.add(searchResultSymbol);
        }
    }

    private void publishSearchResultCommunity(final StackCommunity community, final Set<String> searchResults) {
        if (null != searchResults) {
            for (final String searchResultSymbol : searchResults) {
                publishSingleSearchResultCommunity(community, searchResultSymbol);
            }
        }
    }

    private void publishSingleSearchResultCommunity(final StackCommunity community, final String searchResultSymbol) {
        if (null != community) {
            symbolCommunityChannels.get(community).publish(searchResultSymbol);
        }
    }

}

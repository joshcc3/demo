package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.reddal.stacks.family.StackFamilyView;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RFQCommunityPublisher {

    private final EnumMap<StackCommunity, TypedChannel<String>> symbolCommunityChannels;
    private final Map<String, Set<String>> searchResultCommunities;
    private final Map<String, StackCommunity> isinCommunities;

    public RFQCommunityPublisher(final EnumMap<StackCommunity, TypedChannel<String>> symbolCommunityChannels) {
        this.symbolCommunityChannels = symbolCommunityChannels;
        this.searchResultCommunities = new HashMap<>();
        this.isinCommunities = new HashMap<>();
    }

    public void setCommunityForIsin(final StackCommunity community, final String isin) {
        this.isinCommunities.put(isin, community);
        publishSearchResultCommunity(community, searchResultCommunities.get(isin));
    }

    public void setSearchResult(final SearchResult searchResult) {
        final String searchResultSymbol = searchResult.symbol;
        if (searchResultSymbol.endsWith(StackFamilyView.RFQ_SUFFIX)) {
            final StackCommunity community = isinCommunities.get(searchResult.instID.isin);
            final Set<String> searchResults = searchResultCommunities.computeIfAbsent(searchResult.instID.isin, k -> new HashSet<>());

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

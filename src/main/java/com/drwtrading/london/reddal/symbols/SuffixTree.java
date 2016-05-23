package com.drwtrading.london.reddal.symbols;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class SuffixTree<T> {

    private static final Pattern WHITE_SPACE = Pattern.compile("\\W", Pattern.LITERAL);
    public final NavigableMap<String, Set<T>> suffixTree = new TreeMap<>();

    public void put(String key, final T value) {

        key = normalize(key);
        for (int i = 0; i < key.length(); i++) {

            final String substring = key.substring(i);
            if (!suffixTree.containsKey(substring)) {
                suffixTree.put(substring, new ObjectArraySet<>());
            }
            suffixTree.get(substring).add(value);
        }
    }

    public Set<T> search(String search) {

        search = normalize(search);
        final ObjectArraySet<T> results = new ObjectArraySet<>();
        for (final Map.Entry<String, Set<T>> entry : suffixTree.tailMap(search).entrySet()) {
            if (!entry.getKey().startsWith(search)) {
                break;
            }
            results.addAll(entry.getValue());
        }
        return results;
    }

    private static String normalize(final String key) {
        return WHITE_SPACE.matcher(key.toUpperCase()).replaceAll("");
    }
}

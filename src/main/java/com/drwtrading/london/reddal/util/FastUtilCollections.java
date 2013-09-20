package com.drwtrading.london.reddal.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FastUtilCollections {
    public static <K, V> Map<K, V> newFastMap() {
        return new Object2ObjectOpenHashMap<K, V>();
    }

    public static <K, V> Map<K, V> newFastMap(final int expected) {
        return new Object2ObjectOpenHashMap<K, V>(expected);
    }

    public static <K> Set<K> newFastSet() {
        return new ObjectOpenHashSet<K>();
    }
    
    public static <K> List<K> newFastList() {
        return new ObjectArrayList<K>();
    }
}

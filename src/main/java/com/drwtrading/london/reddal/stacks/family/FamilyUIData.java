package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

class FamilyUIData {

    public final StackUIData uiData;

    private final NavigableMap<String, StackUIRelationship> relationships;

    private StackCommunity stackCommunity;
    private String uiName;
    private boolean isRunnable;

    FamilyUIData(final StackUIData uiData) {

        this.uiData = uiData;
        this.relationships = new TreeMap<>();
        this.uiName = uiData.symbol;
        this.stackCommunity = null;
        this.isRunnable = true;
    }

    void setCommunity(final StackCommunity stackCommunity) {
        this.stackCommunity = stackCommunity;
    }

    StackCommunity getStackCommunity() {
        return stackCommunity;
    }

    boolean removeChild(final String childSymbol) {
        return null != relationships.remove(childSymbol);
    }

    void addChild(final String childSymbol, final StackUIRelationship newRelationship) {
        relationships.put(childSymbol, newRelationship);
    }

    StackUIRelationship getChildRelationship(final String childSymbol) {
        return relationships.get(childSymbol);
    }

    Collection<StackUIRelationship> getAllRelationships() {
        return relationships.values();
    }

    void setUIName(final String uiName) {
        this.uiName = uiName;
    }

    public boolean isRunnable() {
        return isRunnable;
    }

    String getUIName() {
        return uiName;
    }

    void setIsRunnable(final boolean isRunnable) {
        this.isRunnable = isRunnable;
    }
}

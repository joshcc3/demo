package com.drwtrading.london.reddal.stacks.family;

public class ChildUIData {

    public final String childSymbol;

    private StackFamilyChildRow childRow;
    private String family;

    public ChildUIData(final String childSymbol, final StackFamilyChildRow childRow) {
        this.childSymbol = childSymbol;
        this.childRow = childRow;
    }

    public ChildUIData(final String childSymbol, final String family) {
        this.childSymbol = childSymbol;
        this.family = family;
    }

    public void setChildRow(final StackFamilyChildRow childRow) {
        this.childRow = childRow;
    }

    public StackFamilyChildRow getChildRow() {
        return childRow;
    }

    public void setFamily(final String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }
}

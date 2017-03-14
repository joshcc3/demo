package com.drwtrading.london.reddal.stacks.family;

class StackUIRelationship {

    final String childSymbol;

    final long bidPriceOffset;
    final double bidQtyMultiplier;

    final long askPriceOffset;
    final double askQtyMultiplier;

    StackUIRelationship(final String childSymbol, final long bidPriceOffset, final double bidQtyMultiplier, final long askPriceOffset,
            final double askQtyMultiplier) {

        this.childSymbol = childSymbol;

        this.bidPriceOffset = bidPriceOffset;
        this.bidQtyMultiplier = bidQtyMultiplier;

        this.askPriceOffset = askPriceOffset;
        this.askQtyMultiplier = askQtyMultiplier;
    }
}

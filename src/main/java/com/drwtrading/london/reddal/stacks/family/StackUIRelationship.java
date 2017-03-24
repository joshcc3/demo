package com.drwtrading.london.reddal.stacks.family;

class StackUIRelationship {

    final String childSymbol;

    final double bidPriceOffsetBPS;
    final double bidQtyMultiplier;

    final double askPriceOffsetBPS;
    final double askQtyMultiplier;

    StackUIRelationship(final String childSymbol, final double bidPriceOffsetBPS, final double bidQtyMultiplier,
            final double askPriceOffsetBPS, final double askQtyMultiplier) {

        this.childSymbol = childSymbol;

        this.bidPriceOffsetBPS = bidPriceOffsetBPS;
        this.bidQtyMultiplier = bidQtyMultiplier;

        this.askPriceOffsetBPS = askPriceOffsetBPS;
        this.askQtyMultiplier = askQtyMultiplier;
    }
}

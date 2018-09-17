package com.drwtrading.london.reddal.ladders.model;

public enum BookRowPriceType {

    PRICE("0.00"),
    ONE_DP(".0"),
    TWO_DP("0.00");

    public final String formattingCode;

    BookRowPriceType(final String formattingCode) {

        this.formattingCode = formattingCode;
    }
}

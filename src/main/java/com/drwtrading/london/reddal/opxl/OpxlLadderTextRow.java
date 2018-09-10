package com.drwtrading.london.reddal.opxl;

public class OpxlLadderTextRow {

    final String symbol;
    final String cell;
    final String value;
    final String colour;

    OpxlLadderTextRow(final String symbol, final String cell, final String value, final String colour) {

        this.symbol = symbol;
        this.cell = cell;
        this.value = value;
        this.colour = colour;
    }
}

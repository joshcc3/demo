package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.formatting.json.IJSONable;
import com.drwtrading.london.eeif.utils.formatting.json.JSONWriter;
import drw.london.json.Jsonable;

import java.io.IOException;

public class QuotingObligation implements IJSONable, Jsonable {

    private final String symbol;
    private final int quantity;
    private final int width;
    private final String date;

    public QuotingObligation(final String symbol, final int quantity, final int width, final String date) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.width = width;
        this.date = date;
    }

    @Override
    public void appendJSON(final JSONWriter jsonWriter) throws IOException {
        jsonWriter.add("symbol", symbol);
        jsonWriter.add("quantity", quantity);
        jsonWriter.add("width", width);
        jsonWriter.add("date", date);
    }

    @Override
    public void toJson(final Appendable appendable) throws IOException {
        final JSONWriter writer = new JSONWriter(appendable);
        writer.writeJSON(this);
    }

    public String getSymbol() {
        return symbol;
    }

    public int getBpsWide() {
        return width;
    }

    public int getQuantity() {
        return quantity;
    }
}

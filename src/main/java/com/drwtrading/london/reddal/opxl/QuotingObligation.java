package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.formatting.json.IJSONable;
import com.drwtrading.london.eeif.utils.formatting.json.JSONWriter;
import drw.london.json.Jsonable;

import java.io.IOException;

public class QuotingObligation implements IJSONable, Jsonable {

    private final String symbol;
    private final int quantity;
    private final int width;
    private final QuotingObligationType type;

    public QuotingObligation(final String symbol, final int quantity, final int width, final QuotingObligationType type) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.width = width;
        this.type = type;
    }

    @Override
    public void appendJSON(final JSONWriter jsonWriter) throws IOException {
        jsonWriter.add("symbol", symbol);
        jsonWriter.add("quantity", quantity);
        jsonWriter.add("width", width);
        jsonWriter.add("type", type);
    }

    @Override
    public void toJson(final Appendable appendable) throws IOException {
        final JSONWriter writer = new JSONWriter(appendable);
        writer.writeJSON(this);
    }

    public QuotingObligationType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuotingWidth() {
        return width;
    }

    public int getQuantity() {
        return quantity;
    }
}

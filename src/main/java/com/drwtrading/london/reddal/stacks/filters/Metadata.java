package com.drwtrading.london.reddal.stacks.filters;

import com.drwtrading.london.util.Struct;

import java.util.Map;

public interface Metadata {

    public String getSymbol();

    public Map<String, String> getData();

    public abstract class AMetadata extends Struct implements Metadata {

        public final String symbol;
        public final Map<String, String> data;

        public AMetadata(final String symbol, final Map<String, String> data) {

            this.symbol = symbol;
            this.data = data;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public Map<String, String> getData() {
            return data;
        }

    }

}
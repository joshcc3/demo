package com.drwtrading.london.reddal.ladders;

import com.drwtrading.photons.ladder.LaserLine;
import org.jetlang.channels.Converter;

public class LaserLineStringConverter implements Converter<LaserLine, String> {
    @Override
    public String convert(LaserLine msg) {
        return msg.getSymbol() + " " + msg.getId() + " " + msg.isValid();
    }
}

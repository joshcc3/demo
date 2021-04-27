package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import org.jetlang.channels.Converter;

public class UpdateFromServerConverter implements Converter<UpdateFromServer, String> {

    @Override
    public String convert(final UpdateFromServer msg) {
        return msg.key;
    }
}

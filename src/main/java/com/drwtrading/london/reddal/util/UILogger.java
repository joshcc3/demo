package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketInboundData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;

public class UILogger {

    private static final String LOG_FILE = "web.log";

    private final SelectIO selectIO;
    private final SimpleDateFormat sdf;

    private final StringBuilder sb;
    private final BufferedWriter out;

    public UILogger(final SelectIO selectIO, final Path logDir) throws IOException {

        this.selectIO = selectIO;

        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sb = new StringBuilder();
        final Path logFile = logDir.resolve(LOG_FILE);
        this.out = Files.newBufferedWriter(logFile,
                new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND});
    }

    public void write(final String page, final WebSocketInboundData msg) {

        selectIO.execute(() -> {

            final String time = sdf.format(selectIO.getReferenceNanoSinceMidnightUTC() / DateTimeUtil.NANOS_IN_MILLIS);
            final String remoteAddress = msg.getClient().getConnection().httpRequest().remoteAddress().toString();
            final String user = msg.getClient().getUserName();

            sb.setLength(0);
            sb.append(time);
            sb.append(' ');
            sb.append(remoteAddress);
            sb.append(' ');
            sb.append(user);
            sb.append(' ');
            sb.append(page);
            sb.append(':');
            sb.append(' ');
            sb.append(msg.getData());
            sb.append('\n');

            try {
                out.write(sb.toString());
                out.flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void write(final String page, final WebSocketControlMessage msg, final boolean isConnected, final int count) {

        selectIO.execute(() -> {

            final String time = sdf.format(selectIO.getReferenceNanoSinceMidnightUTC() / DateTimeUtil.NANOS_IN_MILLIS);
            final String remoteAddress = msg.getClient().getConnection().httpRequest().remoteAddress().toString();
            final String user = msg.getClient().getUserName();

            sb.setLength(0);
            sb.append(time);
            sb.append(' ');
            sb.append(remoteAddress);
            sb.append(' ');
            sb.append(user);
            sb.append(' ');
            sb.append(page);
            sb.append(':');
            sb.append(' ');
            if (isConnected) {
                sb.append("Connected");
            } else {
                sb.append("Disconnected");
            }
            sb.append(" total connections [");
            sb.append(count);
            sb.append("].\n");

            try {
                out.write(sb.toString());
                out.flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
    }
}

package com.drwtrading.london.reddal.util;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import org.jetlang.channels.Publisher;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class FileLogger {

    private final IClock clock;
    private final SimpleDateFormat sdf;

    private final Publisher<Throwable> errors;

    private final PrintWriter out;

    public FileLogger(final IClock clock, final Path dir, final String fileName, final Publisher<Throwable> errors) throws IOException {

        this.clock = clock;
        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.errors = errors;

        Files.createDirectories(dir);

        if (fileName.endsWith(".gz")) {
            final SimpleDateFormat fileTime = DateTimeUtil.getDateFormatter(DateTimeUtil.SIMPLE_TIME_FORMAT);
            fileTime.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
            final String timestamp = fileTime.format(new Date());
            final String timeFileName = fileName.substring(0, fileName.length() - 3) + '.' + timestamp + ".gz";

            final Path logFile = dir.resolve(timeFileName);
            final OutputStream oStream = Files.newOutputStream(logFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            out = new PrintWriter(new GZIPOutputStream(oStream));
        } else {
            final Path logFile = dir.resolve(fileName);
            final BufferedWriter bWriter = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            out = new PrintWriter(bWriter);
        }
    }

    @Subscribe
    public void onMessage(final Object obj) {
        write(obj);
    }

    public void write(final Object event) {
        try {
            final long currentTime = clock.getMillisSinceMidnightUTC();
            final String timeStamp = sdf.format(currentTime);

            out.write(timeStamp);
            out.write(": ");
            out.write(event.toString());
            out.println();
            out.flush();

        } catch (final Exception e) {
            errors.publish(e);
        }
    }
}


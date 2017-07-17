package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import drw.opxl.OpxlCallbacks;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public abstract class AOpxlReader<T> extends OpxlCallbacks implements Closeable {

    private static final String HOST = "prod-opxl.eeif.drw";
    private static final int PORT = 30002;

    private final SelectIO selectIO;
    protected final IResourceMonitor<ReddalComponents> monitor;

    protected final ReddalComponents component;

    private final String topic;

    private final OpxlClient opxlClient;
    private final BufferedWriter fileWriter;
    private final SimpleDateFormat sdf;

    protected AOpxlReader(final SelectIO selectIO, final IResourceMonitor<ReddalComponents> monitor, final ReddalComponents component,
            final String topic, final Path path) {

        this.selectIO = selectIO;
        this.monitor = monitor;

        this.component = component;

        this.topic = topic;

        this.opxlClient = new OpxlClient(HOST, PORT, Collections.singleton(topic), this);

        final Path logFile = path.resolve(topic + ".txt");
        try {
            Files.createDirectories(path);
            this.fileWriter = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            throw new RuntimeException("Could not create output file for topic [" + logFile + "].", e);
        }
        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
    }

    public void connectToOpxl() {
        try {
            opxlClient.connect();
        } catch (final Exception e) {
            monitor.logError(component, "OPXL connection failure [" + topic + "].", e);
        }
    }

    @Override
    public void on(final OpxlData opxlData) {
        try {

            final Object[][] opxlTable = opxlData.getData();

            final T result = parseTable(opxlTable);

            if (null != result) {

                final String timestamp = sdf.format(new Date());
                fileWriter.write(timestamp);
                fileWriter.write(":\n");
                for (final Object[] row : opxlTable) {
                    fileWriter.write(Arrays.toString(row));
                    fileWriter.write('\n');
                }
                fileWriter.write(Integer.toString(opxlTable.length));
                fileWriter.write(" rows.\n");
                fileWriter.flush();

                selectIO.execute(() -> {
                    try {
                        handleUpdate(result);
                        monitor.setOK(component);
                    } catch (final Throwable t) {
                        logErrorOnSelectIO("Failed to handle update [" + topic + "].", t);
                    }
                });
            }
        } catch (final Throwable t) {
            logErrorOnSelectIO("Failed to parse Table [" + topic + "].", t);
        }

        if (!isConnectionWanted()) {
            close();
        }
    }

    @Override
    public void close() {
        try {
            opxlClient.close();
            fileWriter.close();
        } catch (final IOException ioE) {
            logErrorOnSelectIO("Failed to close OPXL Connection.", ioE);
        }
    }

    @Override
    public void onDisconnect() {
        if (isConnectionWanted()) {
            logErrorOnSelectIO("Disconnected from server [" + topic + "].");
            selectIO.addDelayedAction(60000, () -> {
                connectToOpxl();
                return -1L;
            });
        }
    }

    protected void logErrorOnSelectIO(final String msg) {
        selectIO.execute(() -> handleError(component, msg));
    }

    protected void logErrorOnSelectIO(final String msg, final Throwable t) {
        selectIO.execute(() -> handleError(component, msg, t));
    }

    protected static int findColumn(final Object[] row, final String col) {

        for (int i = 0; i < row.length; ++i) {
            if (col.equals(row[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to find [" + col + "] in row " + Arrays.toString(row) + '.');
    }

    protected static boolean isColsPresent(final Object[] row, final int... wantedCols) {

        for (int i = 0; i < wantedCols.length; ++i) {
            final int col = wantedCols[i];
            if (null == row[col] || row[col].toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected abstract boolean isConnectionWanted();

    protected abstract T parseTable(final Object[][] opxlTable);

    protected abstract void handleUpdate(final T values);

    protected abstract void handleError(final ReddalComponents component, final String msg);

    protected abstract void handleError(final ReddalComponents component, final String msg, final Throwable t);
}

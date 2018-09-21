package com.drwtrading.london.reddal.fastui;

import com.drwtrading.london.eeif.utils.collections.MapUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class DataBatcher<E extends Enum<E> & IEnumKey> implements ICmdAppender {

    private final String command;

    private final Class<E> enumKeyClass;
    private final E[] enumKeyClasses;

    private final Map<String, EnumMap<E, Object>> values;
    private final Map<String, EnumMap<E, Object>> pendingValues;

    DataBatcher(final String command, final Class<E> enumKeyClass) {

        this.command = command;
        this.enumKeyClass = enumKeyClass;
        this.enumKeyClasses = enumKeyClass.getEnumConstants();

        this.values = new HashMap<>();
        this.pendingValues = new HashMap<>();
    }

    void put(final String key, final E enumKey, final Object value) {

        final EnumMap<E, Object> pendingEnabledClasses = MapUtils.getMappedEnumMap(pendingValues, key, enumKeyClass);
        pendingEnabledClasses.put(enumKey, value);
    }

    @Override
    public boolean appendCommand(final StringBuilder cmdSB, final char separator) {

        boolean isCmdHeaderNeeded = true;
        if (!pendingValues.isEmpty()) {
            for (final Map.Entry<String, EnumMap<E, Object>> entry : pendingValues.entrySet()) {

                final EnumMap<E, Object> pendingEnabledClasses = entry.getValue();
                final EnumMap<E, Object> oldEnabledClasses = MapUtils.getMappedEnumMap(values, entry.getKey(), enumKeyClass);

                for (int i = 0; i < enumKeyClasses.length; ++i) {

                    final E enumKey = enumKeyClasses[i];

                    final Object value = pendingEnabledClasses.get(enumKey);
                    final Object oldValue = oldEnabledClasses.put(enumKey, value);

                    if (!Objects.equals(oldValue, value)) {

                        if (isCmdHeaderNeeded) {
                            cmdSB.append(this.command);
                            isCmdHeaderNeeded = false;
                        }
                        cmdSB.append(separator);
                        cmdSB.append(entry.getKey());
                        cmdSB.append(separator);
                        cmdSB.append(enumKey.getHTMLKey());
                        cmdSB.append(separator);
                        cmdSB.append(value);
                    }
                }
            }
        }
        return !isCmdHeaderNeeded;
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}

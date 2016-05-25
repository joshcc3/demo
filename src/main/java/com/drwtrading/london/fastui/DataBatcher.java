package com.drwtrading.london.fastui;

import com.drwtrading.london.eeif.utils.collections.MapUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class DataBatcher<E extends Enum<E> & IEnumKey> {

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

    void flushPendingIntoCommandList(final List<String> commands) {

        if (!pendingValues.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            appendCommands(sb);
            commands.add(sb.toString());
        }
    }

    private void appendCommands(final StringBuilder sb) {

        sb.append(this.command);
        for (final Map.Entry<String, EnumMap<E, Object>> entry : pendingValues.entrySet()) {

            final EnumMap<E, Object> pendingEnabledClasses = entry.getValue();
            final EnumMap<E, Object> oldEnabledClasses = MapUtils.getMappedEnumMap(values, entry.getKey(), enumKeyClass);

            for (int i = 0; i < enumKeyClasses.length; ++i) {

                final E enumKey = enumKeyClasses[i];

                final Object value = pendingEnabledClasses.get(enumKey);
                final Object oldValue = oldEnabledClasses.put(enumKey, value);

                if (!Objects.equals(oldValue, value)) {

                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(entry.getKey());
                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(enumKey.getHTMLKey());
                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(value);
                }
            }
        }
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}

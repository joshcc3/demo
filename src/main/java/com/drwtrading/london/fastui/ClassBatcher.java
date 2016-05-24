package com.drwtrading.london.fastui;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.fastui.html.CSSClass;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClassBatcher {

    private final String command;

    public final Map<String, EnumMap<CSSClass, Boolean>> values;
    public final Map<String, EnumMap<CSSClass, Boolean>> pendingValues;

    ClassBatcher(final String command) {

        this.command = command;
        this.values = new HashMap<>();
        this.pendingValues = new HashMap<>();
    }

    void put(final String key, final CSSClass cssClass, final boolean enabled) {

        final EnumMap<CSSClass, Boolean> pendingEnabledClasses = MapUtils.getMappedEnumMap(pendingValues, key, CSSClass.class);
        pendingEnabledClasses.put(cssClass, enabled);
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
        for (final Map.Entry<String, EnumMap<CSSClass, Boolean>> entry : pendingValues.entrySet()) {

            final EnumMap<CSSClass, Boolean> oldEnabledClasses = MapUtils.getMappedEnumMap(values, entry.getKey(), CSSClass.class);

            for (final Map.Entry<CSSClass, Boolean> changedEnabled : entry.getValue().entrySet()) {

                final CSSClass cssClass = changedEnabled.getKey();
                final boolean cssEnabled = changedEnabled.getValue();

                final Boolean oldValue = oldEnabledClasses.put(cssClass, cssEnabled);

                if (null == oldValue || oldValue != cssEnabled) {

                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(entry.getKey());
                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(cssClass.cssText);
                    sb.append(UiPipeImpl.DATA_SEPARATOR);
                    sb.append(cssEnabled);
                }
            }
        }
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}

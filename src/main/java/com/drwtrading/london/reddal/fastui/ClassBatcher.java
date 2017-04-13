package com.drwtrading.london.reddal.fastui;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.reddal.fastui.html.CSSClass;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ClassBatcher {

    private final String command;

    private final Map<String, EnumMap<CSSClass, Boolean>> values;
    private final Map<String, EnumMap<CSSClass, Boolean>> pendingValues;

    private final CSSClass[] cssClasses;

    ClassBatcher(final String command) {

        this.command = command;
        this.values = new HashMap<>();
        this.pendingValues = new HashMap<>();

        this.cssClasses = CSSClass.values();
    }

    void put(final String key, final CSSClass cssClass, final boolean enabled) {

        final EnumMap<CSSClass, Boolean> pendingEnabledClasses = MapUtils.getMappedEnumMap(pendingValues, key, CSSClass.class);
        pendingEnabledClasses.put(cssClass, enabled);
    }

    void flushPendingIntoCommandList(final List<String> commands) {

        if (!pendingValues.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            appendCommands(sb);
            if (this.command.length() < sb.length()) {
                commands.add(sb.toString());
            }
        }
    }

    private void appendCommands(final StringBuilder sb) {

        sb.append(this.command);
        for (final Map.Entry<String, EnumMap<CSSClass, Boolean>> entry : pendingValues.entrySet()) {

            final EnumMap<CSSClass, Boolean> pendingEnabledClasses = entry.getValue();
            final EnumMap<CSSClass, Boolean> oldEnabledClasses = MapUtils.getMappedEnumMap(values, entry.getKey(), CSSClass.class);

            for (int i = 0; i < cssClasses.length; ++i) {

                final CSSClass cssClass = cssClasses[i];

                final Boolean cssEnabled = pendingEnabledClasses.get(cssClass);
                final Boolean oldValue = oldEnabledClasses.put(cssClass, cssEnabled);

                if (!Objects.equals(oldValue, cssEnabled)) {

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

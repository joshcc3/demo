package com.drwtrading.london.reddal.fastui;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.reddal.fastui.html.CSSClass;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class ClassBatcher implements ICmdAppender {

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

    @Override
    public boolean appendCommand(final StringBuilder cmdSB, final char separator) {

        boolean isCmdHeaderNeeded = true;
        if (!pendingValues.isEmpty()) {
            for (final Map.Entry<String, EnumMap<CSSClass, Boolean>> entry : pendingValues.entrySet()) {

                final EnumMap<CSSClass, Boolean> pendingEnabledClasses = entry.getValue();
                final EnumMap<CSSClass, Boolean> oldEnabledClasses = MapUtils.getMappedEnumMap(values, entry.getKey(), CSSClass.class);

                for (int i = 0; i < cssClasses.length; ++i) {

                    final CSSClass cssClass = cssClasses[i];

                    final Boolean cssEnabled = pendingEnabledClasses.get(cssClass);
                    final Boolean oldValue = oldEnabledClasses.put(cssClass, cssEnabled);

                    if (!Objects.equals(oldValue, cssEnabled)) {

                        if (isCmdHeaderNeeded) {
                            cmdSB.append(this.command);
                            isCmdHeaderNeeded = false;
                        }
                        cmdSB.append(separator);
                        cmdSB.append(entry.getKey());
                        cmdSB.append(separator);
                        cmdSB.append(cssClass.cssText);
                        cmdSB.append(separator);
                        cmdSB.append(cssEnabled);
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

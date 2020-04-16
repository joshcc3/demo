package com.drwtrading.london.reddal.util;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

public class EnumSwitcher<E extends Enum<E>> {

    private final E[] universe;
    private final EnumSet<E> validChoices;

    private E current = null;

    @SafeVarargs
    public EnumSwitcher(final Class<E> clazz, final E... validChoices) {

        this.universe = clazz.getEnumConstants();
        Preconditions.checkArgument(universe.length > 0,
                "Need at least one element enum switcher " + clazz.getName() + ", got " + Arrays.asList(universe));

        this.validChoices = EnumSet.noneOf(clazz);
        setValidChoices(validChoices);
        if (validChoices.length > 0) {
            current = validChoices[0];
        }
    }

    @SafeVarargs
    public final void setValidChoices(final E... validChoices) {
        if (validChoices.length < 1) {
            throw new IllegalArgumentException("Need at least one valid choice from [" + Arrays.toString(universe) + "].");
        } else {
            this.validChoices.clear();
            Collections.addAll(this.validChoices, validChoices);
        }
    }

    public E next() {
        final int previousIdx = current.ordinal();

        for (int i = 1; i <= universe.length; i++) {
            final E next = universe[(previousIdx + i) % universe.length];
            if (validChoices.contains(next)) {
                current = next;
                return next;
            }
        }

        throw new IllegalArgumentException(
                "Cannot find next(): " + validChoices + " current: " + current + " currentIdx: " + current.ordinal());
    }

    public E get() {
        return current;
    }

    public void set(final E choice) {
        if (validChoices.contains(choice)) {
            current = choice;
        } else {
            throw new IllegalArgumentException(
                    "Cannot find valid choice " + choice + " in " + validChoices + " current: " + current + " currentIdx: " +
                            choice.ordinal());
        }
    }

    public E[] getUniverse() {
        return universe;
    }

    public boolean isValidChoice(final E choice) {
        return validChoices.contains(choice);
    }
}

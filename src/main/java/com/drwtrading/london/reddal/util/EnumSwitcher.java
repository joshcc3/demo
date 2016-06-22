package com.drwtrading.london.reddal.util;

import com.google.common.base.Preconditions;
import sun.misc.SharedSecrets;

import java.util.Arrays;
import java.util.EnumSet;

public class EnumSwitcher<E extends Enum<E>> {

    private final E[] universe;
    private final EnumSet<E> validChoices;

    E current = null;
    int currentIdx = 0;

    public EnumSwitcher(final Class<E> clazz, final EnumSet<E> validChoices) {
        this.validChoices = validChoices;
        Preconditions.checkArgument(!validChoices.isEmpty(),
                "Need at least one valid choice for enum switcher " + clazz.getName() + ", got " + validChoices);
        this.universe = getUniverse(clazz);
        Preconditions.checkArgument(universe.length > 0,
                "Need at least one element enum switcher " + clazz.getName() + ", got " + Arrays.asList(universe));
        current = universe[currentIdx];
    }

    public E next() {
        int iters = universe.length;
        do {
            if (iters-- < 0) {
                throw new IllegalArgumentException(
                        "Cannot find next(): " + validChoices + " current: " + current + " currentIdx: " + currentIdx);
            }
            currentIdx = (currentIdx + 1) % universe.length;
            current = universe[currentIdx];
        } while (!validChoices.contains(universe[currentIdx]));
        return current;
    }

    public E get() {
        return current;
    }

    public void set(final E choice) {
        int iters = universe.length;
        while (choice != next()) {
            if (iters-- < 0) {
                throw new IllegalArgumentException(
                        "Cannot find valid choice " + choice + " in " + validChoices + " current: " + current + " currentIdx: " +
                                currentIdx);
            }
        }
    }

    private static <E extends Enum<E>> E[] getUniverse(final Class<E> elementType) {
        return SharedSecrets.getJavaLangAccess().getEnumConstantsShared(elementType);
    }

    public E[] getUniverse() {
        return universe;
    }

    public boolean isValidChoice(final E choice) {
        return validChoices.contains(choice);
    }

}

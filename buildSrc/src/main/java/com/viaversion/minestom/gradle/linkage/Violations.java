package com.viaversion.minestom.gradle.linkage;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class Violations {
    private static final int LISTED_REFERRERS = 3;

    private final Map<String, Set<String>> referrers = new TreeMap<>();

    void add(final String violation, final String referrer) {
        referrers.computeIfAbsent(violation, key -> new TreeSet<>()).add(referrer);
    }

    boolean isEmpty() {
        return referrers.isEmpty();
    }

    int size() {
        return referrers.size();
    }

    String describe() {
        final StringBuilder description = new StringBuilder();
        for (final Map.Entry<String, Set<String>> violation : referrers.entrySet()) {
            description.append(System.lineSeparator()).append("  ").append(violation.getKey());
            description.append(" (required by ").append(summarize(violation.getValue())).append(')');
        }
        return description.toString();
    }

    private static String summarize(final Set<String> referrers) {
        final StringBuilder summary = new StringBuilder();
        final Iterator<String> iterator = referrers.iterator();
        for (int listed = 0; listed < LISTED_REFERRERS && iterator.hasNext(); listed++) {
            if (listed > 0) {
                summary.append(", ");
            }
            summary.append(iterator.next());
        }
        if (referrers.size() > LISTED_REFERRERS) {
            summary.append(" and ").append(referrers.size() - LISTED_REFERRERS).append(" more");
        }
        return summary.toString();
    }
}

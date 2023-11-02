package io.collective;

import java.time.Clock;
import java.time.Instant;

public class SimpleAgedCache {
    private ExpirableEntry latestEntry;
    private final Clock clock;

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
    }

    public SimpleAgedCache() {
        this.clock = Clock.systemUTC();
    }

    public void put(Object key, Object value, int retentionInMillis) {
        Instant expirationTime = clock.instant().plusMillis(retentionInMillis);
        ExpirableEntry entryToAdd = new ExpirableEntry(key, value, expirationTime);

        if (latestEntry == null) {
            // Cache is empty
            latestEntry = entryToAdd;
        } else {
            // Replace the latestEntry with the entryToAdd
            entryToAdd.next = latestEntry;
            latestEntry = entryToAdd;
        }
    }

    public boolean isEmpty() {
        cleanupEntries();
        return latestEntry == null;
    }

    public int size() {
        cleanupEntries(); // Remove expired entries
        int size = 0;
        ExpirableEntry current = latestEntry;
        while (current != null) {
            // Navigate entries through next and increment count
            size++;
            current = current.next;
        }
        return size;
    }

    public Object get(Object key) {
        cleanupEntries(); // Remove expired entries
        ExpirableEntry current = latestEntry;
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    private void cleanupEntries() {
        while (latestEntry != null && latestEntry.isExpired(clock.instant())) {
            latestEntry = latestEntry.next;
        }

        if (latestEntry != null) {
            // Cycle through entries
            ExpirableEntry current = latestEntry;
            while (current.next != null) {
                if (current.next.isExpired(clock.instant())) {
                    // Since next entry is expired, we update
                    // current.next reference to current.next.next to remove it
                    current.next = current.next.next;
                } else {
                    // Not expired, so continue cycling
                    current = current.next;
                }
            }
        }
    }

    private static class ExpirableEntry {
        private final Object key;
        private final Object value;
        private final Instant expirationTime;
        private ExpirableEntry next;

        ExpirableEntry(Object key, Object value, Instant expirationTime) {
            this.key = key;
            this.value = value;
            this.expirationTime = expirationTime;
        }

        boolean isExpired(Instant now) {
            return expirationTime.isBefore(now);
        }
    }
}
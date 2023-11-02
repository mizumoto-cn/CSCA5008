package io.collective;

import java.time.Clock;

public class SimpleAgedCache {

    private Entry[] map;
    private int size;
    private int capacity = 10000;
    private final Clock clock;

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.map = new Entry[capacity + 17]; // Add some extra space to avoid overflows
    }

    public SimpleAgedCache() {
        this(Clock.systemDefaultZone());
    }

    public void put(Object key, Object value, int retentionInMillis) {
        int index =  getIndex(key);
        long expiryTime = clock.millis() + retentionInMillis;
        Entry newEntry = new Entry(key, value, expiryTime);
        newEntry.next = map[index];
        map[index] = newEntry;
        this.size++;

        // Check if the cache is full
        // FIFO eviction
        if (this.size() > capacity) {
            // Remove the oldest entry
            removeOldest();
        }
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public int size() {
        removeExpiredEntries();
        return this.size;
    }

    public Object get(Object key) {
        int index = getIndex(key);
        for (Entry e = map[index]; e != null; e = e.next) {
            if (e.key.equals(key)) {
                if (clock.millis() > e.expiryTime) {
                    // remove the entry
                    e = null;
                } else {
                    return e.value;
                }
            }
        }
        return null;
    }

    private int getIndex(Object key) {
        return (capacity + key.hashCode() % capacity)  % capacity; // Avoid negative indices
    }

    // Remove the oldest entry
    private void removeOldest() {
        long oldestTime = Long.MAX_VALUE;
        int oldestIndex = -1;
        Entry oldestPrev = null;
        Entry oldest = null;
    
        for (int i = 0; i < capacity; i++) {
            Entry prev = null;
            for (Entry e = map[i]; e != null; e = e.next) {
                if (e.expiryTime < oldestTime) {
                    oldestTime = e.expiryTime;
                    oldestIndex = i;
                    oldestPrev = prev;
                    oldest = e;
                }
                prev = e;
            }
        }
    
        if (oldest == null) {
            return;
        }
        if (oldestPrev == null) {
            map[oldestIndex] = oldest.next;
        } else {
            oldestPrev.next = oldest.next;
        }
        this.size--;
    }

    private void removeExpiredEntries() {
        for (int i = 0; i < capacity; i++) {
            Entry prev = null;
            for (Entry e = map[i]; e != null; e = e.next) {
                if (clock.millis() > e.expiryTime) {
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        map[i] = e.next;
                    }
                    this.size--;
                }
                prev = e;
            }
        }
    }

}
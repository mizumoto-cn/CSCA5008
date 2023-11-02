package io.collective

import java.time.Clock

class SimpleAgedKache {
    private val maxCapacity: Int = 10000
    private var entries: Array<Entry?>? = null
    private var clock: Clock? = null
    private var size: Int = 0

    constructor(clock: Clock?) {
        this.entries = Array<Entry?>(maxCapacity + 17) { null }
        this.clock = clock
    }

    constructor() : this(Clock.systemDefaultZone())

    fun put(key: Any?, value: Any?, retentionInMillis: Int) {
        val index = getIndexOf(key)
        val expiryTime = (clock?.millis() ?: 0) + retentionInMillis
        val entry = Entry(key, value, expiryTime)
        entry.next = entries?.get(index)
        entries?.set(index, entry)
        this.size++;

        // Check if the cache is full
        // FIFO eviction
        if (this.size() > maxCapacity) {
            // Remove the oldest entry
            removeOldest();
        }
    }

    fun isEmpty(): Boolean {
        return size() == 0
    }

    fun size(): Int {
        removeExpired();
        return size;
    }

    fun get(key: Any?): Any? {
        val index = getIndexOf(key)
        val now = clock?.millis()
        var e: Entry? = entries?.get(index)
        while (e != null) {
            if (e.key == key) {
                if (e.expiryTime < (now ?: 0)) {
                    // Remove the expired entry
                    removeExpired();
                    return null;
                }
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    private fun getIndexOf(key: Any?): Int {
        return (maxCapacity + key.hashCode() % maxCapacity)  % maxCapacity; // Avoid negative indices
    }

    private fun removeOldest() {
        var oldestTime: Long = Long.MAX_VALUE;
        var oldestIndex: Int = -1;
        var oldestPrev: Entry? = null;
        var oldest: Entry? = null;
        for (i in 0..maxCapacity) {
            var prev: Entry? = null;
            var e = entries?.get(i);
            while (e != null) {
                if (e.expiryTime < oldestTime) {
                    oldestTime = e.expiryTime;
                    oldestIndex = i;
                    oldestPrev = prev;
                    oldest = e;
                }
                prev = e;
                e = e.next;
            }
        }
        if (oldest != null) {
            if (oldestPrev != null) {
                oldestPrev.next = oldest.next;
            } else {
                entries?.set(oldestIndex, oldest.next);
            }
            this.size--;
        }
    }

    private fun removeExpired() {
        val now = clock?.millis() ?: 0
        for (i in 0..maxCapacity) {
            var prev: Entry? = null;
            var e = entries?.get(i);
            while (e != null) {
                if (e.expiryTime < now) {
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        entries?.set(i, e.next);
                    }
                    this.size--;
                }
                prev = e;
                e = e.next;
            }
        }
    }
}
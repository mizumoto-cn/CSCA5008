package io.collective

import java.time.Clock
import java.time.Instant

class SimpleAgedKache(private val clock: Clock = Clock.systemUTC()) {
    private var latestEntry: ExpirableEntry? = null

    fun put(key: Any, value: Any, retentionInMillis: Int) {
        val expirationTime = clock.instant().plusMillis(retentionInMillis.toLong())
        val entryToAdd = ExpirableEntry(key, value, expirationTime)

        if (latestEntry == null) {
            // Cache is empty
            latestEntry = entryToAdd
        } else {
            // Replace the latestEntry with the entryToAdd
            entryToAdd.next = latestEntry
            latestEntry = entryToAdd
        }
    }

    fun isEmpty(): Boolean {
        cleanupEntries()
        return latestEntry == null
    }

    fun size(): Int {
        cleanupEntries() // Remove expired entries
        var size = 0
        var current = latestEntry
        while (current != null) {
            // Navigate entries through next and increment count
            size++
            current = current.next
        }
        return size
    }

    fun get(key: Any): Any? {
        cleanupEntries() // Remove expired entries
        var current = latestEntry
        while (current != null) {
            if (current.key == key) {
                return current.value
            }
            current = current.next
        }
        return null
    }

    private fun cleanupEntries() {
        while (latestEntry != null && latestEntry!!.isExpired(clock.instant())) {
            latestEntry = latestEntry!!.next
        }

        if (latestEntry != null) {
            // Cycle through entries
            var current = latestEntry
            while (current?.next != null) {
                if (current.next!!.isExpired(clock.instant())) {
                    // Since next entry is expired, we update
                    // current.next reference to current.next.next to remove it
                    current.next = current.next!!.next
                } else {
                    // Not expired, so continue cycling
                    current = current.next
                }
            }
        }
    }

    private class ExpirableEntry(
            val key: Any,
            val value: Any,
            val expirationTime: Instant,
            var next: ExpirableEntry? = null
    ) {
        fun isExpired(now: Instant): Boolean {
            return expirationTime.isBefore(now)
        }
    }
}

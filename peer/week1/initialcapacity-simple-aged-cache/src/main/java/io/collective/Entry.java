package io.collective;

public class Entry {
    Object key;
    Object value;
    long expiryTime;
    Entry next;

    Entry(Object key, Object value, long expiryTime) {
        this.key = key;
        this.value = value;
        this.expiryTime = expiryTime;
    }
}

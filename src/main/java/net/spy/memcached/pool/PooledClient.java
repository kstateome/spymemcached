package net.spy.memcached.pool;

import net.spy.memcached.MemcachedClientDelegate;

public class PooledClient {
    long lastAccessed;
    MemcachedClientDelegate memcachedClientDelegate;

    public PooledClient(long lastAccessed, MemcachedClientDelegate memcachedClientDelegate) {
        this.lastAccessed = lastAccessed;
        this.memcachedClientDelegate = memcachedClientDelegate;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public MemcachedClientDelegate getMemcachedClientDelegate() {
        return memcachedClientDelegate;
    }

    public void setMemcachedClientDelegate(MemcachedClientDelegate memcachedClientDelegate) {
        this.memcachedClientDelegate = memcachedClientDelegate;
    }
}

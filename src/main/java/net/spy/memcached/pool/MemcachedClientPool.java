package net.spy.memcached.pool;

import net.spy.memcached.compat.log.LoggerFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;


public class MemcachedClientPool {
    private static final net.spy.memcached.compat.log.Logger LOG = LoggerFactory.getLogger(MemcachedClientPool.class);
    private GenericKeyedObjectPool pools;
    private static final String KEY = "MEMCACHED_KEY";

    public MemcachedClientPool(PooledFactory factory) {
        this.pools = new GenericKeyedObjectPool(factory,PoolConfig.getInstance());
    }

    public PooledClient getMemcachedClient(){
        try {
           return (PooledClient) pools.borrowObject(KEY);

        } catch (Exception e) {
            LOG.error("", e);
        }
        return null;
    }

    public void returnClient(PooledClient client){
        client.setLastAccessed(System.currentTimeMillis());
        try {
            pools.returnObject(KEY,client);
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    public void shutdown(){
        pools.clear();
    }
}

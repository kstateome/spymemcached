package net.spy.memcached.pool;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClientDelegate;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

import java.net.InetSocketAddress;
import java.util.List;

public class PooledFactory implements KeyedPoolableObjectFactory {
    ConnectionFactory cf;
    List<InetSocketAddress> addrs;
    private static final long TIME_TO_LIVE = 300000L;

    public PooledFactory(ConnectionFactory cf, List<InetSocketAddress> addrs) {
        this.cf = cf;
        this.addrs = addrs;
    }

    public Object makeObject(Object o) throws Exception {
       return  new PooledClient(System.currentTimeMillis(),new MemcachedClientDelegate(cf,addrs));
    }

    public void destroyObject(Object o, Object o2) throws Exception {
        PooledClient pooledClient = (PooledClient) o2;
        pooledClient = null;
    }

    public boolean validateObject(Object o, Object o2) {
        PooledClient pooledClient = (PooledClient) o2;
        return pooledClient.lastAccessed > (System.currentTimeMillis() - TIME_TO_LIVE);

    }

    public void activateObject(Object o, Object o2) throws Exception {

    }

    public void passivateObject(Object o, Object o2) throws Exception {

    }
}

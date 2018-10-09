/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2013 Couchbase, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached;

import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.pool.MemcachedClientPool;
import net.spy.memcached.pool.PooledClient;
import net.spy.memcached.pool.PooledFactory;
import net.spy.memcached.transcoders.Transcoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Client to a memcached server.
 *
 * <h2>Basic usage</h2>
 *
 * <pre>
 * MemcachedClient c = new MemcachedClient(
 *    new InetSocketAddress(&quot;hostname&quot;, portNum));
 *
 * // Store a value (async) for one hour
 * c.set(&quot;someKey&quot;, 3600, someObject);
 * // Retrieve a value.
 * Object myObject = c.get(&quot;someKey&quot;);
 * </pre>
 *
 * <h2>Advanced Usage</h2>
 *
 * <p>
 * MemcachedClient may be processing a great deal of asynchronous messages or
 * possibly dealing with an unreachable memcached, which may delay processing.
 * If a memcached is disabled, for example, MemcachedConnection will continue to
 * attempt to reconnect and replay pending operations until it comes back up. To
 * prevent this from causing your application to hang, you can use one of the
 * asynchronous mechanisms to time out a request and cancel the operation to the
 * server.
 * </p>
 *
 * <pre>
 *      // Get a memcached client connected to several servers
 *      // over the binary protocol
 *      MemcachedClient c = new MemcachedClient(new BinaryConnectionFactory(),
 *              AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
 *      // Try to get a value, for up to 5 seconds, and cancel if it
 *      // doesn't return
 *      Object myObj = null;
 *      Future&lt;Object&gt; f = c.asyncGet("someKey");
 *      try {
 *          myObj = f.get(5, TimeUnit.SECONDS);
 *      // throws expecting InterruptedException, ExecutionException
 *      // or TimeoutException
 *      } catch (Exception e) {  /*  /
 *          // Since we don't need this, go ahead and cancel the operation.
 *          // This is not strictly necessary, but it'll save some work on
 *          // the server.  It is okay to cancel it if running.
 *          f.cancel(true);
 *          // Do other timeout related stuff
 *      }
 * </pre>
 *
 * <p>Optionally, it is possible to activate a check that makes sure that
 * the node is alive and responding before running actual operations (even
 * before authentication. Only enable this if you are sure that you do not
 * run into issues during connection (some memcached services have problems
 * with it). You can enable it by setting the net.spy.verifyAliveOnConnect
 * System Property to "true".</p>
 */
public class MemcachedClient extends SpyObject implements MemcachedClientIF, ConnectionObserver {


    private MemcachedClientPool clientPool;

    public MemcachedClient(InetSocketAddress... ia) throws IOException {
        clientPool = new MemcachedClientPool(new PooledFactory(new DefaultConnectionFactory(), Arrays.asList(ia)));

    }

    /**
     * Get a memcache client over the specified memcached locations.
     *
     * @param addrs the socket addrs
     * @throws IOException if connections cannot be established
     */
    public MemcachedClient(List<InetSocketAddress> addrs) throws IOException {
        clientPool = new MemcachedClientPool(new PooledFactory(new DefaultConnectionFactory(), addrs));


    }

    /**
     * Used for testing.
     * @return
     */
    MemcachedClientDelegate getDelegate(){
        return clientPool.getMemcachedClient().getMemcachedClientDelegate();
    }
    /**
     * Get a memcache client over the specified memcached locations.
     *
     * @param cf    the connection factory to configure connections for this client
     * @param addrs the socket addresses
     * @throws IOException if connections cannot be established
     */
    public MemcachedClient(ConnectionFactory cf, List<InetSocketAddress> addrs)
            throws IOException {
        clientPool = new MemcachedClientPool(new PooledFactory(cf, addrs));
    }

    public Collection<SocketAddress> getAvailableServers() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getAvailableServers();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Collection<SocketAddress> getUnavailableServers() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getUnavailableServers();
        } finally {
            clientPool.returnClient(client);
        }

    }

    public NodeLocator getNodeLocator() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getNodeLocator();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Transcoder<Object> getTranscoder() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getTranscoder();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public CountDownLatch broadcastOp(final BroadcastOpFactory of) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().broadcastOp(of);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public CountDownLatch broadcastOp(final BroadcastOpFactory of, Collection<MemcachedNode> nodes) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().broadcastOp(of, nodes);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<Boolean> touch(final String key, final int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().touch(key, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<Boolean> touch(final String key, final int exp, final Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().touch(key, exp, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> append(long cas, String key, Object val) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().append(cas, key, val);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public OperationFuture<Boolean> append(String key, Object val) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().append(key, val);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<Boolean> append(long cas, String key, T val, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().append(cas, key, val, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<Boolean> append(String key, T val, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().append(key, val, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public OperationFuture<Boolean> prepend(long cas, String key, Object val) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().prepend(cas, key, val);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public OperationFuture<Boolean> prepend(String key, Object val) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().prepend(key, val);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().prepend(cas, key, val, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<Boolean> prepend(String key, T val, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().prepend(key, val, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncCAS(key, casId, value, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncCAS(key, casId, exp, value, tc);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public OperationFuture<CASResponse> asyncCAS(String key, long casId, Object value) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncCAS(key, casId, value);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, Object value) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncCAS(key, casId, exp, value);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> CASResponse cas(String key, long casId, T value, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().cas(key, casId, value, tc);

        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> CASResponse cas(String key, long casId, int exp, T value, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().cas(key, casId, exp, value, tc);

        } finally {
            clientPool.returnClient(client);
        }

    }

    public CASResponse cas(String key, long casId, Object value) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().cas(key, casId, value);
        } finally {
            clientPool.returnClient(client);
        }

    }

    public CASResponse cas(String key, long casId, int exp, Object value) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().cas(key, casId, exp, value);

        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().add(key, exp, o, tc);

        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> add(String key, int exp, Object o) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().add(key, exp, o);

        } finally {
            clientPool.returnClient(client);
        }

    }

    public <T> OperationFuture<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().set(key, exp, o, tc);

        } finally {
            clientPool.returnClient(client);
        }

    }

    public OperationFuture<Boolean> set(String key, int exp, Object o) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().set(key, exp, o);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<Boolean> replace(String key, int exp, T o, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().replace(key, exp, o, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> replace(String key, int exp, Object o) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().replace(key, exp, o);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> GetFuture<T> asyncGet(final String key, final Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGet(key, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public GetFuture<Object> asyncGet(final String key) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGet(key);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<CASValue<T>> asyncGets(final String key, final Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGets(key, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<CASValue<Object>> asyncGets(final String key) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGets(key);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().gets(key, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getAndTouch(key, exp, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public CASValue<Object> getAndTouch(String key, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getAndTouch(key, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public CASValue<Object> gets(String key) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().gets(key);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> T get(String key, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().get(key, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Object get(String key) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().get(key);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter, Iterator<Transcoder<T>> tcIter) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keyIter, tcIter);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Iterator<Transcoder<T>> tcIter) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keys, tcIter);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keyIter, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keys, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keyIter) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keyIter);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(tc, keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetBulk(keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<CASValue<Object>> asyncGetAndTouch(final String key, final int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetAndTouch(key, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> OperationFuture<CASValue<T>> asyncGetAndTouch(final String key, final int exp, final Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncGetAndTouch(key, exp, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> Map<String, T> getBulk(Iterator<String> keyIter, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(keyIter, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<String, Object> getBulk(Iterator<String> keyIter) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(keyIter);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(keys, tc);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<String, Object> getBulk(Collection<String> keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(tc, keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<String, Object> getBulk(String... keys) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getBulk(keys);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<SocketAddress, String> getVersions() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getVersions();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<SocketAddress, Map<String, String>> getStats() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getStats();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Map<SocketAddress, Map<String, String>> getStats(final String arg) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getStats(arg);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, long by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, int by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, long by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, int by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, long by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, int by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, long by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, int by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, long by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, int by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, long by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, int by) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, long by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, int by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, long by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, int by, long def, int exp) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by, def, exp);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, long by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncIncr(String key, int by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncIncr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, long by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Long> asyncDecr(String key, int by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().asyncDecr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, long by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long incr(String key, int by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().incr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, long by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long decr(String key, int by, long def) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().decr(key, by, def);
        } finally {
            clientPool.returnClient(client);
        }
    }
    @Deprecated
    public OperationFuture<Boolean> delete(String key, int hold) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().delete(key, hold);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> delete(String key) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().delete(key);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> delete(String key, long cas) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().delete(key, cas);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> flush(final int delay) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().flush(delay);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public OperationFuture<Boolean> flush() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().flush();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public Set<String> listSaslMechanisms() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().listSaslMechanisms();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public void shutdown() {
        clientPool.shutdown();
    }

    public boolean shutdown(long timeout, TimeUnit unit) {
        clientPool.shutdown();
        return true;

    }

    public boolean waitForQueues(long timeout, TimeUnit unit) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().waitForQueues(timeout, unit);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public boolean addObserver(ConnectionObserver obs) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().addObserver(obs);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public boolean removeObserver(ConnectionObserver obs) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().removeObserver(obs);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public void connectionEstablished(SocketAddress sa, int reconnectCount) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            client.getMemcachedClientDelegate().connectionEstablished(sa, reconnectCount);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public void connectionLost(SocketAddress sa) {
        PooledClient client = clientPool.getMemcachedClient();
        try {
             client.getMemcachedClientDelegate().connectionLost(sa);
        } finally {
            clientPool.returnClient(client);
        }
    }

    public long getOperationTimeout() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getOperationTimeout();
        } finally {
            clientPool.returnClient(client);
        }
    }

    public MemcachedConnection getConnection() {
        PooledClient client = clientPool.getMemcachedClient();
        try {
            return client.getMemcachedClientDelegate().getConnection();
        } finally {
            clientPool.returnClient(client);
        }
    }



}


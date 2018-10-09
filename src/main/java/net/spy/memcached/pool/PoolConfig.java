package net.spy.memcached.pool;

import net.spy.memcached.compat.log.LoggerFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PoolConfig extends GenericKeyedObjectPool.Config {
    private static final net.spy.memcached.compat.log.Logger LOG = LoggerFactory.getLogger(PoolConfig.class);
    private static PoolConfig INSTANCE = new PoolConfig();
    private final int defaultMaxIdle = 10;
    private final int defaultMaxActive = 20;
    private final boolean defaultTestOnBorrow = true;
    private final boolean defaultTestOnReturn = false;
    private final boolean defaultTestWhileIdle = true;

    public static PoolConfig getInstance() {
        return INSTANCE;
    }

    private PoolConfig() {
        Properties serverPoolProperties = null;
        try {
            serverPoolProperties = getServerPoolProperties();
        } catch (IOException e) {
            serverPoolProperties = null;
        }
        if (serverPoolProperties != null) {
            LOG.info("coping properties from file.");
            copyProperties(serverPoolProperties);
        } else {

            //original default values.
            super.testOnBorrow = defaultTestOnBorrow;
            super.testOnReturn = defaultTestOnReturn;
            super.testWhileIdle = defaultTestWhileIdle;
            super.maxIdle = defaultMaxIdle;
            super.maxActive = defaultMaxActive;
        }
    }

    private void copyProperties(Properties props) {
        String maxIdle = props.getProperty("maxIdle");
        String maxActive = props.getProperty("maxActive");
        String maxTotal = props.getProperty("maxTotal");
        String maxWait = props.getProperty("maxWait");
        String whenExhaustedAction = props.getProperty("whenExhaustedAction");
        String testOnBorrow = props.getProperty("testOnBorrow");
        String testOnReturn = props.getProperty("testOnReturn");
        String testWhileIdle = props.getProperty("testWhileIdle");
        String timeBetweenEvictionRunsMillis = props.getProperty("timeBetweenEvictionRunsMillis");
        String numTestsPerEvictionRun = props.getProperty("numTestsPerEvictionRun");
        String minEvictableIdleTimeMillis = props.getProperty("minEvictableIdleTimeMillis");
        String empty = "";
        if (maxIdle != null && !maxIdle.trim().equals(empty))
            super.maxIdle = Integer.parseInt(maxIdle);
        else
            super.maxIdle = defaultMaxIdle;

        if (maxActive != null && !maxActive.trim().equals(empty))
            super.maxActive = Integer.parseInt(maxActive);
        else
            super.maxActive = defaultMaxActive;
        if (maxTotal != null && !maxTotal.trim().equals(empty))
            super.maxTotal = Integer.parseInt(maxTotal);
        if (maxWait != null && !maxWait.trim().equals(empty))
            super.maxWait = Integer.parseInt(maxWait);
        if (whenExhaustedAction != null && !whenExhaustedAction.trim().equals(empty))
            super.whenExhaustedAction = Byte.parseByte(whenExhaustedAction);
        if (testOnBorrow != null && !testOnBorrow.trim().equals(empty)) {
            super.testOnBorrow = testOnBorrow.trim().equals("true");
        } else
            super.testOnBorrow = defaultTestOnBorrow;
        if (testOnReturn != null && !testOnReturn.trim().equals(empty)) {
            super.testOnReturn = testOnReturn.trim().equals("true");
        } else
            super.testOnReturn = defaultTestOnReturn;
        if (testWhileIdle != null && !testWhileIdle.trim().equals(empty)) {
            super.testWhileIdle = testWhileIdle.trim().equals("true");
        } else
            super.testWhileIdle = defaultTestWhileIdle;
        if (timeBetweenEvictionRunsMillis != null && !timeBetweenEvictionRunsMillis.trim().equals(empty)) {
            super.timeBetweenEvictionRunsMillis = Long.parseLong(timeBetweenEvictionRunsMillis);
        }
        if (numTestsPerEvictionRun != null && !numTestsPerEvictionRun.trim().equals(empty))
            super.numTestsPerEvictionRun = Integer.parseInt(numTestsPerEvictionRun);
        if (minEvictableIdleTimeMillis != null && !minEvictableIdleTimeMillis.trim().equals(empty)) {
            super.minEvictableIdleTimeMillis = Long.parseLong(minEvictableIdleTimeMillis);
        }

    }

    private Properties getServerPoolProperties() throws IOException {
        Properties props = new Properties();
        InputStream in = PoolConfig.class.getResourceAsStream("/serverPoolConfig.properties");
        if (in == null)
            return null;
        props.load(in);
        return props;
    }
}

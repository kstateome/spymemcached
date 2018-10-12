package net.spy.memcached;

import net.spy.memcached.compat.SpyObject;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import static net.spy.memcached.DefaultConnectionFactory.DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;

/**
 * This class is a Runnable that is designed to push a bit of data into
 * each SocketAddress configured for the MemcachedClient.  This does two things.
 * One it keeps the network connection alive to servers in which the TCP Keepalive is ignored
 * and the connection dies. If the connection dies, it attempts to send to the node until
 * the node is removed from the pool of available servers, or the connection works again.
 * This will minimize user impact of network failure.
 */
/**
 * This class is a Runnable that is designed to push a bit of data into
 * each SocketAddress configured for the MemcachedClient.  This does two things.
 * One it keeps the network connection alive to servers in which the TCP Keepalive is ignored
 * and the connection dies. If the connection dies, it attempts to send to the node until
 * the node is removed from the pool of available servers, or the connection works again.
 * This will minimize user impact of network failure.
 */
public class PingServer extends SpyObject implements Runnable {
    private static final String EMPTY_STRING = "";
    private static final String PRE_KEY = "spycache_";
    MemcachedClient client;
    Map<SocketAddress, String> addressMap;

    public PingServer(MemcachedClient client) {
        this.client = client;
        this.addressMap = new HashMap<SocketAddress, String>();
        for (MemcachedNode node : client.getNodeLocator().getAll()) {
            addressMap.put(node.getSocketAddress(), EMPTY_STRING);
        }

        findKeysForEachNode(client);
    }

    /**
     * Look for a key that will hit each node configured in the client.
     * This will iterate until a key is found for each, and store the key/SocketAddress in the
     * addressMap variable.
     *
     * @param client The memcached client to search for keys against.
     */
    private void findKeysForEachNode(MemcachedClient client) {
        int numberOfNodes = addressMap.size();
        int currentNode = 0;
        while (currentNode < numberOfNodes) {
            String generatedString = getRandomString();
            MemcachedNode node = client.getNodeLocator().getPrimary(PRE_KEY + generatedString);
            if (addressMap.get(node.getSocketAddress()).equals(EMPTY_STRING)) {
                addressMap.put(node.getSocketAddress(), PRE_KEY + generatedString);
                currentNode++;
            }
        }
    }

    /**
     * Generate a random string to use as a key for hitting
     * as specific memcachednode.  This method will return a random UUID.
     *
     * @return a random UUID String.
     */
    private String getRandomString() {
        return UUID.randomUUID().toString();
    }

    /**
     * For each SocketAddress that is still marked available, this method
     * will iterate and test each address to verify the network didn't die on the
     * address.
     */
    public void run() {
        Collection<SocketAddress> addresses = new LinkedList<SocketAddress>();
        addresses.addAll(client.getAvailableServers());
        getLogger().info("Begin memcached check");
        for (SocketAddress address : addresses) {
            getLogger().info("pinging:" + address.toString());
            testAddress(address);
        }
    }

    /**
     * Test to see if the address connection is still alive, and try it until its
     * removed from the list of availableServers or succeeds.
     *
     * @param address The SocketAddress to test.
     */
    private void testAddress(SocketAddress address) {
        boolean working = false;
        int tries = 0;
        while (client.getAvailableServers().contains(address) &&
                       !working &&
                       tries++ < DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD) {

            try {
                client.get(addressMap.get(address));
                working = true;
            } catch (OperationTimeoutException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (IllegalStateException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (Exception e){
                getLogger().info(EMPTY_STRING, e);
            }
        }

    }
}

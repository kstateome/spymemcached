package net.spy.memcached;

import net.spy.memcached.compat.SpyObject;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PingServer extends SpyObject implements Runnable {
    private static final String EMPTY_STRING = "";
    private static final String PRE_KEY = "key";
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

    private String getRandomString() {
        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }


    public void run() {

        Collection<SocketAddress> addresses = new LinkedList<SocketAddress>();
        addresses.addAll(client.getAvailableServers());
        getLogger().info("Begin memcached check");
        for (SocketAddress address : addresses) {
            try {
                getLogger().info("pinging:" + address.toString());
                testAddress(address);
            } catch (InterruptedException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (ExecutionException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (Exception e) {
                getLogger().info(EMPTY_STRING, e);
            }
        }


    }

    private void testAddress(SocketAddress address) throws InterruptedException, ExecutionException {
        while (client.getAvailableServers().contains(address) && !client.set(addressMap.get(address), 10, "test").get()) {
            // do nothing..
            // this should pull the node out of the availableServer list if connections stop working.
        }
    }
}

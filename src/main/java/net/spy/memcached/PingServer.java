package net.spy.memcached;

import net.spy.memcached.compat.SpyObject;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
       return UUID.randomUUID().toString();
    }


    public void run() {

        Collection<SocketAddress> addresses = new LinkedList<SocketAddress>();
        addresses.addAll(client.getAvailableServers());
        getLogger().info("Begin memcached check");
        for (SocketAddress address : addresses) {
                getLogger().info("pinging:" + address.toString());
                testAddress(address);

        }


    }

    private void testAddress(SocketAddress address)  {
        boolean working = false;
        while (client.getAvailableServers().contains(address) && !working) {
            try {
                working = client.set(addressMap.get(address), 10, "test").get();
            } catch (InterruptedException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (ExecutionException e) {
                getLogger().info(EMPTY_STRING, e);
            } catch (Exception e) {
                getLogger().info(EMPTY_STRING, e);
            }
        }
    }
}

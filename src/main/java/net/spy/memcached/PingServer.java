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
    MemcachedClient client;
    Map<SocketAddress, String> addressMap;

    public PingServer(MemcachedClient client) {
        this.client = client;
        this.addressMap = new HashMap<SocketAddress, String>();
        for (MemcachedNode node : client.getNodeLocator().getAll()) {
            addressMap.put(node.getSocketAddress(), "");
        }

        findKeysForEachNode(client);
    }

    private void findKeysForEachNode(MemcachedClient client) {
        int numberOfNodes = addressMap.size();
        int currentNode = 0;
        while (currentNode < numberOfNodes) {
            String generatedString = getRandomString();
            MemcachedNode node = client.getNodeLocator().getPrimary("key" + generatedString);
            if (addressMap.get(node.getSocketAddress()).equals("")) {
                addressMap.put(node.getSocketAddress(), "key" + generatedString);
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
        for (SocketAddress address : addresses) {
            try {
                testAddress(address);
            } catch (InterruptedException e) {
                getLogger().info("", e);
            } catch (ExecutionException e) {
                getLogger().info("", e);
            }
        }


    }

    private void testAddress(SocketAddress address) throws InterruptedException, ExecutionException {
        while (client.getAvailableServers().contains(address) && !client.set(addressMap.get(address), 10, "test").get()) {
            // do nothing..
            // this should pull the node out of the availableServer list if connections stop working.
        }
        ;

    }
}

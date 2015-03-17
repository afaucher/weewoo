package com.beanfarmergames.weewoo;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;

public class ServerBrowser implements Runnable {
    
    private final Client client;
    private Thread discoveryThread = null;
    
    //synchronized
    private Set<InetAddress> discoveredServers = new HashSet<InetAddress>();
    
    public ServerBrowser() {
        client = new Client();
        Kryo kryo = client.getKryo();
        client.start();
    }
    
    public void startDiscovery() {
        if (discoveryThread == null) {
            discoveryThread = new Thread(this);
        }
    }
    
    public InetAddress quickDiscovery() {
        return client.discoverHost(54777, 1000);
    }

    @Override
    public void run() {
        while (true) {
            List<InetAddress> hosts = client.discoverHosts(54777, 1000);
            if (hosts != null && hosts.size() > 0) {
                synchronized(discoveredServers) {
                    discoveredServers.addAll(hosts);
                }
            }
        }
    }
}

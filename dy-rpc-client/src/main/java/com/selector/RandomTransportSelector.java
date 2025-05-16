package com.selector;


import com.protocol.Peer;
import com.service.TransportClient;
import com.service.TransportSelector;
import com.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class RandomTransportSelector implements TransportSelector {
    private List<TransportClient> clients;
    public RandomTransportSelector() {
        clients=new ArrayList<>();
    }
    @Override
    public void init(List<Peer> peers, int count, Class<? extends TransportClient> clazz) {
        count=Math.max(count,1);
        for (Peer peer:peers){
            for (int i = 0; i < count; i++) {
                TransportClient transportClient= ReflectionUtils.newInstance(clazz);
                transportClient.init(peer);
                clients.add(transportClient);
            }
            log.info("connect server:{}",peer);
        }
    }

    @Override
    public synchronized TransportClient select() {
        int i= new Random().nextInt(clients.size());
        return clients.remove(i);
    }

    @Override
    public synchronized void release(TransportClient client) {
        clients.add(client);
    }

    @Override
    public synchronized void close() {
        for(TransportClient client:clients){
            client.close();
        }
        clients.clear();
    }
}

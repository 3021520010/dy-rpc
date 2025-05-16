package com.registry;

import com.service.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisServiceRegistry implements ServiceRegistry {
    //redis连接池
    private final JedisPool jedisPool;
    //心跳线程池
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final String KEY_PREFIX = "rpc:service:";
    private static final int HEARTBEAT_INTERVAL = 30; // 心跳间隔（秒）
    private static final int SERVICE_TIMEOUT = 90; // 服务超时时间（秒）

    public RedisServiceRegistry(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PREFIX + serviceName;
            String value = address.getHostString() + ":" + address.getPort();
            jedis.hset(key, value, String.valueOf(System.currentTimeMillis()));
            
            // 启动心跳
            startHeartbeat(serviceName, address);
        } catch (Exception e) {
            log.error("Failed to register service: " + serviceName, e);
        }
    }

    @Override
    public void unregister(String serviceName, InetSocketAddress address) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PREFIX + serviceName;
            String value = address.getHostString() + ":" + address.getPort();
            jedis.hdel(key, value);
        } catch (Exception e) {
            log.error("Failed to unregister service: " + serviceName, e);
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PREFIX + serviceName;
            Map<String, String> instances = jedis.hgetAll(key);
            List<InetSocketAddress> addresses = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            
            for (Map.Entry<String, String> entry : instances.entrySet()) {
                String address = entry.getKey();
                String lastHeartbeat = entry.getValue();
                
                // 检查服务是否超时
                if (currentTime - Long.parseLong(lastHeartbeat) > SERVICE_TIMEOUT * 1000) {
                    // 服务已超时，从注册表中删除
                    jedis.hdel(key, address);
                    log.warn("Service {} at {} has timed out, removing from registry", serviceName, address);
                    continue;
                }
                
                String[] parts = address.split(":");
                addresses.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
            }
            
            return addresses;
        } catch (Exception e) {
            log.error("Failed to lookup service: " + serviceName, e);
            return new ArrayList<>();
        }
    }

    private void startHeartbeat(String serviceName, InetSocketAddress address) {
        scheduler.scheduleAtFixedRate(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = KEY_PREFIX + serviceName;
                String value = address.getHostString() + ":" + address.getPort();
                jedis.hset(key, value, String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                log.error("Failed to send heartbeat for service: " + serviceName, e);
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    public void close() {
        scheduler.shutdown();
        jedisPool.close();
    }
} 
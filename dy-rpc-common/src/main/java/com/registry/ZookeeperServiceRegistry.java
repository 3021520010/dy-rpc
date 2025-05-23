package com.registry;

import com.service.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry {

    private static final String ROOT_PATH = "/rpc";
    private final CuratorFramework client;

    public ZookeeperServiceRegistry(String host, int port) {
        String zkAddress = host + ":" + port;
        client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(15000)
                .connectionTimeoutMs(1000*60)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
    }

    /**
     * 注册服务：创建临时有序节点，节点内容为服务地址
     */
    @Override
    public void register(String serviceName, InetSocketAddress address) {
        try {
            String servicePath = ROOT_PATH + "/" + serviceName;
            // 创建服务根路径（持久节点）
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(servicePath);
            }
            // 创建临时顺序节点
            String addressPath = servicePath + "/instance-";
            String addressData = address.getHostString() + ":" + address.getPort();

            String nodePath = client.create()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(addressPath, addressData.getBytes());

            log.info("Registered service instance: {} with address: {}", nodePath, addressData);
        } catch (Exception e) {
            log.error("Failed to register service: " + serviceName, e);
        }
    }

    /**
     * 注销服务：删除对应临时节点（可选，一般会话断开自动删除）
     */
    @Override
    public void unregister(String serviceName, InetSocketAddress address) {
        try {
            String servicePath = ROOT_PATH + "/" + serviceName;
            List<String> children = client.getChildren().forPath(servicePath);
            String addressData = address.getHostString() + ":" + address.getPort();
            for (String child : children) {
                String fullPath = servicePath + "/" + child;
                byte[] data = client.getData().forPath(fullPath);
                if (addressData.equals(new String(data))) {
                    client.delete().forPath(fullPath);
                    log.info("Unregistered service instance: {}", fullPath);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to unregister service: " + serviceName, e);
        }
    }

    /**
     * 服务发现：获取某服务所有存活实例地址
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        try {
            String servicePath = ROOT_PATH + "/" + serviceName;
            if (client.checkExists().forPath(servicePath) == null) {
                log.warn("No such service registered: {}", serviceName);
                return addresses;
            }
            List<String> children = client.getChildren().forPath(servicePath);
            for (String child : children) {
                byte[] data = client.getData().forPath(servicePath + "/" + child);
                String addressStr = new String(data);
                String[] parts = addressStr.split(":");
                addresses.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
            }
        } catch (Exception e) {
            log.error("Failed to lookup service: " + serviceName, e);
        }
        return addresses;
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        client.close();
    }
}

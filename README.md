## 🌱项目简介：dy-rpc 轻量级自研 RPC 框架

**`dy-rpc`** 是一个参考 **Dubbo** 和 **Netty** 设计理念，面向学习和实战的轻量级 RPC 框架。项目融合了 **Java NIO、并发编程、Spring Boot 自动装配、SPI 插件机制、RPC 原理** 等核心知识，支持高性能服务通信与灵活的扩展机制。

### ✅ 核心特性

- **注册中心**：
  - 支持 **Redis**（基于心跳机制）和 **Zookeeper**（临时节点）作为服务注册与发现组件。
- **负载均衡**：
  - 支持 **全局 / 局部节点选择**
  - 策略包含：**随机、轮询、一致性哈希**
  - 可通过注解 + SPI 插件机制灵活扩展
- **序列化方式**：
  - 内置支持：`JSON`、`Hessian`、`Kryo`，通过配置可切换
- **服务端通信模型**：
  - 采用 **Boss-Worker 架构**，基于 **Java NIO + Selector** 实现事件驱动模型
  - 另提供 **BIO、TCP、HTTP** 示例实现供学习参考
- **客户端连接池**：
  - 自研连接池，支持连接复用，避免频繁创建/销毁，提高性能
- **协议与传输**：
  - 自定义协议结构：**4字节报文长度 + 数据体**
  - 有效解决粘包/半包问题，支持高并发传输
- **容错机制**：
  - 支持通过注解配置 **调用重试次数与间隔时间**
- **Spring Boot 集成**：
  - 通过注解 + 自动装配实现服务发布与消费
  - 注册中心、负载均衡策略、序列化方式等均可通过配置文件灵活切换

## 📦项目架构


📁 dy-rpc-client                 # RPC 客户端实现  
📁 dy-rpc-code                   # 通信消息结构与序列化机制（请求/响应等）  
📁 dy-rpc-common                 # 通用工具类、接口定义、负载均衡等核心组件  
📁 dy-rpc-protocol               # 通信协议处理（含协议解析与封装）  
📁 dy-rpc-server                 # RPC 服务端实现逻辑  
📁 dy-rpc-transport              # 底层通信实现（支持 BIO / NIO / TCP / HTTP）  

📁 dy-rpc-example-service        # 通用服务接口定义（客户端与服务端共享）  

📁 dy-rpc-spring-boot-starter    # Spring Boot 启动器，提供注解与自动配置支持  
📁 dy-rpc-spring-boot-client     # 用于测试的 Spring Boot 客户端示例项目  
📁 dy-rpc-spring-boot-server     # 用于测试的 Spring Boot 服务端示例项目  

## 🔧技术栈

| 分类     | 技术框架 / 工具                         |
| -------- | --------------------------------------- |
| 编程语言 | Java 17                                 |
| 通信模型 | Java NIO（Selector）、BIO、TCP、HTTP    |
| 注册中心 | Redis、Zookeeper                        |
| 序列化   | JSON、Hessian、Kryo                     |
| 负载均衡 | 随机、轮询、一致性哈希（支持 SPI 扩展） |
| 容错机制 | 自定义注解 + 重试策略                   |
| 连接池   | 自研 NIO 异步连接池                     |
| 协议设计 | 自定义 RPC 协议（4字节长度 + 数据体）   |
| 框架集成 | Spring Boot Starter 自动装配            |
| 构建工具 | Maven                                   |
| 日志     | SLF4J + Logback                         |

## 🚀 快速开始

项目启动时自动装配主要在dy-rpc-spring-boot-starter的autoxxx类中，在项目启动时对bean和注解进行扫描，实现自动装配



### 服务端使用方式

```java
package com.dy.dyrpcspringbootserver.service.impl;

import com.annotation.RpcService;

import com.service.Test;
import org.springframework.stereotype.Service;

@RpcService(interfaceClass = Test.class)
@Service
public class TestImpl implements Test {
    @Override
    public int add(int a, int b) {
        return a+b;
    }
}

```

### 客户端使用方式

```
 @RpcReference(interfaceClass = Test.class, retryCount = 2,retryTime = 500)
 private  Test test;
 @RequestMapping("/test")
public void test(){
         System.out.println(test.add(1,2));
}
```


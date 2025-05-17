# dy-prc1

#### 介绍
dy-prc1 是一个主要用来进行学习网络传输原理以及RCP原理的 自研的轻量级 RPC 框架，支持注解方式集成至 Spring Boot 项目中，底层通信支持 TCP/HTTP、多种传输模型（BIO/NIO），具备可扩展的服务注册、负载均衡与编码解码机制。

## 📦 项目结构
├── dy-rpc-client # RPC 客户端实现
├── dy-rpc-code # 通信消息结构与序列化机制
├── dy-rpc-common # 通用工具、接口定义、负载均衡等
├── dy-rpc-protocol # 通信协议处理
├── dy-rpc-server # RPC 服务端实现
├── dy-rpc-transport # 底层通信（BIO/NIO/TCP/HTTP）
├── dy-rpc-spring-boot-starter # 提供服务端注解注册支持
├── dy-rpc-spring-boot-client # 提供客户端注解消费支持


## 🚀 快速开始

### 服务端使用方式

```java
@RpcService(interfaceClass = HelloService.class)
public class HelloServiceImpl implements HelloService {
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
### 客户端使用方式
@RpcReference(interfaceClass = HelloService.class)
private HelloService helloService;





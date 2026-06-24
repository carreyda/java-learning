# 2-02 · Spring Boot 入门

> **目标**：用 Spring Initializr 创建第一个 Spring Boot 项目，理解自动装配原理，运行第一个能访问的 Web 接口。  
> **预计时间**：3-4 小时  
> **前置要求**：完成 2-01，理解 Maven 基础

---

## 目录

- [2-02 · Spring Boot 入门](#2-02--spring-boot-入门)
  - [目录](#目录)
  - [1. Spring 与 Spring Boot 的关系](#1-spring-与-spring-boot-的关系)
  - [2. 创建第一个 Spring Boot 项目](#2-创建第一个-spring-boot-项目)
    - [2.1 使用 Spring Initializr（推荐）](#21-使用-spring-initializr推荐)
    - [2.2 第一次启动](#22-第一次启动)
  - [3. 项目结构解析](#3-项目结构解析)
    - [生成的 pom.xml](#生成的-pomxml)
  - [4. Spring Boot 核心机制](#4-spring-boot-核心机制)
    - [4.1 @SpringBootApplication](#41-springbootapplication)
    - [4.2 自动装配原理（了解即可）](#42-自动装配原理了解即可)
    - [4.3 Starter 机制](#43-starter-机制)
  - [5. application.yml 配置](#5-applicationyml-配置)
    - [YAML 语法要点](#yaml-语法要点)
    - [多环境配置](#多环境配置)
  - [6. 写第一个接口](#6-写第一个接口)
  - [7. 热重载（可选）](#7-热重载可选)
  - [8. 和 Node.js/Express 的对比](#8-和-nodejsexpress-的对比)
  - [练习](#练习)
    - [练习 1：创建项目并启动](#练习-1创建项目并启动)
    - [练习 2：写基础接口](#练习-2写基础接口)
    - [练习 3：配置文件实践](#练习-3配置文件实践)
    - [练习 4：多环境配置](#练习-4多环境配置)
  - [常见问题](#常见问题)
  - [下一步](#下一步)

---

## 1. Spring 与 Spring Boot 的关系

**Spring Framework：** Java 最流行的应用框架，提供 IoC 容器、AOP、事务管理、MVC 等核心功能。强大但配置繁琐（大量 XML 或 Java Config）。

**Spring Boot：** Spring 官方推出的"脚手架"，基于 Spring Framework，主要解决配置繁琐的问题：

```
Spring Framework：
  你说清楚要什么，框架帮你做 → 但需要写很多配置

Spring Boot：
  约定大于配置 → 合理的默认值，不满意再覆盖
  自动装配    → 引入 starter，相关 Bean 自动创建
  内嵌服务器  → 不需要单独安装 Tomcat，直接 java -jar 运行
```

**类比：**

| 类比                               | 说明                               |
| ---------------------------------- | ---------------------------------- |
| Spring Framework ≈ Vue             | 核心框架，功能强大但需要自己搭架子 |
| Spring Boot ≈ Nuxt.js / Vite + Vue | 在框架基础上提供开箱即用的脚手架   |

---

## 2. 创建第一个 Spring Boot 项目

### 2.1 使用 Spring Initializr（推荐）

**方式一：在线创建**

1. 访问 [https://start.spring.io](https://start.spring.io)
2. 填写配置：
   - Project：Maven
   - Language：Java
   - Spring Boot：3.2.x（选最新稳定版）
   - Group：`com.example`
   - Artifact：`blog-api`
   - Package name：`com.example.blogapi`
   - Java：17
3. 添加依赖：搜索并选择 `Spring Web`
4. 点击 GENERATE，下载 zip，解压后用 IDEA 打开

**方式二：IDEA 内直接创建（推荐）**

1. File → New → Project
2. 选择 Spring Initializr
3. 填写同上配置
4. 勾选依赖：`Spring Web`
5. Finish

### 2.2 第一次启动

打开 `src/main/java/com/example/blogapi/BlogApiApplication.java`：

```java
package com.example.blogapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApiApplication.class, args);
    }
}
```

点击 `main` 方法左侧的绿色三角，或右键 → Run。

控制台看到以下内容说明启动成功：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.0)

... Started BlogApiApplication in 1.234 seconds (JVM running for 1.891)
```

---

## 3. 项目结构解析

```
blog-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/blogapi/
    │   │   └── BlogApiApplication.java     ← 启动类
    │   └── resources/
    │       ├── application.properties       ← 默认配置文件（建议改为 .yml）
    │       ├── static/                      ← 静态资源（图片、JS、CSS）
    │       └── templates/                   ← 模板文件（Thymeleaf 等）
    └── test/
        └── java/com/example/blogapi/
            └── BlogApiApplicationTests.java ← 自动生成的测试类
```

### 生成的 pom.xml

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.0</version>
</parent>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 无需写版本，由 parent 统一管理 -->
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## 4. Spring Boot 核心机制

### 4.1 @SpringBootApplication

这个注解是三合一的组合：

```java
@SpringBootApplication
// 等价于同时标注了：
@SpringBootConfiguration   // 表明这是配置类
@EnableAutoConfiguration   // 开启自动装配
@ComponentScan             // 扫描当前包及子包下的组件
```

### 4.2 自动装配原理（了解即可）

```
引入 spring-boot-starter-web
  ↓
spring-boot-autoconfigure 扫描 META-INF/spring/...AutoConfiguration.imports
  ↓
发现 WebMvcAutoConfiguration（满足条件：classpath 有 Spring MVC 相关类）
  ↓
自动创建 DispatcherServlet、ViewResolver 等 Bean
  ↓
内嵌 Tomcat 自动启动，监听 8080 端口
```

**核心思想：** 引入 starter 依赖 → Spring Boot 检测到相关类在 classpath → 按约定自动完成配置。你不需要手动声明这些 Bean。

### 4.3 Starter 机制

Starter 是一组预定义依赖的集合，命名规律：

```
官方 starter：spring-boot-starter-{功能}
  spring-boot-starter-web        → Spring MVC + 内嵌 Tomcat
  spring-boot-starter-data-jpa   → JPA + Hibernate
  spring-boot-starter-security   → Spring Security
  spring-boot-starter-data-redis → Redis
  spring-boot-starter-test       → JUnit + Mockito

第三方 starter：{技术名}-spring-boot-starter
  mybatis-plus-spring-boot3-starter
  knife4j-openapi3-jakarta-spring-boot-starter
```

---

## 5. application.yml 配置

把默认的 `application.properties` 重命名为 `application.yml`（YAML 格式更简洁，是主流选择）：

```yaml
# 服务器配置
server:
  port: 8080 # 端口（默认就是 8080）
  servlet:
    context-path: /api # 接口前缀，所有接口都以 /api 开头

# Spring 配置
spring:
  application:
    name: blog-api # 应用名称

  # 数据源（现在先注释掉，2-08 再配）
  # datasource:
  #   url: jdbc:mysql://localhost:3306/blog_db
  #   username: root
  #   password: 123456
  #   driver-class-name: com.mysql.cj.jdbc.Driver

  # Jackson JSON 配置
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null # 不序列化 null 字段

# 日志配置
logging:
  level:
    root: info # 全局日志级别
    com.example.blogapi: debug # 自己包的日志级别更详细
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### YAML 语法要点

```yaml
# 键值对（冒号后必须有空格）
key: value

# 嵌套（用缩进，不能用 Tab，只能用空格）
server:
  port: 8080
  address: localhost

# 列表
allowed-origins:
  - http://localhost:3000
  - http://localhost:5173

# 多行字符串
description: |
  这是一段
  多行文字

# 引用（Spring Boot 支持 ${} 占位符）
app:
  name: blog-api
  full-name: ${app.name}-service
```

### 多环境配置

```yaml
# application.yml（公共配置）
spring:
  application:
    name: blog-api

---
# 开发环境
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://localhost:3306/blog_dev

---
# 生产环境
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-server:3306/blog_prod
```

激活方式：

```yaml
# 激活 dev 环境
spring:
  profiles:
    active: dev
```

---

## 6. 写第一个接口

在 `src/main/java/com/example/blogapi/` 下新建 `controller/HelloController.java`：

```java
package com.example.blogapi.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController                    // = @Controller + @ResponseBody
@RequestMapping("/hello")          // 该 Controller 下所有接口的前缀
public class HelloController {

    // GET /hello
    @GetMapping
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // GET /hello/info
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "app", "blog-api",
            "version", "1.0.0",
            "time", LocalDateTime.now().toString()
        );
    }

    // GET /hello/greet?name=张三
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }

    // GET /hello/user/42
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        return Map.of("id", id, "name", "用户" + id);
    }

    // POST /hello/echo
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        return Map.of("received", body, "echo", true);
    }
}
```

启动应用后，打开浏览器或 Postman：

```bash
GET  http://localhost:8080/hello
# → Hello, Spring Boot!

GET  http://localhost:8080/hello/info
# → {"app":"blog-api","version":"1.0.0","time":"2026-06-..."}

GET  http://localhost:8080/hello/greet?name=张三
# → Hello, 张三!

GET  http://localhost:8080/hello/user/42
# → {"id":42,"name":"用户42"}

POST http://localhost:8080/hello/echo
Body: {"message": "test", "count": 3}
# → {"received":{"message":"test","count":3},"echo":true}
```

---

## 7. 热重载（可选）

每次修改代码都要重启服务，很麻烦。添加 `spring-boot-devtools` 实现热重载：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
```

IDEA 还需要配置：

1. Settings → Build, Execution, Deployment → Compiler → 勾选 `Build project automatically`
2. Settings → Advanced Settings → 勾选 `Allow auto-make to start even if developed application is currently running`

修改代码后 Ctrl+S 保存，几秒后自动重启。

---

## 8. 和 Node.js/Express 的对比

用 Express 和 Spring Boot 实现同样的功能对比：

```javascript
// Node.js + Express
const express = require("express");
const app = express();
app.use(express.json());

app.get("/hello", (req, res) => {
  res.send("Hello, Express!");
});

app.get("/hello/greet", (req, res) => {
  const name = req.query.name || "World";
  res.send(`Hello, ${name}!`);
});

app.get("/hello/user/:id", (req, res) => {
  res.json({ id: req.params.id, name: `用户${req.params.id}` });
});

app.post("/hello/echo", (req, res) => {
  res.json({ received: req.body, echo: true });
});

app.listen(3000, () => console.log("Server running on port 3000"));
```

```java
// Spring Boot
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        return Map.of("id", id, "name", "用户" + id);
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        return Map.of("received", body, "echo", true);
    }
}
```

**主要差异：**

| 对比       | Spring Boot               | Express/Node.js            |
| ---------- | ------------------------- | -------------------------- |
| 路由定义   | 注解（@GetMapping）       | 函数调用（app.get）        |
| 请求参数   | 注解声明（@RequestParam） | `req.query`                |
| 路径参数   | 注解声明（@PathVariable） | `req.params`               |
| 请求体     | 注解声明（@RequestBody）  | `req.body`                 |
| 响应       | 直接返回对象，自动序列化  | `res.json()`、`res.send()` |
| 启动服务器 | 内嵌 Tomcat，自动启动     | `app.listen()`             |
| 类型安全   | 编译时检查                | 运行时才知道               |

Spring Boot 更"重"，但注解声明式的风格让代码更清晰，类型安全也减少了运行时错误。

---

## 练习

### 练习 1：创建项目并启动

1. 用 Spring Initializr 创建一个 Spring Boot 项目（groupId: `com.yourname`，artifactId: `hello-spring`，依赖选 Spring Web）
2. 把 `application.properties` 改为 `application.yml`，把端口改为 `8090`
3. 启动项目，确认控制台输出 `Started HelloSpringApplication`

### 练习 2：写基础接口

在项目中创建 `controller/PracticeController.java`，实现以下接口：

1. `GET /practice/ping` → 返回字符串 `"pong"`
2. `GET /practice/add?a=3&b=4` → 返回两数之和（返回 JSON：`{"a":3,"b":4,"sum":7}`）
3. `GET /practice/repeat/{word}/{times}` → 把 word 重复 times 次返回
4. `POST /practice/upper` → 接收 `{"text": "hello world"}`，返回 `{"original": "hello world", "upper": "HELLO WORLD"}`

用 Postman 或浏览器测试所有接口。

### 练习 3：配置文件实践

1. 在 `application.yml` 中添加自定义配置：
   ```yaml
   app:
     name: 我的博客
     version: 1.0.0
     max-articles: 100
   ```
2. 在 Controller 中用 `@Value("${app.name}")` 注入这些配置值，并提供一个接口 `GET /practice/config` 返回这些配置信息

### 练习 4：多环境配置

1. 在 `application.yml` 中配置 dev 和 prod 两套环境
2. dev 环境端口 8080，prod 环境端口 9090
3. 提供一个接口 `GET /practice/env`，返回当前激活的环境名称（用 `@Value("${spring.profiles.active:default}")`）
4. 分别激活 dev 和 prod，验证端口不同

---

## 常见问题

**Q：启动报错 `Port 8080 was already in use`？**  
A：8080 端口被占用，两个方案：① 在 `application.yml` 里改 `server.port: 8090`；② 找到占用 8080 的进程并关闭（`netstat -ano | findstr :8080`，然后 `taskkill /PID xxx /F`）。

**Q：接口返回中文乱码？**  
A：在 `application.yml` 添加：

```yaml
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

**Q：`@RestController` 和 `@Controller` 的区别？**  
A：`@Controller` 返回视图名（用于模板渲染），`@RestController = @Controller + @ResponseBody`，方法的返回值直接序列化为 JSON（或 String）写入响应体。做 REST API 统一用 `@RestController`。

**Q：Spring Boot 内嵌的是什么服务器？**  
A：默认内嵌 Tomcat。可以换成 Jetty 或 Undertow（性能略有差异），在 pom.xml 中排除 Tomcat 并引入对应 starter。生产中大多数场景 Tomcat 够用。

**Q：`@RequestMapping` 和 `@GetMapping` 有什么关系？**  
A：`@GetMapping("/path")` 是 `@RequestMapping(method = RequestMethod.GET, value = "/path")` 的简写。同样有 `@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping`，分别对应各 HTTP 方法。

---

## 下一步

下一节 **[2-03 · IoC 与依赖注入](../2-03/README.md)** 深入理解 Spring 最核心的概念——IoC 容器和依赖注入，这是理解所有 Spring 注解背后机制的关键。

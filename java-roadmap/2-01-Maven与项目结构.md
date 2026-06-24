# 2-01 · Maven 与项目结构

> **目标**：理解 Maven 的核心概念，掌握 pom.xml 的编写，能创建标准 Maven 项目并管理依赖。  
> **预计时间**：2-3 小时  
> **前置要求**：完成阶段一，IDEA 已安装

---

## 目录

- [Maven 是什么](#1-maven-是什么)
- [安装与配置](#2-安装与配置)
- [Maven 项目结构](#3-maven-项目结构)
- [pom.xml 详解](#4-pomxml-详解)
- [依赖管理](#5-依赖管理)
- [Maven 生命周期与常用命令](#6-maven-生命周期与常用命令)
- [在 IDEA 中使用 Maven](#7-在-idea-中使用-maven)
- [和 npm 的对比](#8-和-npm-的对比)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. Maven 是什么

Maven 是 Java 生态最主流的**项目构建和依赖管理工具**，类似于前端的 npm/pnpm。

**解决的核心问题：**

```
没有 Maven 之前：
  - 手动下载 jar 包，拷贝到项目里
  - jar 包版本冲突、依赖的依赖找不到
  - 项目构建步骤全靠手动（编译→测试→打包）

有了 Maven：
  - 在 pom.xml 声明依赖，Maven 自动下载
  - 依赖关系自动解析，传递依赖自动引入
  - 一条命令完成编译、测试、打包、部署
```

**Maven vs Gradle：**

| 对比     | Maven                  | Gradle                            |
| -------- | ---------------------- | --------------------------------- |
| 配置文件 | XML（pom.xml）         | Groovy/Kotlin DSL（build.gradle） |
| 学习曲线 | 平缓，约定大于配置     | 稍陡，更灵活                      |
| 国内使用 | 更主流（特别是老项目） | Spring Boot 官方推荐              |
| 构建速度 | 较慢                   | 更快（增量构建）                  |

阶段二使用 Maven，这是国内 Java 后端岗位最常见的选择。

---

## 2. 安装与配置

### 2.1 IDEA 内置 Maven（推荐）

IDEA 自带 Maven，无需额外安装。使用 IDEA 创建项目时会自动使用内置 Maven。

### 2.2 单独安装 Maven（可选）

如果需要在命令行使用 `mvn` 命令：

1. 下载：[https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)，选择 Binary zip archive
2. 解压到 `D:\tools\apache-maven-3.9.x`
3. 配置环境变量：`MAVEN_HOME = D:\tools\apache-maven-3.9.x`，`Path` 添加 `%MAVEN_HOME%\bin`
4. 验证：`mvn -version`

### 2.3 配置国内镜像（必做）

Maven 默认从国外中央仓库下载，国内很慢。修改 Maven 的 `settings.xml`：

**IDEA 内置 Maven 的 settings.xml 位置：**

- IDEA 菜单 → Settings → Build → Maven → User settings file，点击右侧文件夹图标找到位置
- 通常在 `C:\Users\用户名\.m2\settings.xml`（没有就新建）

**settings.xml 内容：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <!-- 本地仓库位置（jar 包缓存目录） -->
  <localRepository>C:\Users\用户名\.m2\repository</localRepository>

  <mirrors>
    <!-- 阿里云镜像 -->
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>jdk-17</id>
      <activation>
        <activeByDefault>true</activeByDefault>
        <jdk>17</jdk>
      </activation>
      <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <maven.compiler.compilerVersion>17</maven.compiler.compilerVersion>
      </properties>
    </profile>
  </profiles>
</settings>
```

---

## 3. Maven 项目结构

Maven 规定了标准目录结构（约定大于配置）：

```
my-project/
├── pom.xml                          ← 项目描述文件（核心）
├── src/
│   ├── main/
│   │   ├── java/                    ← 主代码
│   │   │   └── com/example/
│   │   │       ├── Application.java
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       └── model/
│   │   └── resources/               ← 配置文件、静态资源
│   │       ├── application.yml
│   │       └── mapper/              ← MyBatis XML 映射文件
│   └── test/
│       ├── java/                    ← 测试代码
│       └── resources/               ← 测试配置
└── target/                          ← 编译输出目录（自动生成，不提交 Git）
    ├── classes/
    └── my-project-1.0.jar
```

**和前端项目的对比：**

| Maven 结构           | 前端对应                    |
| -------------------- | --------------------------- |
| `pom.xml`            | `package.json`              |
| `src/main/java`      | `src/`                      |
| `src/main/resources` | `public/` 或 `assets/`      |
| `src/test/java`      | `__tests__/` 或 `*.spec.ts` |
| `target/`            | `dist/`                     |
| `~/.m2/repository`   | `node_modules/`（全局缓存） |

---

## 4. pom.xml 详解

pom.xml（Project Object Model）是 Maven 项目的核心配置文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <!-- Maven POM 版本，固定写 4.0.0 -->
  <modelVersion>4.0.0</modelVersion>

  <!-- ===== 项目坐标（GAV）===== -->
  <!-- groupId：组织/公司反转域名 -->
  <groupId>com.example</groupId>
  <!-- artifactId：项目名 -->
  <artifactId>my-blog</artifactId>
  <!-- version：版本号，SNAPSHOT 表示开发中 -->
  <version>1.0.0-SNAPSHOT</version>
  <!-- packaging：打包方式，jar（默认）/ war / pom -->
  <packaging>jar</packaging>

  <!-- 项目描述信息（可选）-->
  <name>My Blog</name>
  <description>基于 Spring Boot 的博客系统</description>

  <!-- ===== 父项目（Spring Boot 推荐方式）===== -->
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
  </parent>

  <!-- ===== 全局属性 ===== -->
  <properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- 自定义属性，可以在 dependencies 中用 ${} 引用 -->
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
  </properties>

  <!-- ===== 依赖声明 ===== -->
  <dependencies>

    <!-- Spring Boot Web（包含 Spring MVC、内嵌 Tomcat）-->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <!-- 版本由 parent 统一管理，无需写 version -->
    </dependency>

    <!-- MyBatis Plus -->
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>  <!-- 只在运行时需要 -->
    </dependency>

    <!-- Lombok（简化 getter/setter/toString 等）-->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>  <!-- 不传递给依赖方 -->
    </dependency>

    <!-- Spring Boot 测试 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>  <!-- 只在测试时需要 -->
    </dependency>

  </dependencies>

  <!-- ===== 构建插件 ===== -->
  <build>
    <plugins>
      <!-- Spring Boot Maven 插件：打可执行 jar -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <!-- 排除 Lombok，避免打包进最终 jar -->
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>

</project>
```

---

## 5. 依赖管理

### 5.1 依赖坐标（GAV）

每个 Maven 依赖由三个坐标唯一确定：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>   <!-- 组织 -->
  <artifactId>spring-boot-starter-web</artifactId>  <!-- 项目名 -->
  <version>3.2.0</version>                      <!-- 版本 -->
</dependency>
```

在 [https://mvnrepository.com](https://mvnrepository.com) 搜索任意库，可以找到 GAV 坐标和对应的 XML 片段。

### 5.2 依赖范围（scope）

| scope             | 编译 | 测试 | 运行 | 说明                           |
| ----------------- | ---- | ---- | ---- | ------------------------------ |
| `compile`（默认） | ✅   | ✅   | ✅   | 最常用，打包时包含             |
| `test`            | ❌   | ✅   | ❌   | 只用于测试，如 JUnit           |
| `runtime`         | ❌   | ✅   | ✅   | 运行时需要，如 MySQL 驱动      |
| `provided`        | ✅   | ✅   | ❌   | 运行环境已提供，如 Servlet API |

### 5.3 依赖传递

A 依赖 B，B 依赖 C，则 A 也自动拥有 C（传递依赖）。

```
spring-boot-starter-web
  ├── spring-webmvc
  │     ├── spring-context
  │     │     └── spring-beans
  │     └── spring-web
  ├── spring-boot-starter-tomcat
  │     └── tomcat-embed-core
  └── ...
```

引入 `spring-boot-starter-web` 一个依赖，实际引入了几十个 jar。

### 5.4 依赖冲突与排除

当不同依赖引入了同一个库的不同版本时出现冲突：

```xml
<!-- 排除某个传递依赖 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<!-- 手动指定版本（覆盖传递依赖的版本）-->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.16.0</version>
</dependency>
```

### 5.5 dependencyManagement（统一版本管理）

在多模块项目中，用 `dependencyManagement` 统一管理版本，子模块只需声明 GAV 无需写版本：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>3.5.5</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## 6. Maven 生命周期与常用命令

Maven 定义了三套生命周期，最常用的是 **default** 生命周期：

```
validate → compile → test → package → verify → install → deploy

validate：验证项目配置正确
compile ：编译 src/main/java
test    ：运行 src/test/java 中的测试
package ：打成 jar/war 包，放到 target/
install ：把 jar 安装到本地仓库（~/.m2）
deploy  ：上传到远程仓库
```

**执行某个阶段会自动执行它之前的所有阶段。**

### 常用命令

```bash
# 编译
mvn compile

# 跳过测试打包（最常用）
mvn package -DskipTests

# 清理 target 目录再打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn install -DskipTests

# 只运行测试
mvn test

# 查看依赖树（排查冲突时用）
mvn dependency:tree

# 分析未使用/缺少的依赖
mvn dependency:analyze
```

---

## 7. 在 IDEA 中使用 Maven

### 7.1 创建 Maven 项目

**方式一：直接用 Spring Initializr（推荐，下一节会用到）**

- File → New → Project → Spring Initializr

**方式二：手动创建**

- File → New → Project → Maven Archetype → maven-archetype-quickstart

### 7.2 IDEA Maven 面板

IDEA 右侧有 Maven 面板，常用功能：

```
Maven 面板
├── Lifecycle（生命周期）
│   ├── clean     → 清理 target
│   ├── compile   → 编译
│   ├── package   → 打包
│   └── install   → 安装到本地仓库
├── Plugins（插件）
└── Dependencies（依赖树，可视化查看）
```

双击 Lifecycle 中的命令即可执行，等同于命令行 `mvn xxx`。

### 7.3 刷新依赖

修改 `pom.xml` 后，点击右上角弹出的 **刷新图标**（或 Maven 面板右上角的刷新按钮），IDEA 会自动下载新依赖。

### 7.4 常用快捷操作

```
Ctrl + Shift + Alt + S  → Project Structure（查看依赖）
Maven 面板 → Dependencies → 展开查看传递依赖
pom.xml 中 Alt + Insert → 添加依赖（有代码提示）
```

---

## 8. 和 npm 的对比

| 概念       | Maven                                          | npm/pnpm                       |
| ---------- | ---------------------------------------------- | ------------------------------ |
| 配置文件   | `pom.xml`                                      | `package.json`                 |
| 依赖声明   | `<dependencies>`                               | `"dependencies"`               |
| 安装依赖   | `mvn install`（自动）                          | `npm install`                  |
| 运行脚本   | `mvn package`                                  | `npm run build`                |
| 本地缓存   | `~/.m2/repository`                             | `~/.npm` / `node_modules`      |
| 版本锁文件 | 无（靠版本号约束）                             | `package-lock.json`            |
| 依赖仓库   | Maven Central                                  | npmjs.com                      |
| 国内镜像   | 阿里云 Maven 镜像                              | npmmirror.com                  |
| 查找依赖   | [mvnrepository.com](https://mvnrepository.com) | [npmjs.com](https://npmjs.com) |

**主要差异：**

npm 的 `node_modules` 每个项目独立一份，Maven 的依赖缓存在 `~/.m2/repository` 全局共享，不同项目复用同一份 jar 包。

```javascript
// package.json
{
  "name": "my-app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "mysql2": "^3.0.0"
  },
  "devDependencies": {
    "jest": "^29.0.0"
  }
}
```

```xml
<!-- pom.xml -->
<project>
  <groupId>com.example</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0.0</version>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>  <!-- 对应 devDependencies -->
    </dependency>
  </dependencies>
</project>
```

---

## 练习

### 练习 1：创建第一个 Maven 项目

1. 在 IDEA 中创建一个 Maven 项目（不用 Spring Initializr），groupId 填 `com.yourname`，artifactId 填 `maven-demo`
2. 查看自动生成的目录结构，对照本节说明理解每个目录的作用
3. 在 `pom.xml` 中添加 `fastjson2` 依赖（去 mvnrepository.com 搜索最新版本）
4. 在 Main 类中用 fastjson2 序列化一个对象为 JSON 字符串并打印，验证依赖引入成功

### 练习 2：配置国内镜像

1. 按照本节说明配置阿里云镜像到 `settings.xml`
2. 删除本地仓库中的某个已存在的 jar（`~/.m2/repository/...`），重新 `mvn compile`，观察是否从阿里云快速下载

### 练习 3：依赖分析

1. 在 pom.xml 中添加 `spring-boot-starter-web`
2. 执行 `mvn dependency:tree`（或在 Maven 面板展开 Dependencies），找出：
   - 哪个依赖引入了 Tomcat？
   - 哪个依赖引入了 Jackson（JSON 处理）？
   - 共引入了多少个 jar？

### 练习 4：生命周期实践

1. 在 maven-demo 项目中写一个简单的单元测试（用 JUnit 5）
2. 依次执行 `mvn compile`、`mvn test`、`mvn package`，观察每步的输出和 target 目录变化
3. 在 package 时加 `-DskipTests` 跳过测试，对比两次打包的差异

---

## 常见问题

**Q：pom.xml 修改后依赖没有下载？**  
A：点击 pom.xml 右上角出现的大象图标（或 Maven 面板的刷新按钮）。如果还是没有，检查网络或镜像配置是否正确。

**Q：`~/.m2/repository` 能删掉吗？**  
A：可以，删掉后下次构建会重新下载所有依赖。磁盘空间不足时可以定期清理长期不用的旧版本。

**Q：SNAPSHOT 版本是什么意思？**  
A：SNAPSHOT（快照）表示开发中的不稳定版本，每次构建都可能不同。Release 版本（如 `1.0.0`）是稳定发布版，内容固定。自己的项目开发阶段用 SNAPSHOT，发布时去掉 SNAPSHOT。

**Q：Maven 和 Gradle 以后会用哪个？**  
A：这套路线用 Maven，国内大多数公司老项目也是 Maven。Gradle 在 Android 开发和新 Spring Boot 项目中更常见。两者核心概念相通，学会一个后另一个很快上手。

**Q：`<optional>true</optional>` 是什么意思？**  
A：标记依赖为可选，不会传递给依赖此项目的其他项目。Lombok 通常这样标记——只有开发者自己需要，库的使用者不需要。

---

## 下一步

下一节 **[2-02 · Spring Boot 入门](../2-02/README.md)** 用 Spring Initializr 创建第一个 Spring Boot 项目，理解自动装配和 starter 机制，运行第一个 Web 服务。

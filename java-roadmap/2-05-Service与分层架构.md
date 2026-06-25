# 2-05 · Service 与分层架构

> **目标**：理解三层架构的设计原则，掌握 Service 层的职责划分，学会接口与实现类的标准写法，能设计清晰的业务逻辑层。  
> **预计时间**：3-4 小时  
> **前置要求**：完成 2-04，熟悉 Controller 层开发

---

## 目录

- [三层架构概述](#1-三层架构概述)
- [各层的职责划分](#2-各层的职责划分)
- [Service 层标准写法](#3-service-层标准写法)
- [DTO 与 Entity 的转换](#4-dto-与-entity-的转换)
- [Lombok 简化代码](#5-lombok-简化代码)
- [事务基础](#6-事务基础)
- [完整示例：用户模块](#7-完整示例用户模块)
- [和 Node.js 分层的对比](#8-和-nodejs-分层的对比)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. 三层架构概述

Spring Boot 后端项目通常采用三层架构：

```
请求
  ↓
Controller 层（表现层）
  ↓  调用
Service 层（业务逻辑层）
  ↓  调用
Repository/DAO 层（数据访问层）
  ↓  操作
数据库
```

每一层只与相邻层交互，不跨层调用：

```
✅ Controller → Service → Repository
❌ Controller → Repository（跨层，破坏分层）
❌ Repository → Service（逆向调用）
```

### 为什么要分层？

```
没有分层：
  一个类里既写 HTTP 参数解析，又写业务逻辑，又写 SQL
  → 难以维护，难以测试，难以复用

分层之后：
  Controller：只管 HTTP 相关（参数绑定、状态码、格式）
  Service：只管业务逻辑（规则、计算、编排）
  Repository：只管数据操作（增删改查）

  → 每层职责单一，可以独立测试
  → Service 可以被多个 Controller 复用
  → 换数据库只改 Repository 层，其他层不动
```

---

## 2. 各层的职责划分

### Controller 层的职责

```
✅ 应该做：
  - 接收 HTTP 请求，绑定参数
  - 调用 Service 完成业务
  - 把结果包装成统一响应格式返回
  - 处理 HTTP 层面的事情（状态码、Content-Type）

❌ 不应该做：
  - 直接操作数据库
  - 写业务逻辑（条件判断、计算）
  - 直接访问 Repository
```

```java
// ✅ 好的 Controller：只做请求处理和响应封装
@PostMapping
public Result<UserVO> createUser(@RequestBody @Valid CreateUserDTO dto) {
    UserVO user = userService.createUser(dto);
    return Result.ok(user);
}

// ❌ 坏的 Controller：混入了业务逻辑
@PostMapping
public Result<User> createUser(@RequestBody CreateUserDTO dto) {
    // 这些业务逻辑不该在 Controller 里
    if (userRepository.existsByUsername(dto.getUsername())) {
        return Result.fail(400, "用户名已存在");
    }
    User user = new User();
    user.setUsername(dto.getUsername());
    user.setPasswordHash(bCryptPasswordEncoder.encode(dto.getPassword()));
    userRepository.save(user);
    return Result.ok(user);
}
```

### Service 层的职责

```
✅ 应该做：
  - 实现业务规则和流程
  - 参数校验（业务层面的，不是格式校验）
  - 调用 Repository 获取数据
  - 编排多个 Repository 的调用
  - 抛出业务异常
  - 管理事务

❌ 不应该做：
  - 处理 HTTP 请求（不依赖 HttpServletRequest 等）
  - 直接操作 JSON 序列化
  - 知道自己被哪个 Controller 调用
```

### Repository 层的职责

```
✅ 应该做：
  - 封装所有数据库操作
  - 返回 Entity 对象或基本类型
  - 写 SQL / HQL / MyBatis Mapper

❌ 不应该做：
  - 写业务逻辑
  - 调用其他 Repository（复杂查询可以，但要谨慎）
  - 返回 HTTP 相关的对象
```

---

## 3. Service 层标准写法

### 3.1 接口 + 实现类模式

```java
// 接口：定义行为契约
package com.example.blogapi.service;

public interface UserService {
    UserVO createUser(CreateUserDTO dto);
    UserVO getUserById(Long id);
    PageResult<UserVO> listUsers(Integer page, Integer size);
    UserVO updateUser(Long id, UpdateUserDTO dto);
    void deleteUser(Long id);
}
```

```java
// 实现类：具体业务逻辑
package com.example.blogapi.service.impl;

@Service  // 注册为 Bean
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 构造器注入
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserVO createUser(CreateUserDTO dto) {
        // 业务校验
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(400, "用户名已被占用：" + dto.getUsername());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册：" + dto.getEmail());
        }

        // 创建实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);

        // 保存
        User saved = userRepository.save(user);

        // 转换为 VO 返回
        return UserVO.from(saved);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("用户", id));
        return UserVO.from(user);
    }

    // ... 其他方法
}
```

### 3.2 是否必须写接口？

**实际开发中的做法：**

```
需要写接口的场景：
  - 有多个实现（如本地存储 vs OSS 存储）
  - 需要 Mock 测试（单元测试替换实现）
  - 作为 SDK/库 对外暴露

不一定需要接口的场景：
  - 确定只有一个实现的普通 Service
  - 小项目快速开发
```

Spring Boot 项目中，如果用 MyBatis Plus，Repository 层通常不写接口（直接继承 BaseMapper），Service 层视情况而定。本路线后续以**接口 + 实现类**为准，这是大厂标准写法。

---

## 4. DTO 与 Entity 的转换

### 4.1 各种对象的定义

```
Entity（实体）：对应数据库表的 Java 对象，字段和数据库列一一对应
DTO（Data Transfer Object）：数据传输对象，用于接收客户端请求数据
VO（View Object）：视图对象，用于返回给客户端，过滤掉敏感字段
```

```java
// Entity：数据库映射
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;   // 敏感字段，不能直接返回
    private LocalDateTime createdAt;
    private UserStatus status;
}

// CreateUserDTO：接收创建请求
@Data
public class CreateUserDTO {
    @NotBlank(message = "用户名不能为空")
    @Length(min = 3, max = 20, message = "用户名长度 3-20 字符")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 字符")
    private String password;
}

// UpdateUserDTO：接收更新请求（字段可选）
@Data
public class UpdateUserDTO {
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;
    private String avatar;
}

// UserVO：返回给客户端（不含密码）
@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private LocalDateTime createdAt;
    private String status;

    // 工厂方法：从 Entity 转换
    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setStatus(user.getStatus().name());
        return vo;
    }
}
```

### 4.2 为什么不直接返回 Entity？

```java
// ❌ 直接返回 Entity 的问题：
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    return Result.ok(userRepository.findById(id).get());
    // 问题1：passwordHash 被序列化返回给客户端（安全漏洞！）
    // 问题2：数据库字段变化直接影响接口（耦合）
    // 问题3：可能触发懒加载，序列化报错
}

// ✅ 返回 VO：
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    return Result.ok(userService.getUserById(id));
    // UserVO 只包含要返回的字段，安全可控
}
```

---

## 5. Lombok 简化代码

Lombok 通过注解在编译时自动生成样板代码（getter/setter/构造器等），大幅减少冗余代码。

### 5.1 添加依赖

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

IDEA 需要安装 Lombok 插件（通常已预装）：Settings → Plugins → 搜索 Lombok → Install。

### 5.2 常用注解

```java
@Data                  // = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
@Getter                // 为所有字段生成 getter
@Setter                // 为所有字段生成 setter
@ToString              // 生成 toString()
@EqualsAndHashCode     // 生成 equals() 和 hashCode()
@NoArgsConstructor     // 生成无参构造
@AllArgsConstructor    // 生成全参构造
@RequiredArgsConstructor  // 为 final 字段生成构造（与 Spring 构造器注入完美配合）
@Builder               // 生成 Builder 模式
@Slf4j                 // 注入 log 变量（org.slf4j.Logger）
```

### 5.3 实际使用

```java
// 没有 Lombok：需要写大量样板代码
public class User {
    private Long id;
    private String username;
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
    // ... equals/hashCode
}

// 有 Lombok：简洁清爽
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    // 编译时自动生成所有 getter/setter/toString/equals/hashCode
}
```

```java
// @RequiredArgsConstructor + 构造器注入
@Service
@RequiredArgsConstructor   // 自动为 final 字段生成构造方法
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;    // 自动注入
    private final PasswordEncoder passwordEncoder;  // 自动注入

    // 不需要手写构造方法
}
```

```java
// @Builder 模式
@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String email;

    // 使用
    UserVO vo = UserVO.builder()
        .id(1L)
        .username("张三")
        .email("zs@example.com")
        .build();
}
```

```java
// @Slf4j 日志
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    public UserVO createUser(CreateUserDTO dto) {
        log.info("创建用户：{}", dto.getUsername());
        log.debug("用户详情：{}", dto);
        // ...
        log.info("用户创建成功，id={}", saved.getId());
    }
}
```

---

## 6. 事务基础

### 6.1 什么是事务

事务保证一组数据库操作**要么全部成功，要么全部回滚**：

```java
// 场景：转账
// 1. A 账户减 1000
// 2. B 账户加 1000

// 如果步骤1成功，步骤2失败 → 钱凭空消失！
// 事务保证：步骤1失败 → 步骤2不执行；步骤2失败 → 步骤1回滚
```

### 6.2 @Transactional 使用

```java
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;

    // 添加 @Transactional：方法内的所有数据库操作在同一个事务中
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId)
            .orElseThrow(() -> new ResourceNotFoundException("账户", fromId));
        Account to = accountRepository.findById(toId)
            .orElseThrow(() -> new ResourceNotFoundException("账户", toId));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足");
        }

        // 两个更新操作在同一事务中
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);  // 如果这里失败，上面的 save 也会回滚

        log.info("转账成功：{} → {}，金额：{}", fromId, toId, amount);
    }
}
```

### 6.3 常用属性

```java
// 只读事务（查询优化，不能修改数据）
@Transactional(readOnly = true)
public List<UserVO> listUsers() { ... }

// 出现特定异常时才回滚（默认只有 RuntimeException 触发回滚）
@Transactional(rollbackFor = Exception.class)
public void importData() throws IOException { ... }

// 出现特定异常时不回滚
@Transactional(noRollbackFor = BusinessException.class)
public void process() { ... }
```

> ⚠️ **@Transactional 常见陷阱：**
>
> 1. 只能用在 `public` 方法上（private/protected 方法上的 @Transactional 无效）
> 2. 同类内部调用不触发事务（`this.methodB()` 不走代理）
> 3. 默认只回滚 `RuntimeException`，受检异常需要加 `rollbackFor = Exception.class`

---

## 7. 完整示例：用户模块

把前面的知识串起来，看完整的分层代码：

### 目录结构

```
com.example.blogapi/
├── controller/
│   └── UserController.java
├── service/
│   ├── UserService.java              ← 接口
│   └── impl/
│       └── UserServiceImpl.java      ← 实现
├── repository/
│   └── UserRepository.java
├── model/
│   ├── entity/
│   │   └── User.java                 ← 数据库实体
│   └── dto/
│       ├── CreateUserDTO.java
│       ├── UpdateUserDTO.java
│       └── UserVO.java
└── common/
    ├── Result.java
    ├── BusinessException.java
    └── GlobalExceptionHandler.java
```

### Controller 层

```java
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<List<UserVO>> list() {
        return Result.ok(userService.listAll());
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<UserVO> create(@RequestBody @Valid CreateUserDTO dto) {
        return Result.ok(userService.createUser(dto));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateUserDTO dto) {
        return Result.ok(userService.updateUser(id, dto));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
```

### Service 接口

```java
public interface UserService {
    List<UserVO> listAll();
    UserVO getUserById(Long id);
    UserVO createUser(CreateUserDTO dto);
    UserVO updateUser(Long id, UpdateUserDTO dto);
    void deleteUser(Long id);
}
```

### Service 实现（暂时用内存模拟，2-11 接入真实数据库）

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 暂时用 Map 模拟数据库（2-11 替换为真实 Repository）
    private final Map<Long, User> db = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<UserVO> listAll() {
        return db.values().stream()
            .map(UserVO::from)
            .collect(Collectors.toList());
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = db.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户", id);
        }
        return UserVO.from(user);
    }

    @Override
    public UserVO createUser(CreateUserDTO dto) {
        // 用户名唯一性校验
        boolean usernameExists = db.values().stream()
            .anyMatch(u -> u.getUsername().equals(dto.getUsername()));
        if (usernameExists) {
            throw new BusinessException(400, "用户名已存在：" + dto.getUsername());
        }

        User user = new User();
        user.setId(idGen.getAndIncrement());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setCreatedAt(LocalDateTime.now());

        db.put(user.getId(), user);
        log.info("用户创建成功：id={}, username={}", user.getId(), user.getUsername());

        return UserVO.from(user);
    }

    @Override
    public UserVO updateUser(Long id, UpdateUserDTO dto) {
        User user = db.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户", id);
        }

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());

        db.put(id, user);
        return UserVO.from(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!db.containsKey(id)) {
            throw new ResourceNotFoundException("用户", id);
        }
        db.remove(id);
        log.info("用户删除成功：id={}", id);
    }
}
```

---

## 8. 和 Node.js 分层的对比

```javascript
// Node.js 分层（Express + 手动分层）
// controllers/userController.js
const userService = require("../services/userService");

exports.createUser = async (req, res, next) => {
  try {
    const user = await userService.createUser(req.body);
    res.status(201).json({ code: 200, data: user });
  } catch (err) {
    next(err);
  }
};

// services/userService.js
const userRepository = require("../repositories/userRepository");

exports.createUser = async (dto) => {
  const exists = await userRepository.existsByUsername(dto.username);
  if (exists) throw new Error("用户名已存在");
  return userRepository.save(dto);
};
```

```java
// Spring Boot 分层（框架强制+注解驱动）
// Controller 通过 @RestController 自动注册
// Service 通过 @Service 自动注册
// 通过构造器注入，IoC 容器自动组装
// 事务通过 @Transactional 声明
// 异常通过 @RestControllerAdvice 统一处理
```

| 对比     | Spring Boot           | Node.js                      |
| -------- | --------------------- | ---------------------------- |
| 分层约束 | 框架通过注解强制      | 手动约定，靠规范             |
| 依赖注入 | 容器自动完成          | 手动 require                 |
| 事务管理 | @Transactional 声明式 | 手动写 begin/commit/rollback |
| 日志     | @Slf4j 自动注入       | 手动引入 winston/pino        |
| 参数校验 | @Valid 声明式         | 手动或用 joi/zod             |

---

## 练习

### 练习 1：完善用户模块

基于本节的完整示例，继续完善：

1. 添加 `@Valid` 参数校验（在 DTO 上加 `@NotBlank`、`@Email` 等注解，在 Controller 参数上加 `@Valid`）
2. 在 `GlobalExceptionHandler` 中处理 `MethodArgumentNotValidException`，返回具体的字段错误信息
3. 测试：发送缺少必填字段的请求，验证返回 400 + 错误描述

### 练习 2：文章模块（独立完成）

独立设计并实现一个文章（Article）模块，包含：

**字段：** id、title、content、authorId、status（DRAFT/PUBLISHED）、createdAt、updatedAt

**接口：**

- `GET /api/articles` — 列表（支持按 status 过滤）
- `GET /api/articles/{id}` — 详情
- `POST /api/articles` — 创建
- `PUT /api/articles/{id}` — 更新
- `POST /api/articles/{id}/publish` — 发布（把 status 改为 PUBLISHED）
- `DELETE /api/articles/{id}` — 删除

要求：完整三层分层、统一响应格式、全局异常处理、Lombok 注解、接口+实现类。

### 练习 3：Lombok 实践

1. 在一个 Entity 类上同时使用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`
2. 发现 `@Data` 和 `@Builder` 一起用时有什么问题（提示：`@Builder` 会覆盖无参构造，导致 Jackson 反序列化失败）
3. 找到解决方案并修复

### 练习 4（进阶）：分页响应

设计一个通用分页结果类 `PageResult<T>`：

```java
@Data
public class PageResult<T> {
    private List<T> records;   // 当前页数据
    private long total;        // 总记录数
    private int page;          // 当前页码
    private int size;          // 每页大小
    private int totalPages;    // 总页数

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) { ... }
}
```

在 `ArticleService` 的 `list` 方法中实现分页逻辑（用内存数据模拟），`GET /api/articles?page=1&size=5` 返回分页结果。

---

## 常见问题

**Q：Service 一定要写接口吗？**  
A：不是强制的，但推荐。接口让 Service 和实现解耦，便于后续替换实现（如从内存换到数据库），也便于单元测试（可以 Mock 接口）。如果是小项目或确定不需要替换，可以直接用类。

**Q：DTO、VO、Entity 的命名有没有统一规范？**  
A：没有强制标准，各公司有自己的约定。常见的有：`XxxRequest`/`XxxResponse`（以请求/响应为视角）、`XxxDTO`/`XxxVO`（以传输对象为视角）、`XxxParam`/`XxxResult`。保持项目内一致即可。

**Q：Lombok 的 `@Data` 会有什么问题？**  
A：`@Data` 生成的 `equals` 和 `hashCode` 基于所有字段，在实体类中可能有问题（如循环引用导致 `toString` 栈溢出）。JPA 实体类推荐不用 `@Data`，改为单独加 `@Getter`、`@Setter`，手动重写 `equals`/`hashCode`（只基于 id 字段）。

**Q：`@Transactional` 加在接口上还是实现类上？**  
A：建议加在**实现类**上（或具体方法上）。加在接口上理论上可以，但 Spring 的文档不推荐，因为不同代理机制下行为可能不同（JDK 动态代理 vs CGLIB）。

---

## 下一步

下一节 **[2-06 · 请求验证与异常处理](../2-06/README.md)** 深入学习 Spring Validation 的各种校验注解，以及如何优雅地把校验结果集成进统一异常处理体系。

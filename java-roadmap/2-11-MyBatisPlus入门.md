# 2-11 · MyBatis Plus 入门

> **目标**：在 Spring Boot 中接入 MySQL，用 MyBatis Plus 完成基本 CRUD 操作，掌握 BaseMapper、条件构造器和分页插件的核心用法。  
> **预计时间**：4-5 小时  
> **前置要求**：完成 2-10，MySQL 已启动，建表脚本已执行

---

## 目录

- [MyBatis Plus 简介](#1-mybatis-plus-简介)
- [集成配置](#2-集成配置)
- [实体类映射](#3-实体类映射)
- [BaseMapper：基础 CRUD](#4-basemapper基础-crud)
- [条件构造器](#5-条件构造器)
- [分页插件](#6-分页插件)
- [IService 与 ServiceImpl](#7-iservice-与-serviceimpl)
- [完整示例：用户模块接入数据库](#8-完整示例用户模块接入数据库)
- [和 Sequelize 的对比](#9-和-sequelize-的对比)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. MyBatis Plus 简介

MyBatis Plus（MP）是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强，不做改变：

```
MyBatis（原版）：
  - 需要手写 Mapper 接口 + XML 映射文件（或注解）
  - 每个实体类的 CRUD 都要重复写

MyBatis Plus：
  - 继承 BaseMapper<T> 即获得所有 CRUD 方法
  - 内置分页、逻辑删除、自动填充等功能
  - 提供条件构造器，告别拼接 SQL 字符串
  - 复杂 SQL 仍然可以用 XML 或注解扩展
```

---

## 2. 集成配置

### 2.1 添加依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- MyBatis Plus（Spring Boot 3 专用版本）-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.5</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 2.2 数据源配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 连接池配置（Spring Boot 默认使用 HikariCP）
    hikari:
      maximum-pool-size: 10 # 最大连接数
      minimum-idle: 5 # 最小空闲连接
      connection-timeout: 30000 # 连接超时（毫秒）
      idle-timeout: 600000 # 空闲超时（毫秒）

# MyBatis Plus 配置
mybatis-plus:
  # 映射文件位置（XML 方式时用到）
  mapper-locations: classpath*:mapper/**/*.xml
  # 实体类包路径
  type-aliases-package: com.example.blogapi.model.entity
  configuration:
    # 下划线转驼峰（数据库 user_name → Java userName）
    map-underscore-to-camel-case: true
    # 打印 SQL 日志（开发时开启，生产关闭）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略：AUTO=数据库自增
      id-type: auto
      # 逻辑删除字段名
      logic-delete-field: isDeleted
      # 逻辑删除值（1=已删除）
      logic-delete-value: 1
      # 逻辑未删除值
      logic-not-delete-value: 0
```

### 2.3 启动类扫描 Mapper

```java
@SpringBootApplication
@MapperScan("com.example.blogapi.mapper")  // 扫描 Mapper 接口
public class BlogApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApiApplication.class, args);
    }
}
```

---

## 3. 实体类映射

```java
package com.example.blogapi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")  // 对应数据库表名（默认按类名驼峰转下划线，User → user）
public class User {

    @TableId(type = IdType.AUTO)   // 主键，数据库自增
    private Long id;

    private String username;
    private String email;
    private String passwordHash;   // 自动映射到 password_hash（map-underscore-to-camel-case=true）
    private String nickname;
    private String avatar;
    private String bio;
    private String role;

    private Integer status;

    @TableLogic                    // 标记逻辑删除字段
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)         // 插入时自动填充
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时自动填充
    private LocalDateTime updatedAt;
}
```

### 常用注解说明

| 注解                           | 说明                                           |
| ------------------------------ | ---------------------------------------------- |
| `@TableName("表名")`           | 指定对应的数据库表名                           |
| `@TableId(type = IdType.AUTO)` | 标记主键，AUTO=数据库自增                      |
| `@TableField("列名")`          | 指定对应的列名（不同名时使用）                 |
| `@TableField(exist = false)`   | 标记该字段不是数据库列（如关联查询的临时字段） |
| `@TableLogic`                  | 逻辑删除字段                                   |
| `@TableField(fill = ...)`      | 自动填充（需配合处理器使用）                   |
| `@Version`                     | 乐观锁版本字段                                 |

### 自动填充处理器

```java
package com.example.blogapi.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

## 4. BaseMapper：基础 CRUD

### 4.1 定义 Mapper 接口

```java
package com.example.blogapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blogapi.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 即获得所有基础 CRUD 方法
    // 如需自定义 SQL，可以在这里添加方法
}
```

### 4.2 BaseMapper 提供的方法

```java
// 注入后直接使用
@Autowired
private UserMapper userMapper;

// ===== 插入 =====
int insert(T entity);

// ===== 删除 =====
int deleteById(Serializable id);           // 按主键删除（逻辑删除）
int deleteBatchIds(Collection<?> ids);     // 批量删除
int delete(Wrapper<T> queryWrapper);       // 按条件删除

// ===== 更新 =====
int updateById(T entity);                  // 按主键更新（null 字段不更新）
int update(T entity, Wrapper<T> updateWrapper); // 按条件更新

// ===== 查询 =====
T selectById(Serializable id);             // 按主键查询
List<T> selectBatchIds(Collection<?> ids); // 批量查询
List<T> selectList(Wrapper<T> queryWrapper); // 按条件查询列表
T selectOne(Wrapper<T> queryWrapper);      // 查询单个（结果多于1条抛异常）
Long selectCount(Wrapper<T> queryWrapper); // 统计数量
IPage<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper); // 分页查询
```

### 4.3 基础使用示例

```java
// 插入
User user = new User();
user.setUsername("newuser");
user.setEmail("new@example.com");
user.setPasswordHash("$2a$...");
userMapper.insert(user);
// insert 后，user.getId() 自动被填充

// 按 ID 查询
User found = userMapper.selectById(1L);

// 按 ID 更新（只更新非 null 字段）
User update = new User();
update.setId(1L);
update.setNickname("新昵称");
userMapper.updateById(update);  // 只更新 nickname，其他字段不变

// 按 ID 删除（逻辑删除：UPDATE SET is_deleted=1）
userMapper.deleteById(1L);

// 批量查询
List<User> users = userMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L));
```

---

## 5. 条件构造器

条件构造器（Wrapper）是 MyBatis Plus 最核心的特性，用链式 API 代替手动拼接 SQL：

### 5.1 QueryWrapper（查询条件）

```java
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

// SELECT * FROM user WHERE status = 1 AND is_deleted = 0 ORDER BY created_at DESC
List<User> users = userMapper.selectList(
    new QueryWrapper<User>()
        .eq("status", 1)
        .orderByDesc("created_at")
);

// 常用条件方法
QueryWrapper<User> wrapper = new QueryWrapper<User>()
    .eq("status", 1)                          // = 等于
    .ne("role", "ADMIN")                      // != 不等于
    .gt("id", 10)                             // > 大于
    .ge("id", 10)                             // >= 大于等于
    .lt("id", 100)                            // < 小于
    .le("id", 100)                            // <= 小于等于
    .like("nickname", "张")                   // LIKE '%张%'
    .likeRight("username", "z")               // LIKE 'z%'（前缀匹配，能用索引）
    .in("status", Arrays.asList(0, 1))        // IN (0, 1)
    .between("created_at", start, end)        // BETWEEN
    .isNull("avatar")                         // IS NULL
    .isNotNull("avatar")                      // IS NOT NULL
    .orderByDesc("created_at")               // ORDER BY created_at DESC
    .orderByAsc("username")                   // ORDER BY username ASC
    .last("LIMIT 5");                         // 拼接在 SQL 最后
```

### 5.2 LambdaQueryWrapper（推荐，避免硬编码字段名）

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

// 用方法引用代替字符串字段名，重构时不会遗漏
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .like(User::getNickname, "张")
        .orderByDesc(User::getCreatedAt)
);

// 条件动态拼接（第一个参数为 false 时该条件不生效）
String keyword = "张";  // 可能是 null
List<User> users2 = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .like(keyword != null && !keyword.isBlank(), User::getNickname, keyword)  // 条件生效
        .orderByDesc(User::getCreatedAt)
);
```

### 5.3 LambdaUpdateWrapper（更新）

```java
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

// UPDATE user SET nickname='新昵称', avatar='xxx' WHERE id = 1
userMapper.update(null,
    new LambdaUpdateWrapper<User>()
        .eq(User::getId, 1L)
        .set(User::getNickname, "新昵称")
        .set(User::getAvatar, "https://...")
);

// 批量更新状态
userMapper.update(null,
    new LambdaUpdateWrapper<User>()
        .in(User::getId, Arrays.asList(1L, 2L, 3L))
        .set(User::getStatus, 0)  // 禁用
);
```

### 5.4 常用条件一览

```java
LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
    // 指定查询的列（不写默认 SELECT *）
    .select(Article::getId, Article::getTitle, Article::getAuthorName,
            Article::getViewCount, Article::getPublishedAt)
    // 条件
    .eq(Article::getStatus, "PUBLISHED")
    .eq(Article::getIsDeleted, 0)
    // 动态条件（参数为 null 时不拼接）
    .eq(authorId != null, Article::getAuthorId, authorId)
    .like(keyword != null, Article::getTitle, keyword)
    // 排序
    .orderByDesc(Article::getIsTop)      // 置顶的排前面
    .orderByDesc(Article::getPublishedAt);
```

---

## 6. 分页插件

### 6.1 配置分页插件

```java
package com.example.blogapi.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件（指定数据库类型）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 6.2 使用分页

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

// 创建分页对象（第几页，每页多少条）
Page<Article> page = new Page<>(pageNum, pageSize);

// 执行分页查询
Page<Article> result = articleMapper.selectPage(page,
    new LambdaQueryWrapper<Article>()
        .eq(Article::getStatus, "PUBLISHED")
        .eq(Article::getIsDeleted, 0)
        .orderByDesc(Article::getPublishedAt)
);

// 获取结果
List<Article> records = result.getRecords();   // 当前页数据
long total = result.getTotal();                 // 总记录数
long pages = result.getPages();                 // 总页数
long current = result.getCurrent();             // 当前页码
long size = result.getSize();                   // 每页大小
boolean hasNext = result.hasNext();             // 是否有下一页
```

### 6.3 分页结果转换

```java
// 把 Page<Entity> 转为 Page<VO>
Page<Article> entityPage = articleMapper.selectPage(page, wrapper);

// 方式一：手动 stream 转换
List<ArticleVO> voList = entityPage.getRecords()
    .stream()
    .map(ArticleVO::from)
    .collect(Collectors.toList());

// 构建返回给前端的分页结果
PageResult<ArticleVO> pageResult = PageResult.of(
    voList, entityPage.getTotal(),
    (int) entityPage.getCurrent(), (int) entityPage.getSize()
);
```

---

## 7. IService 与 ServiceImpl

MyBatis Plus 提供了 `IService` 接口和 `ServiceImpl` 实现类，内置了比 BaseMapper 更丰富的方法：

```java
// Mapper 层
@Mapper
public interface UserMapper extends BaseMapper<User> {}

// Service 接口：继承 IService
public interface UserService extends IService<User> {
    // 自定义业务方法
    UserVO getUserVO(Long id);
    PageResult<UserVO> listUsers(Integer page, Integer size, String keyword);
}

// Service 实现：继承 ServiceImpl
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    // ServiceImpl 已经注入了 baseMapper（就是 UserMapper）
    // 无需再 @Autowired UserMapper

    @Override
    public UserVO getUserVO(Long id) {
        User user = getById(id);  // ServiceImpl 提供的方法
        if (user == null) throw new AppException(ErrorCode.USER_NOT_FOUND, "id=" + id);
        return UserVO.from(user);
    }

    @Override
    public PageResult<UserVO> listUsers(Integer page, Integer size, String keyword) {
        Page<User> pageObj = new Page<>(page, size);
        Page<User> result = page(pageObj,
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .eq(User::getStatus, 1)
                .like(keyword != null && !keyword.isBlank(), User::getNickname, keyword)
                .orderByDesc(User::getCreatedAt)
        );
        List<UserVO> vos = result.getRecords().stream()
            .map(UserVO::from).collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }
}
```

### ServiceImpl 提供的常用方法

```java
// 查询
getById(id)                    // 按 ID 查询
list(wrapper)                  // 查询列表
listByIds(ids)                 // 批量 ID 查询
getOne(wrapper)                // 查询单个
count(wrapper)                 // 统计

// 分页
page(page, wrapper)            // 分页查询

// 插入
save(entity)                   // 单条插入
saveBatch(entities)            // 批量插入

// 更新
updateById(entity)             // 按 ID 更新
update(entity, wrapper)        // 按条件更新
saveOrUpdate(entity)           // 存在则更新，不存在则插入

// 删除
removeById(id)                 // 按 ID 删除（逻辑删除）
removeBatchByIds(ids)          // 批量删除
remove(wrapper)                // 按条件删除
```

---

## 8. 完整示例：用户模块接入数据库

### User Entity

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### UserMapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 暂时不需要自定义方法
}
```

### UserService 接口

```java
public interface UserService extends IService<User> {
    UserVO getUserVO(Long id);
    PageResult<UserVO> listUsers(Integer page, Integer size, String keyword);
    UserVO createUser(CreateUserDTO dto);
    UserVO updateUser(Long id, UpdateUserDTO dto);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### UserServiceImpl 实现

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    // 注意：已有 baseMapper，不需要再注入 UserMapper

    @Override
    public UserVO getUserVO(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "id=" + id);
        }
        return UserVO.from(user);
    }

    @Override
    public PageResult<UserVO> listUsers(Integer page, Integer size, String keyword) {
        Page<User> result = page(new Page<>(page, size),
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .like(keyword != null && !keyword.isBlank(), User::getNickname, keyword)
                .orderByDesc(User::getCreatedAt)
        );
        List<UserVO> vos = result.getRecords().stream()
            .map(UserVO::from).collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public UserVO createUser(CreateUserDTO dto) {
        if (existsByUsername(dto.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS, dto.getUsername());
        }
        if (existsByEmail(dto.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, dto.getEmail());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        // 实际项目用 BCrypt 加密：new BCryptPasswordEncoder().encode(dto.getPassword())
        user.setPasswordHash("$2a$..." + dto.getPassword());
        user.setRole("USER");
        user.setStatus(1);

        save(user);
        log.info("用户创建成功：id={}, username={}", user.getId(), user.getUsername());
        return UserVO.from(user);
    }

    @Override
    public UserVO updateUser(Long id, UpdateUserDTO dto) {
        User user = getById(id);
        if (user == null) throw new AppException(ErrorCode.USER_NOT_FOUND, "id=" + id);

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getBio() != null) user.setBio(dto.getBio());

        updateById(user);
        return UserVO.from(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!removeById(id)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "id=" + id);
        }
        log.info("用户删除成功：id={}", id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return count(new LambdaQueryWrapper<User>()
            .eq(User::getEmail, email)) > 0;
    }
}
```

### UserController（基本不变）

```java
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> list(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String keyword
    ) {
        return Result.ok(userService.listUsers(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getUserVO(id));
    }

    @PostMapping
    public Result<UserVO> create(@RequestBody @Valid CreateUserDTO dto) {
        return Result.ok(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateUserDTO dto) {
        return Result.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
```

---

## 9. 和 Sequelize 的对比

```javascript
// Sequelize（Node.js ORM）
const users = await User.findAll({
  where: {
    status: 1,
    nickname: { [Op.like]: "%张%" },
  },
  order: [["createdAt", "DESC"]],
  limit: 10,
  offset: 0,
});

// 分页
const { count, rows } = await User.findAndCountAll({
  where: { status: 1 },
  limit: 10,
  offset: (page - 1) * 10,
});
```

```java
// MyBatis Plus
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .like(User::getNickname, "张")
        .orderByDesc(User::getCreatedAt)
        .last("LIMIT 10")
);

// 分页
Page<User> result = userMapper.selectPage(
    new Page<>(page, 10),
    new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
);
```

| 对比     | MyBatis Plus                | Sequelize            |
| -------- | --------------------------- | -------------------- |
| 查询方式 | LambdaQueryWrapper 链式     | 对象条件 + Op 操作符 |
| 字段引用 | 方法引用（`User::getName`） | 字符串（`'name'`）   |
| 分页     | Page 对象                   | limit + offset       |
| 关联查询 | 通常手写 XML                | include 声明         |
| 类型安全 | ✅ 编译期检查               | ❌ 运行时才知道      |

---

## 练习

### 练习 1：接入数据库

1. 按 2-02 的项目添加 MyBatis Plus 依赖，配置数据源
2. 创建 `User`、`Article` 实体类（对应 2-10 的表结构）
3. 创建对应的 `UserMapper`、`ArticleMapper`
4. 写一个测试接口 `GET /test/users`，调用 `userMapper.selectList(null)` 返回所有用户，验证数据库连通

### 练习 2：条件构造器练习

在 `ArticleMapper` 的基础上，用 `LambdaQueryWrapper` 实现以下查询，在接口中暴露并测试：

1. 查询所有已发布文章，只返回 id、title、authorName、viewCount 四个字段
2. 查询某作者（`authorId=2`）的文章，按浏览量倒序
3. 查询标题包含"Spring"的文章
4. 统计各状态的文章数量（提示：多次 `count` 或用分组）

### 练习 3：分页查询

实现文章列表接口 `GET /api/articles`，支持：

- `page`（默认 1）、`size`（默认 10）
- `status` 过滤（可选）
- `keyword` 标题搜索（可选）
- `authorId` 过滤（可选）
- 按发布时间倒序，置顶文章排最前

返回 `PageResult<ArticleVO>` 格式。

### 练习 4（进阶）：软删除验证

1. 确认 `@TableLogic` 配置正确：执行 `removeById` 时，观察控制台输出是 `UPDATE SET is_deleted=1` 还是真正的 DELETE
2. 验证软删除后 `selectById` 查询不到（WHERE is_deleted=0 自动加上）
3. 如果需要查询"包括已删除的"数据，应该怎么写？（提示：在 QueryWrapper 上关闭逻辑删除过滤）

---

## 常见问题

**Q：BaseMapper 和 IService 的方法有什么区别，用哪个？**  
A：BaseMapper 是 Mapper 层的基础方法，IService 是 Service 层的封装，提供了更多便捷方法（如 saveBatch 批量插入优化）。推荐 Service 继承 IService/ServiceImpl，在 Service 层用 `this.getById()` 等方法，Mapper 层自定义 SQL 时才直接操作 baseMapper。

**Q：LambdaQueryWrapper 和 QueryWrapper 有什么区别，用哪个？**  
A：推荐用 `LambdaQueryWrapper`。`QueryWrapper` 用字符串指定字段名（如 `eq("user_name", value)`），重构时容易遗漏修改。`LambdaQueryWrapper` 用方法引用（如 `eq(User::getUsername, value)`），类型安全，重构友好。

**Q：MyBatis Plus 能处理复杂 SQL（多表 JOIN、子查询）吗？**  
A：条件构造器主要用于单表操作。复杂 SQL（多表 JOIN、窗口函数、子查询）通常用 XML Mapper 或 `@Select` 注解手写。这两种方式可以混用，在同一个 Mapper 里一部分用 BaseMapper 继承，一部分自定义方法。

**Q：`selectById` 查出来的对象，我只想更新某几个字段，其他字段保持不变，怎么做？**  
A：用 `updateById(entity)` 时，MP 会跳过 null 字段，只更新非 null 的字段。所以直接 `setXxx(value)` 其他字段保持 null，调 `updateById` 即可。也可以用 `LambdaUpdateWrapper` 精确指定 set 的字段。

---

## 下一步

下一节 **[2-12 · MyBatis Plus 进阶](../2-12/README.md)** 深入 MyBatis Plus 的进阶特性：XML 自定义 SQL、自动填充、乐观锁、逻辑删除配置和多表关联查询的最佳实践。

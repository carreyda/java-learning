# 2-13 · RESTful API 设计原则

> **目标**：系统掌握 REST API 的设计规范，包括资源命名、版本化、幂等性、状态码规范，能设计出清晰易用的 API。  
> **预计时间**：2-3 小时  
> **前置要求**：完成 2-12，已有 Controller 开发经验

---

## 目录

- [REST 核心约束](#1-rest-核心约束)
- [资源命名规范](#2-资源命名规范)
- [HTTP 方法语义](#3-http-方法语义)
- [状态码规范](#4-状态码规范)
- [API 版本化](#5-api-版本化)
- [幂等性设计](#6-幂等性设计)
- [请求与响应规范](#7-请求与响应规范)
- [常见设计坑](#8-常见设计坑)
- [博客 API 完整设计](#9-博客-api-完整设计)
- [练习](#练习)
- [下一步](#下一步)

---

## 1. REST 核心约束

REST（Representational State Transfer）不是协议，是一种**架构风格**，核心约束：

```
1. 统一接口（Uniform Interface）
   → 用 HTTP 方法语义表达操作，用 URI 标识资源

2. 无状态（Stateless）
   → 每个请求包含所有必要信息，服务器不保存客户端状态
   → Session 状态由客户端维护（JWT Token）

3. 可缓存（Cacheable）
   → GET 请求结果可缓存，用 Cache-Control、ETag 控制

4. 分层系统（Layered System）
   → 客户端不需要知道是否直接连接到服务器（可能有代理、负载均衡）
```

---

## 2. 资源命名规范

### 2.1 用名词不用动词

```
❌ 动词风格（RPC 风格）
GET  /getArticles
POST /createArticle
POST /deleteArticle?id=1
POST /publishArticle

✅ REST 风格（资源 + HTTP 方法）
GET    /articles
POST   /articles
DELETE /articles/1
POST   /articles/1/publish  ← 非 CRUD 操作用子资源或动作
```

### 2.2 用复数名词

```
✅ /articles      /users      /categories    /tags
❌ /article       /user       /category      /tag
```

### 2.3 层级关系用路径表达

```
/users/42/articles          用户42的所有文章
/articles/1/comments        文章1的所有评论
/articles/1/comments/5      文章1的第5条评论
/articles/1/tags            文章1的所有标签

不要太深（超过3级就要考虑是否合理）：
❌ /users/42/articles/1/comments/5/likes  太深了
✅ /comments/5/likes                      简化
```

### 2.4 过滤、排序、分页用查询参数

```
GET /articles?status=PUBLISHED              过滤
GET /articles?keyword=Spring&category=Java  多条件过滤
GET /articles?sortBy=viewCount&order=desc   排序
GET /articles?page=1&size=10               分页
GET /articles?fields=id,title,author       字段选择（稀疏字段）

不要用路径表达过滤条件：
❌ /articles/published/page/1
✅ /articles?status=published&page=1
```

### 2.5 命名风格

```
URI 用小写字母和连字符（kebab-case）：
✅ /article-categories
✅ /blog-posts
❌ /articleCategories  （不用驼峰）
❌ /article_categories （不用下划线）
```

---

## 3. HTTP 方法语义

### 3.1 各方法的正确用法

| 方法    | 语义                        | 幂等 | 安全 | 请求体 |
| ------- | --------------------------- | ---- | ---- | ------ |
| GET     | 查询资源，不修改数据        | ✅   | ✅   | 无     |
| POST    | 创建资源，提交数据          | ❌   | ❌   | 有     |
| PUT     | 全量替换资源                | ✅   | ❌   | 有     |
| PATCH   | 部分更新资源                | ❌   | ❌   | 有     |
| DELETE  | 删除资源                    | ✅   | ❌   | 无/有  |
| HEAD    | 只获取响应头（不返回 body） | ✅   | ✅   | 无     |
| OPTIONS | 查询支持的方法（CORS 预检） | ✅   | ✅   | 无     |

### 3.2 PUT vs PATCH

```java
// PUT：全量替换，未传的字段设为默认值/null
PUT /api/articles/1
{
    "title": "新标题",
    "content": "新内容",
    "status": "DRAFT"
    // 未传的字段（如 category_id）会被设为 null
}

// PATCH：部分更新，只更新传入的字段
PATCH /api/articles/1
{
    "title": "新标题"
    // 只更新 title，其他字段保持不变
}
```

```java
// Spring Boot 实现 PATCH（注意：只更新非 null 字段）
@PatchMapping("/{id}")
public Result<ArticleVO> partialUpdate(
    @PathVariable Long id,
    @RequestBody Map<String, Object> fields  // 用 Map 接收，避免 null 值问题
) {
    return Result.ok(articleService.partialUpdate(id, fields));
}

// Service 层：只更新 Map 中包含的字段
@Override
public ArticleVO partialUpdate(Long id, Map<String, Object> fields) {
    Article article = getById(id);
    if (article == null) throw new AppException(ErrorCode.ARTICLE_NOT_FOUND);

    // 只更新 Map 中存在的字段
    if (fields.containsKey("title")) article.setTitle((String) fields.get("title"));
    if (fields.containsKey("summary")) article.setSummary((String) fields.get("summary"));
    if (fields.containsKey("categoryId")) {
        article.setCategoryId(((Number) fields.get("categoryId")).intValue());
    }

    updateById(article);
    return ArticleVO.from(article);
}
```

### 3.3 非 CRUD 操作的处理

有些操作不是简单的 CRUD，比如"发布"、"点赞"、"关注"：

```
方式一：子资源（推荐）
POST /articles/1/publish        发布文章
POST /articles/1/like           点赞
DELETE /articles/1/like         取消点赞
POST /users/2/follow            关注用户
DELETE /users/2/follow          取消关注

方式二：动作名词
POST /articles/1/actions/publish
POST /articles/1/actions/archive

方式三：状态更新（用 PATCH）
PATCH /articles/1
{ "status": "PUBLISHED" }
```

---

## 4. 状态码规范

### 4.1 常用状态码

```
2xx 成功：
200 OK               → GET、PUT、PATCH 成功
201 Created          → POST 创建成功（Location 头指向新资源）
204 No Content       → DELETE 成功，或 PUT/PATCH 不返回内容

4xx 客户端错误：
400 Bad Request      → 请求格式错误、参数校验失败
401 Unauthorized     → 未认证（没有登录 / Token 无效）
403 Forbidden        → 已认证但没有权限
404 Not Found        → 资源不存在
405 Method Not Allowed → 请求方法不支持
409 Conflict         → 数据冲突（如用户名已存在）
422 Unprocessable Entity → 语义上无法处理（更精确的 400）
429 Too Many Requests → 请求频率超限

5xx 服务器错误：
500 Internal Server Error → 服务器未处理的异常
502 Bad Gateway           → 网关/代理问题
503 Service Unavailable   → 服务不可用（维护/过载）
```

### 4.2 正确使用状态码

```java
// ✅ 创建成功返回 201
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Result<ArticleVO> create(@RequestBody @Valid CreateArticleDTO dto) {
    return Result.ok(articleService.create(dto));
}

// ✅ 删除成功返回 204（不返回 body）
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    articleService.delete(id);
}

// ✅ 不存在返回 404（在异常处理器里配置）
// 用户不存在 → 抛 ResourceNotFoundException → 处理器返回 404

// ✅ 权限不足返回 403（不是 401）
// 已登录但无权限操作 → 返回 403

// ❌ 把所有错误都返回 200
// 不要这样做：
{
    "code": 0,     ← 自定义错误码
    "msg": "用户不存在",
    "data": null
}
// HTTP 状态码就是 200，这让客户端无法通过状态码判断成功/失败
```

---

## 5. API 版本化

### 5.1 为什么需要版本化

```
当 API 的接口定义发生破坏性变更时（修改字段名、删除字段、改变行为），
需要版本化来保证旧版客户端仍然可以正常工作。
```

### 5.2 三种版本化方式

**方式一：URI 版本（最常用）**

```
/api/v1/articles
/api/v2/articles
```

```java
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleV1Controller { ... }

@RestController
@RequestMapping("/api/v2/articles")
public class ArticleV2Controller { ... }
```

**方式二：请求头版本**

```
GET /api/articles
Accept: application/vnd.blogapi.v2+json
```

**方式三：查询参数版本（不推荐）**

```
GET /api/articles?version=2
```

**推荐**：URI 版本最直观，最容易理解和调试，是国内主流方案。

### 5.3 版本迁移策略

```java
// 在 application.yml 中配置当前版本
app:
  api:
    current-version: v1
    deprecated-versions: []

// 废弃旧版本时加 @Deprecated 标记和警告响应头
@GetMapping("/v1/articles/{id}")
@Deprecated
public ResponseEntity<Result<ArticleV1VO>> getByIdV1(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header("Deprecation", "version v1 will be removed on 2027-01-01")
        .header("Sunset", "Tue, 01 Jan 2027 00:00:00 GMT")
        .body(Result.ok(articleService.getV1(id)));
}
```

---

## 6. 幂等性设计

**幂等性**：对同一个请求，执行一次和执行多次的效果相同。

### 6.1 各方法的幂等性

```
GET：天然幂等（不修改数据）
DELETE：幂等（删除已删除的资源，还是删除状态）
PUT：幂等（相同请求体多次 PUT，结果一样）
POST：不幂等（多次提交会创建多条记录）
PATCH：不一定（具体看实现）
```

### 6.2 POST 的幂等性处理（防重复提交）

```java
// 方案：客户端生成唯一请求 ID，服务端检查是否已处理
@PostMapping
public Result<ArticleVO> create(
    @RequestHeader("X-Request-Id") String requestId,  // 客户端生成的唯一 ID
    @RequestBody @Valid CreateArticleDTO dto
) {
    return Result.ok(articleService.createIdempotent(requestId, dto));
}

// Service 实现
@Override
public ArticleVO createIdempotent(String requestId, CreateArticleDTO dto) {
    // 检查 Redis 中是否已处理（用 requestId 作为 key）
    String cacheKey = "create:article:" + requestId;
    String cachedId = redisTemplate.opsForValue().get(cacheKey);

    if (cachedId != null) {
        // 已处理，直接返回缓存的结果
        return ArticleVO.from(getById(Long.parseLong(cachedId)));
    }

    // 首次处理
    Article article = doCreate(dto);

    // 存入缓存，有效期 5 分钟（防止 5 分钟内重复提交）
    redisTemplate.opsForValue().set(cacheKey, article.getId().toString(),
        5, TimeUnit.MINUTES);

    return ArticleVO.from(article);
}
```

---

## 7. 请求与响应规范

### 7.1 统一响应格式（最终版）

```json
{
    "code": 200,
    "message": "success",
    "data": { ... },
    "timestamp": "2026-06-19T10:30:00",
    "traceId": "a1b2c3d4"
}
```

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private String timestamp;
    private String traceId;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
        this.traceId = MDC.get("traceId");  // 链路追踪 ID（后续章节会讲）
    }
    // ... 工厂方法同前
}
```

### 7.2 分页响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [ ... ],
        "total": 156,
        "page": 1,
        "size": 10,
        "totalPages": 16,
        "hasNext": true,
        "hasPrev": false
    }
}
```

### 7.3 错误响应格式

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "title": "标题不能为空",
    "content": "内容长度不能超过 50000 字符"
  },
  "timestamp": "2026-06-19T10:30:00"
}
```

### 7.4 时间格式规范

```java
// ✅ 推荐：ISO 8601 格式，带时区
"createdAt": "2026-06-19T10:30:00+08:00"

// 或者统一用 UTC，前端自行转换
"createdAt": "2026-06-19T02:30:00Z"

// application.yml 配置
spring:
  jackson:
    date-format: yyyy-MM-dd'T'HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false  # 不要用时间戳，用字符串
```

---

## 8. 常见设计坑

### 坑 1：URL 里用动词

```
❌ /api/getUserInfo
❌ /api/deleteArticle
❌ /api/article/create
✅ /api/users/{id}
✅ /api/articles/{id}
✅ /api/articles
```

### 坑 2：用 GET 做修改操作

```
❌ GET /api/article/delete?id=1
❌ GET /api/user/ban?id=5
✅ DELETE /api/articles/1
✅ PATCH /api/users/5 + body: {"status": "BANNED"}
```

### 坑 3：过度嵌套 URL

```
❌ /api/users/1/articles/5/comments/3/likes/2
✅ /api/likes/2  或  /api/comment-likes/2
```

### 坑 4：把错误信息放在 200 响应里

```json
❌ HTTP 200
{ "success": false, "code": -1, "msg": "用户不存在" }

✅ HTTP 404
{ "code": 404, "message": "用户不存在" }
```

### 坑 5：字段命名不一致

```
❌ 有时 userId，有时 user_id，有时 uid
✅ 统一用 camelCase：userId（JSON 标准）
✅ 或者统一用 snake_case：user_id（看团队约定，保持一致）
```

### 坑 6：删除操作返回 200 + 已删除的对象

```java
❌
@DeleteMapping("/{id}")
public Result<Article> delete(@PathVariable Long id) {
    Article article = articleService.delete(id);
    return Result.ok(article);  // 返回被删除的对象，通常不必要
}

✅
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    articleService.delete(id);
    // 不返回内容
}
```

---

## 9. 博客 API 完整设计

整理博客系统的完整 API 列表：

### 用户模块 /api/v1/users

```
POST   /api/v1/auth/register         注册
POST   /api/v1/auth/login            登录（返回 Token）
POST   /api/v1/auth/logout           登出
POST   /api/v1/auth/refresh          刷新 Token

GET    /api/v1/users                 用户列表（管理员）
GET    /api/v1/users/{id}            用户详情
PUT    /api/v1/users/{id}            更新用户信息
DELETE /api/v1/users/{id}            删除用户（管理员）
GET    /api/v1/users/{id}/articles   用户的文章列表
```

### 文章模块 /api/v1/articles

```
GET    /api/v1/articles              文章列表（分页 + 过滤）
GET    /api/v1/articles/hot          热门文章
GET    /api/v1/articles/recommended  推荐文章
GET    /api/v1/articles/{id}         文章详情
POST   /api/v1/articles              创建文章（草稿）
PUT    /api/v1/articles/{id}         更新文章
PATCH  /api/v1/articles/{id}         部分更新（如只改标题）
DELETE /api/v1/articles/{id}         删除文章

POST   /api/v1/articles/{id}/publish     发布
POST   /api/v1/articles/{id}/archive     归档
POST   /api/v1/articles/{id}/like        点赞
DELETE /api/v1/articles/{id}/like        取消点赞
POST   /api/v1/articles/{id}/favorite    收藏
DELETE /api/v1/articles/{id}/favorite    取消收藏
```

### 评论模块 /api/v1/comments

```
GET    /api/v1/articles/{id}/comments    文章的评论列表
POST   /api/v1/articles/{id}/comments    发表评论
PUT    /api/v1/comments/{id}             编辑评论
DELETE /api/v1/comments/{id}             删除评论
POST   /api/v1/comments/{id}/like        点赞评论
```

### 分类/标签模块

```
GET    /api/v1/categories               分类列表（树形）
POST   /api/v1/categories               创建分类（管理员）
PUT    /api/v1/categories/{id}          更新分类
DELETE /api/v1/categories/{id}          删除分类

GET    /api/v1/tags                     标签列表
GET    /api/v1/tags/{slug}/articles     某标签的文章
POST   /api/v1/tags                     创建标签
DELETE /api/v1/tags/{id}               删除标签
```

### 统计模块

```
GET    /api/v1/stats/overview           全站统计（管理员）
GET    /api/v1/stats/my                 个人统计
```

---

## 练习

### 练习 1：API Review

审查你在 2-07（内存版 TODO API）和 2-11（用户模块）中设计的接口：

1. 找出违反 RESTful 规范的地方（URI 有动词？GET 做了修改？状态码不对？）
2. 列出至少 3 个需要改进的点，并给出改进方案

### 练习 2：博客 API 版本化

1. 把现有的博客 API 统一加上 `/v1` 前缀
2. 假设 v2 版本的文章 VO 调整了字段名（`viewCount` 改为 `views`），实现 v2 接口并保持 v1 继续可用
3. 给 v1 的接口添加 `Deprecation` 响应头

### 练习 3：幂等性保护

给文章创建接口添加幂等性保护（暂时用 Map 模拟 Redis）：

1. 请求头加 `X-Request-Id` 参数
2. Service 中检查该 requestId 是否已处理
3. 测试：用 Postman 连续发两次带同一 requestId 的创建请求，验证只创建一次

---

## 下一步

下一节 **[2-14 · API 文档与测试](../2-14/README.md)** 系统学习 Knife4j API 文档的高级配置，以及如何用 Spring Boot Test 编写接口自动化测试，告别手动 Postman 点点点。

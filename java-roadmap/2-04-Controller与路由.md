# 2-04 · Controller 与路由

> **目标**：系统掌握 Spring MVC 的 Controller 层开发，包括参数绑定、请求处理、统一响应格式，能独立完成 RESTful API 的路由设计。  
> **预计时间**：3-4 小时  
> **前置要求**：完成 2-03，理解 IoC 与 Bean 注入

---

## 目录

- [RESTful API 设计规范](#1-restful-api-设计规范)
- [Controller 基础注解](#2-controller-基础注解)
- [参数绑定](#3-参数绑定)
- [请求体与响应体](#4-请求体与响应体)
- [统一响应格式](#5-统一响应格式)
- [全局异常处理](#6-全局异常处理)
- [接口文档（Knife4j）](#7-接口文档knife4j)
- [和 Express 的对比](#8-和-express-的对比)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. RESTful API 设计规范

REST（Representational State Transfer）是目前最主流的 API 风格。

### 1.1 核心原则

```
资源用名词，不用动词：
  ✅ GET /articles        获取文章列表
  ❌ GET /getArticles

HTTP 方法表达操作：
  GET     → 查询（不修改数据）
  POST    → 创建
  PUT     → 全量更新（替换整个资源）
  PATCH   → 部分更新（只改某几个字段）
  DELETE  → 删除

用复数名词：
  /articles（不是 /article）
  /users（不是 /user）

层级关系用路径表达：
  /users/42/articles      用户42的所有文章
  /articles/1/comments    文章1的所有评论
```

### 1.2 状态码规范

| 状态码                    | 含义         | 场景                 |
| ------------------------- | ------------ | -------------------- |
| 200 OK                    | 成功         | GET、PUT、PATCH 成功 |
| 201 Created               | 创建成功     | POST 成功            |
| 204 No Content            | 成功但无内容 | DELETE 成功          |
| 400 Bad Request           | 请求参数错误 | 参数校验失败         |
| 401 Unauthorized          | 未认证       | 未登录               |
| 403 Forbidden             | 无权限       | 没有操作权限         |
| 404 Not Found             | 资源不存在   | 查询不存在的资源     |
| 500 Internal Server Error | 服务器错误   | 未处理的异常         |

---

## 2. Controller 基础注解

```java
@RestController                    // 标记为 REST 控制器，返回值自动序列化为 JSON
@RequestMapping("/api/articles")   // 类级别路径前缀
public class ArticleController {

    // GET /api/articles
    @GetMapping
    public List<Article> list() { ... }

    // GET /api/articles/1
    @GetMapping("/{id}")
    public Article getById(@PathVariable Long id) { ... }

    // POST /api/articles
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)  // 返回 201 状态码
    public Article create(@RequestBody ArticleDTO dto) { ... }

    // PUT /api/articles/1
    @PutMapping("/{id}")
    public Article update(@PathVariable Long id, @RequestBody ArticleDTO dto) { ... }

    // PATCH /api/articles/1
    @PatchMapping("/{id}")
    public Article partialUpdate(@PathVariable Long id,
                                  @RequestBody Map<String, Object> fields) { ... }

    // DELETE /api/articles/1
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 返回 204
    public void delete(@PathVariable Long id) { ... }
}
```

---

## 3. 参数绑定

### 3.1 路径参数 @PathVariable

```java
// GET /articles/42
@GetMapping("/{id}")
public Article getById(@PathVariable Long id) { ... }

// GET /users/10/articles/42（多个路径参数）
@GetMapping("/users/{userId}/articles/{articleId}")
public Article getUserArticle(@PathVariable Long userId,
                               @PathVariable Long articleId) { ... }

// 变量名和路径占位符不一致时显式指定
@GetMapping("/{article-id}")
public Article get(@PathVariable("article-id") Long articleId) { ... }
```

### 3.2 查询参数 @RequestParam

```java
// GET /articles?page=1&size=10&keyword=Java
@GetMapping
public Page<Article> list(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer size,
    @RequestParam(required = false) String keyword  // 可选参数
) { ... }

// 接收多值参数：GET /articles?tags=Java&tags=Spring
@GetMapping("/tagged")
public List<Article> getByTags(@RequestParam List<String> tags) { ... }
```

### 3.3 请求头 @RequestHeader

```java
// 读取请求头
@GetMapping("/profile")
public User getProfile(
    @RequestHeader("Authorization") String token,
    @RequestHeader(value = "Accept-Language", defaultValue = "zh-CN") String lang
) { ... }
```

### 3.4 Cookie @CookieValue

```java
@GetMapping("/check")
public String check(@CookieValue(value = "sessionId", required = false) String sessionId) {
    return sessionId != null ? "已登录" : "未登录";
}
```

### 3.5 HttpServletRequest（原生对象）

```java
@GetMapping("/raw")
public String raw(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    String userAgent = request.getHeader("User-Agent");
    return "IP: " + ip + ", UA: " + userAgent;
}
```

---

## 4. 请求体与响应体

### 4.1 @RequestBody 接收 JSON

```java
// 接收 JSON，自动反序列化为对象
@PostMapping
public Article create(@RequestBody ArticleCreateDTO dto) {
    // dto 已经是填好值的对象
    return articleService.create(dto);
}

// DTO 类
public class ArticleCreateDTO {
    private String title;
    private String content;
    private List<String> tags;
    // getter / setter
}
```

### 4.2 ResponseEntity（精确控制响应）

```java
// 返回 ResponseEntity 可以精确控制状态码、响应头、响应体
@GetMapping("/{id}")
public ResponseEntity<Article> getById(@PathVariable Long id) {
    Article article = articleService.findById(id);

    if (article == null) {
        return ResponseEntity.notFound().build();  // 404
    }

    return ResponseEntity.ok(article);  // 200 + body
}

@PostMapping
public ResponseEntity<Article> create(@RequestBody ArticleCreateDTO dto) {
    Article article = articleService.create(dto);

    URI location = URI.create("/api/articles/" + article.getId());
    return ResponseEntity.created(location).body(article);  // 201 + Location 头
}

// 自定义响应头
@GetMapping("/download/{id}")
public ResponseEntity<byte[]> download(@PathVariable Long id) {
    byte[] data = fileService.getContent(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.pdf")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
        .body(data);
}
```

---

## 5. 统一响应格式

实际项目中，所有接口应该返回**统一的 JSON 格式**，方便前端处理：

```json
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

### 5.1 定义统一响应类

```java
package com.example.blogapi.common;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    // 私有构造
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功（有数据）
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功（无数据）
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    // 失败
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    // 常用失败快捷方法
    public static <T> Result<T> badRequest(String message) {
        return fail(400, message);
    }

    public static <T> Result<T> notFound(String message) {
        return fail(404, message);
    }

    public static <T> Result<T> serverError(String message) {
        return fail(500, message);
    }
}
```

### 5.2 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Result<List<Article>> list() {
        List<Article> articles = articleService.findAll();
        return Result.ok(articles);
    }

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id) {
        Article article = articleService.findById(id);
        if (article == null) {
            return Result.notFound("文章不存在：id=" + id);
        }
        return Result.ok(article);
    }

    @PostMapping
    public Result<Article> create(@RequestBody ArticleCreateDTO dto) {
        Article article = articleService.create(dto);
        return Result.ok(article);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.ok();
    }
}
```

响应示例：

```json
// GET /api/articles/1 - 成功
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "title": "Spring Boot 入门",
        "content": "..."
    }
}

// GET /api/articles/999 - 不存在
{
    "code": 404,
    "message": "文章不存在：id=999",
    "data": null
}
```

---

## 6. 全局异常处理

不在每个 Controller 里写 try-catch，用 `@ControllerAdvice` 统一处理：

```java
package com.example.blogapi.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    // 处理资源不存在
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(ResourceNotFoundException e) {
        return Result.notFound(e.getMessage());
    }

    // 处理参数校验失败（@Valid 触发）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationFailed(MethodArgumentNotValidException e) {
        // 收集所有字段的错误信息
        String message = e.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(java.util.stream.Collectors.joining("; "));
        return Result.badRequest(message);
    }

    // 处理其他未知异常（兜底）
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        // 生产环境不要把详细错误信息返回给客户端
        return Result.serverError("服务器内部错误，请稍后重试");
    }
}
```

**搭配自定义异常：**

```java
// 业务异常基类
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public int getCode() { return code; }
}

// 具体异常
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super(404, resource + " 不存在：id=" + id);
    }
}

// Service 中抛出
public Article findById(Long id) {
    return articleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("文章", id));
}
```

现在 Controller 不需要处理任何异常，代码非常干净：

```java
@GetMapping("/{id}")
public Result<Article> getById(@PathVariable Long id) {
    // 如果文章不存在，Service 抛出 ResourceNotFoundException
    // GlobalExceptionHandler 自动捕获并返回 404 响应
    Article article = articleService.findById(id);
    return Result.ok(article);
}
```

---

## 7. 接口文档（Knife4j）

Knife4j 是 Swagger 的增强版，能自动生成在线接口文档。

### 7.1 引入依赖

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

### 7.2 配置

```yaml
# application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 7.3 在 Controller 中添加文档注解

```java
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "文章管理", description = "文章的增删改查接口")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Operation(summary = "获取文章列表", description = "支持分页和关键词搜索")
    @GetMapping
    public Result<List<Article>> list(
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size
    ) { ... }

    @Operation(summary = "根据ID获取文章")
    @GetMapping("/{id}")
    public Result<Article> getById(
        @Parameter(description = "文章ID") @PathVariable Long id
    ) { ... }
}
```

启动后访问 `http://localhost:8080/doc.html` 即可看到在线文档。

---

## 8. 和 Express 的对比

```javascript
// Express 全局异常处理
app.use((err, req, res, next) => {
  if (err instanceof BusinessError) {
    return res.status(err.code).json({ code: err.code, message: err.message });
  }
  res.status(500).json({ code: 500, message: "服务器错误" });
});

// Express 统一响应（手动封装）
const Result = {
  ok: (data) => ({ code: 200, message: "success", data }),
  fail: (code, message) => ({ code, message, data: null }),
};

// Express Controller
app.get("/api/articles/:id", async (req, res, next) => {
  try {
    const article = await articleService.findById(req.params.id);
    if (!article) return res.status(404).json(Result.fail(404, "文章不存在"));
    res.json(Result.ok(article));
  } catch (err) {
    next(err); // 传给全局错误处理
  }
});
```

```java
// Spring Boot：全局异常处理器自动捕获，Controller 完全不需要 try-catch
@GetMapping("/{id}")
public Result<Article> getById(@PathVariable Long id) {
    return Result.ok(articleService.findById(id));  // Service 内部抛异常，这里无感知
}
```

| 对比     | Spring Boot             | Express                                 |
| -------- | ----------------------- | --------------------------------------- |
| 全局异常 | `@RestControllerAdvice` | `app.use((err, req, res, next) => ...)` |
| 参数获取 | 注解声明，自动绑定      | `req.params` / `req.query` / `req.body` |
| 响应格式 | 返回对象，自动序列化    | `res.json()`                            |
| 接口文档 | Knife4j 自动生成        | 手写或用 swagger-jsdoc                  |
| 类型安全 | ✅ 编译时检查           | ❌ 运行时才知道                         |

---

## 练习

### 练习 1：完整 CRUD Controller

设计一个**用户管理模块**，包含：

**DTO 类：**

```java
// 创建用户
class CreateUserDTO { String username; String email; Integer age; }
// 更新用户（所有字段可选）
class UpdateUserDTO { String email; Integer age; }
```

**Controller 接口（暂时用 Map 模拟数据，不连数据库）：**

- `GET /api/users` → 返回所有用户列表
- `GET /api/users/{id}` → 返回指定用户，不存在时返回 404
- `POST /api/users` → 创建用户，返回创建后的用户（含自动生成的 ID）
- `PUT /api/users/{id}` → 全量更新，不存在时返回 404
- `DELETE /api/users/{id}` → 删除用户，返回 204

所有接口使用统一的 `Result<T>` 响应格式。

### 练习 2：全局异常处理

1. 定义 `UserNotFoundException extends BusinessException`
2. 在 UserService 的 `findById` 中，找不到用户时抛出该异常
3. 在 `GlobalExceptionHandler` 中处理该异常，返回 404 + 友好提示
4. 测试：请求不存在的用户 ID，验证返回格式正确

### 练习 3：参数绑定练习

新建 `SearchController`，实现搜索接口 `GET /api/search`，支持：

- `keyword`（必填）：搜索关键词
- `type`（可选，默认 `all`）：搜索类型，值为 `article`/`user`/`all`
- `page`（可选，默认 1）
- `size`（可选，默认 10，最大 100）
- `sort`（可选，默认 `desc`）：排序方向

返回接收到的参数信息（用 Map 包装），验证默认值和可选参数都正确处理。

### 练习 4（进阶）：引入 Knife4j

1. 在项目中添加 Knife4j 依赖
2. 给前面写的 UserController 添加完整的 Swagger 注解（`@Tag`、`@Operation`、`@Parameter`）
3. 启动后访问 `http://localhost:8080/doc.html`，确认文档生成正确
4. 在文档页面直接测试接口（Knife4j 内置调试功能）

---

## 常见问题

**Q：`@RequestParam` 和 `@PathVariable` 怎么选？**  
A：路径参数（`/articles/42`）用 `@PathVariable`，查询参数（`?page=1`）用 `@RequestParam`。ID 类的资源标识用路径参数，过滤/分页/搜索条件用查询参数。

**Q：POST 接口收不到 `@RequestBody`，一直是 null？**  
A：检查三点：① 请求的 Content-Type 必须是 `application/json`；② Postman 中选 Body → raw → JSON；③ DTO 类要有无参构造方法和 getter/setter（或用 Lombok 的 `@Data`）。

**Q：`@ControllerAdvice` 和 `@RestControllerAdvice` 的区别？**  
A：`@RestControllerAdvice = @ControllerAdvice + @ResponseBody`，方法返回值自动序列化为 JSON。做 REST API 时用 `@RestControllerAdvice`。

**Q：全局异常处理器捕获不到某些异常？**  
A：`@ControllerAdvice` 只能捕获进入 Spring MVC 处理链后的异常。如果是过滤器（Filter）里抛出的异常，需要在 Filter 内处理或用 `ErrorController`。

**Q：多个 `@ExceptionHandler` 都能匹配时，哪个优先执行？**  
A：越具体的越优先。`BusinessException` 优先于 `Exception`，子类优先于父类。

---

## 下一步

下一节 **[2-05 · Service 与分层架构](../2-05/README.md)** 系统学习三层架构的设计原则，掌握 Service 层的职责划分，以及接口与实现类的标准写法。

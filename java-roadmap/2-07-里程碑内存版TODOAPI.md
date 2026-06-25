# 2-07 · 🏆 里程碑：内存版 TODO API

> **目标**：综合运用 2-01 至 2-06 所有知识，不连数据库，用内存存储构建完整的 TODO 管理 REST API，并用 Postman 全面联调测试。  
> **预计时间**：5-6 小时  
> **前置要求**：完成 2-01 至 2-06

---

## 目录

- [项目介绍](#1-项目介绍)
- [需求分析](#2-需求分析)
- [项目结构](#3-项目结构)
- [数据模型与枚举](#4-数据模型与枚举)
- [DTO 设计](#5-dto-设计)
- [异常体系](#6-异常体系)
- [Repository 层](#7-repository-层)
- [Service 层](#8-service-层)
- [Controller 层](#9-controller-层)
- [统一响应与异常处理](#10-统一响应与异常处理)
- [Postman 测试](#11-postman-测试)
- [自测清单](#12-自测清单)
- [扩展挑战](#13-扩展挑战)

---

## 1. 项目介绍

用 Spring Boot 构建一个 TODO 管理系统的 REST API，数据存储在内存中（`ConcurrentHashMap`），无需数据库。这是进入数据库前的完整 API 开发实战。

**技术栈：**

| 技术              | 用途         |
| ----------------- | ------------ |
| Spring Boot 3.x   | 核心框架     |
| Spring MVC        | REST API     |
| Spring Validation | 参数校验     |
| Lombok            | 简化代码     |
| Knife4j           | 接口文档     |
| ConcurrentHashMap | 内存数据存储 |

---

## 2. 需求分析

### TODO 功能

- 创建 TODO（标题、描述、优先级、截止日期）
- 查询 TODO 列表（支持按状态/优先级过滤，支持分页）
- 查询单个 TODO 详情
- 更新 TODO 信息
- 更新 TODO 状态（待办→进行中→已完成）
- 删除 TODO

### 标签功能

- 为 TODO 添加标签
- 移除标签
- 按标签筛选 TODO

### 统计功能

- 各状态 TODO 数量统计
- 今日到期/已逾期 TODO 列表

### 接口清单

```
TODO 管理：
  POST   /api/todos              创建
  GET    /api/todos              列表（分页 + 过滤）
  GET    /api/todos/{id}         详情
  PUT    /api/todos/{id}         更新
  PATCH  /api/todos/{id}/status  更新状态
  DELETE /api/todos/{id}         删除

标签管理：
  POST   /api/todos/{id}/tags    添加标签
  DELETE /api/todos/{id}/tags/{tag}  移除标签

统计：
  GET    /api/todos/stats        统计数据
  GET    /api/todos/overdue      已逾期列表
```

---

## 3. 项目结构

```
src/main/java/com/example/todoapi/
├── TodoApiApplication.java
├── common/
│   ├── Result.java
│   ├── PageResult.java
│   ├── ErrorCode.java
│   ├── AppException.java
│   └── GlobalExceptionHandler.java
├── controller/
│   └── TodoController.java
├── service/
│   ├── TodoService.java
│   └── impl/
│       └── TodoServiceImpl.java
├── repository/
│   └── InMemoryTodoRepository.java
└── model/
    ├── entity/
    │   └── Todo.java
    ├── enums/
    │   ├── TodoStatus.java
    │   └── Priority.java
    └── dto/
        ├── CreateTodoDTO.java
        ├── UpdateTodoDTO.java
        ├── UpdateStatusDTO.java
        ├── TodoQueryDTO.java
        ├── TodoVO.java
        └── TodoStatsVO.java
```

---

## 4. 数据模型与枚举

### TodoStatus.java

```java
package com.example.todoapi.model.enums;

public enum TodoStatus {
    TODO("待办"),
    IN_PROGRESS("进行中"),
    DONE("已完成"),
    CANCELLED("已取消");

    private final String label;

    TodoStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

### Priority.java

```java
package com.example.todoapi.model.enums;

public enum Priority {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    URGENT(4, "紧急");

    private final int level;
    private final String label;

    Priority(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() { return level; }
    public String getLabel() { return label; }
}
```

### Todo.java

```java
package com.example.todoapi.model.entity;

import com.example.todoapi.model.enums.Priority;
import com.example.todoapi.model.enums.TodoStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Todo {
    private Long id;
    private String title;
    private String description;
    private TodoStatus status;
    private Priority priority;
    private LocalDate dueDate;           // 截止日期
    private List<String> tags;           // 标签列表
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 工厂方法
    public static Todo create(Long id, String title, String description,
                               Priority priority, LocalDate dueDate) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setStatus(TodoStatus.TODO);
        todo.setPriority(priority != null ? priority : Priority.MEDIUM);
        todo.setDueDate(dueDate);
        todo.setTags(new ArrayList<>());
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        return todo;
    }

    // 是否已逾期
    public boolean isOverdue() {
        return dueDate != null
            && LocalDate.now().isAfter(dueDate)
            && status != TodoStatus.DONE
            && status != TodoStatus.CANCELLED;
    }
}
```

---

## 5. DTO 设计

### CreateTodoDTO.java

```java
package com.example.todoapi.model.dto;

import com.example.todoapi.model.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTodoDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度 1-100 字符")
    private String title;

    @Size(max = 500, message = "描述最多 500 字符")
    private String description;

    private Priority priority;  // 可选，默认 MEDIUM

    @Future(message = "截止日期必须是未来的日期")
    private LocalDate dueDate;   // 可选
}
```

### UpdateTodoDTO.java

```java
package com.example.todoapi.model.dto;

import com.example.todoapi.model.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTodoDTO {

    @Size(min = 1, max = 100, message = "标题长度 1-100 字符")
    private String title;       // 可选

    @Size(max = 500, message = "描述最多 500 字符")
    private String description; // 可选

    private Priority priority;  // 可选

    private LocalDate dueDate;  // 可选（null 表示清除截止日期）
}
```

### UpdateStatusDTO.java

```java
package com.example.todoapi.model.dto;

import com.example.todoapi.model.enums.TodoStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusDTO {

    @NotNull(message = "状态不能为空")
    private TodoStatus status;
}
```

### TodoQueryDTO.java

```java
package com.example.todoapi.model.dto;

import com.example.todoapi.model.enums.Priority;
import com.example.todoapi.model.enums.TodoStatus;
import lombok.Data;

@Data
public class TodoQueryDTO {
    private TodoStatus status;     // 过滤状态
    private Priority priority;     // 过滤优先级
    private String keyword;        // 标题关键词搜索
    private String tag;            // 按标签过滤
    private int page = 1;
    private int size = 10;
    private String sortBy = "createdAt";   // 排序字段
    private String sortDir = "desc";       // asc/desc
}
```

### TodoVO.java

```java
package com.example.todoapi.model.dto;

import com.example.todoapi.model.entity.Todo;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TodoVO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String statusLabel;
    private String priority;
    private String priorityLabel;
    private LocalDate dueDate;
    private boolean overdue;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TodoVO from(Todo todo) {
        TodoVO vo = new TodoVO();
        vo.setId(todo.getId());
        vo.setTitle(todo.getTitle());
        vo.setDescription(todo.getDescription());
        vo.setStatus(todo.getStatus().name());
        vo.setStatusLabel(todo.getStatus().getLabel());
        vo.setPriority(todo.getPriority().name());
        vo.setPriorityLabel(todo.getPriority().getLabel());
        vo.setDueDate(todo.getDueDate());
        vo.setOverdue(todo.isOverdue());
        vo.setTags(todo.getTags());
        vo.setCreatedAt(todo.getCreatedAt());
        vo.setUpdatedAt(todo.getUpdatedAt());
        return vo;
    }
}
```

### TodoStatsVO.java

```java
package com.example.todoapi.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodoStatsVO {
    private long total;
    private long todoCount;
    private long inProgressCount;
    private long doneCount;
    private long cancelledCount;
    private long overdueCount;
    private long dueTodayCount;
}
```

---

## 6. 异常体系

### ErrorCode.java

```java
package com.example.todoapi.common;

public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突"),
    SERVER_ERROR(500, "服务器内部错误"),

    TODO_NOT_FOUND(100101, "TODO 不存在"),
    INVALID_STATUS_TRANSITION(100102, "状态流转不合法"),
    TAG_ALREADY_EXISTS(100103, "标签已存在"),
    TAG_NOT_FOUND(100104, "标签不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
```

### AppException.java

```java
package com.example.todoapi.common;

public class AppException extends RuntimeException {
    private final int code;
    private final String message;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public AppException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage() + "：" + detail;
    }

    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
```

---

## 7. Repository 层

```java
package com.example.todoapi.repository;

import com.example.todoapi.model.entity.Todo;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public Todo save(Todo todo) {
        if (todo.getId() == null) {
            todo.setId(idGen.getAndIncrement());
        }
        store.put(todo.getId(), todo);
        return todo;
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Todo> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    public long count() {
        return store.size();
    }
}
```

---

## 8. Service 层

### TodoService.java（接口）

```java
package com.example.todoapi.service;

import com.example.todoapi.model.dto.*;
import com.example.todoapi.common.PageResult;

import java.util.List;

public interface TodoService {
    TodoVO createTodo(CreateTodoDTO dto);
    TodoVO getTodoById(Long id);
    PageResult<TodoVO> listTodos(TodoQueryDTO query);
    TodoVO updateTodo(Long id, UpdateTodoDTO dto);
    TodoVO updateStatus(Long id, UpdateStatusDTO dto);
    void deleteTodo(Long id);
    TodoVO addTag(Long id, String tag);
    TodoVO removeTag(Long id, String tag);
    TodoStatsVO getStats();
    List<TodoVO> getOverdue();
}
```

### TodoServiceImpl.java

```java
package com.example.todoapi.service.impl;

import com.example.todoapi.common.*;
import com.example.todoapi.model.dto.*;
import com.example.todoapi.model.entity.Todo;
import com.example.todoapi.model.enums.TodoStatus;
import com.example.todoapi.repository.InMemoryTodoRepository;
import com.example.todoapi.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final InMemoryTodoRepository repository;

    @Override
    public TodoVO createTodo(CreateTodoDTO dto) {
        Todo todo = Todo.create(
            null,
            dto.getTitle(),
            dto.getDescription(),
            dto.getPriority(),
            dto.getDueDate()
        );
        repository.save(todo);
        log.info("TODO 创建成功：id={}, title={}", todo.getId(), todo.getTitle());
        return TodoVO.from(todo);
    }

    @Override
    public TodoVO getTodoById(Long id) {
        Todo todo = repository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id));
        return TodoVO.from(todo);
    }

    @Override
    public PageResult<TodoVO> listTodos(TodoQueryDTO query) {
        List<Todo> all = repository.findAll();

        // 过滤
        List<Todo> filtered = all.stream()
            .filter(t -> query.getStatus() == null || t.getStatus() == query.getStatus())
            .filter(t -> query.getPriority() == null || t.getPriority() == query.getPriority())
            .filter(t -> query.getKeyword() == null || query.getKeyword().isBlank()
                || t.getTitle().contains(query.getKeyword()))
            .filter(t -> query.getTag() == null || t.getTags().contains(query.getTag()))
            .collect(Collectors.toList());

        // 排序
        Comparator<Todo> comparator = switch (query.getSortBy()) {
            case "priority" -> Comparator.comparing(t -> t.getPriority().getLevel());
            case "dueDate" -> Comparator.comparing(
                Todo::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(Todo::getCreatedAt);
        };
        if ("desc".equalsIgnoreCase(query.getSortDir())) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);

        // 分页
        int total = filtered.size();
        int page = Math.max(1, query.getPage());
        int size = Math.min(100, Math.max(1, query.getSize()));
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<TodoVO> records = fromIndex >= total
            ? Collections.emptyList()
            : filtered.subList(fromIndex, toIndex).stream()
                .map(TodoVO::from)
                .collect(Collectors.toList());

        return PageResult.of(records, total, page, size);
    }

    @Override
    public TodoVO updateTodo(Long id, UpdateTodoDTO dto) {
        Todo todo = repository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id));

        if (dto.getTitle() != null) todo.setTitle(dto.getTitle());
        if (dto.getDescription() != null) todo.setDescription(dto.getDescription());
        if (dto.getPriority() != null) todo.setPriority(dto.getPriority());
        // dueDate 允许设置为 null（清除截止日期）
        todo.setDueDate(dto.getDueDate());
        todo.setUpdatedAt(LocalDateTime.now());

        repository.save(todo);
        log.info("TODO 更新成功：id={}", id);
        return TodoVO.from(todo);
    }

    @Override
    public TodoVO updateStatus(Long id, UpdateStatusDTO dto) {
        Todo todo = repository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id));

        // 状态流转校验
        validateStatusTransition(todo.getStatus(), dto.getStatus());

        todo.setStatus(dto.getStatus());
        todo.setUpdatedAt(LocalDateTime.now());
        repository.save(todo);

        log.info("TODO 状态更新：id={}, {} → {}", id, todo.getStatus(), dto.getStatus());
        return TodoVO.from(todo);
    }

    private void validateStatusTransition(TodoStatus from, TodoStatus to) {
        // 已完成和已取消的 TODO 不能再流转
        if (from == TodoStatus.DONE || from == TodoStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                "状态 " + from.getLabel() + " 不能流转到 " + to.getLabel());
        }
    }

    @Override
    public void deleteTodo(Long id) {
        if (!repository.deleteById(id)) {
            throw new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id);
        }
        log.info("TODO 删除成功：id={}", id);
    }

    @Override
    public TodoVO addTag(Long id, String tag) {
        Todo todo = repository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id));

        if (todo.getTags().contains(tag)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS, tag);
        }
        if (todo.getTags().size() >= 10) {
            throw new AppException(ErrorCode.BAD_REQUEST, "最多添加 10 个标签");
        }

        todo.getTags().add(tag);
        todo.setUpdatedAt(LocalDateTime.now());
        repository.save(todo);
        return TodoVO.from(todo);
    }

    @Override
    public TodoVO removeTag(Long id, String tag) {
        Todo todo = repository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.TODO_NOT_FOUND, "id=" + id));

        if (!todo.getTags().remove(tag)) {
            throw new AppException(ErrorCode.TAG_NOT_FOUND, tag);
        }

        todo.setUpdatedAt(LocalDateTime.now());
        repository.save(todo);
        return TodoVO.from(todo);
    }

    @Override
    public TodoStatsVO getStats() {
        List<Todo> all = repository.findAll();
        return TodoStatsVO.builder()
            .total(all.size())
            .todoCount(all.stream().filter(t -> t.getStatus() == TodoStatus.TODO).count())
            .inProgressCount(all.stream().filter(t -> t.getStatus() == TodoStatus.IN_PROGRESS).count())
            .doneCount(all.stream().filter(t -> t.getStatus() == TodoStatus.DONE).count())
            .cancelledCount(all.stream().filter(t -> t.getStatus() == TodoStatus.CANCELLED).count())
            .overdueCount(all.stream().filter(Todo::isOverdue).count())
            .dueTodayCount(all.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isEqual(LocalDate.now()))
                .count())
            .build();
    }

    @Override
    public List<TodoVO> getOverdue() {
        return repository.findAll().stream()
            .filter(Todo::isOverdue)
            .sorted(Comparator.comparing(Todo::getDueDate))
            .map(TodoVO::from)
            .collect(Collectors.toList());
    }
}
```

---

## 9. Controller 层

```java
package com.example.todoapi.controller;

import com.example.todoapi.common.*;
import com.example.todoapi.model.dto.*;
import com.example.todoapi.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "TODO 管理")
@Validated
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "创建 TODO")
    @PostMapping
    public Result<TodoVO> create(@RequestBody @Valid CreateTodoDTO dto) {
        return Result.ok(todoService.createTodo(dto));
    }

    @Operation(summary = "TODO 列表（支持分页和过滤）")
    @GetMapping
    public Result<PageResult<TodoVO>> list(TodoQueryDTO query) {
        return Result.ok(todoService.listTodos(query));
    }

    @Operation(summary = "TODO 详情")
    @GetMapping("/{id}")
    public Result<TodoVO> getById(@PathVariable Long id) {
        return Result.ok(todoService.getTodoById(id));
    }

    @Operation(summary = "更新 TODO")
    @PutMapping("/{id}")
    public Result<TodoVO> update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateTodoDTO dto) {
        return Result.ok(todoService.updateTodo(id, dto));
    }

    @Operation(summary = "更新 TODO 状态")
    @PatchMapping("/{id}/status")
    public Result<TodoVO> updateStatus(@PathVariable Long id,
                                        @RequestBody @Valid UpdateStatusDTO dto) {
        return Result.ok(todoService.updateStatus(id, dto));
    }

    @Operation(summary = "删除 TODO")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return Result.ok();
    }

    @Operation(summary = "添加标签")
    @PostMapping("/{id}/tags")
    public Result<TodoVO> addTag(
        @PathVariable Long id,
        @RequestParam @NotBlank(message = "标签不能为空")
        @Size(max = 20, message = "标签最长 20 字符") String tag
    ) {
        return Result.ok(todoService.addTag(id, tag));
    }

    @Operation(summary = "移除标签")
    @DeleteMapping("/{id}/tags/{tag}")
    public Result<TodoVO> removeTag(@PathVariable Long id, @PathVariable String tag) {
        return Result.ok(todoService.removeTag(id, tag));
    }

    @Operation(summary = "统计数据")
    @GetMapping("/stats")
    public Result<TodoStatsVO> getStats() {
        return Result.ok(todoService.getStats());
    }

    @Operation(summary = "已逾期 TODO")
    @GetMapping("/overdue")
    public Result<List<TodoVO>> getOverdue() {
        return Result.ok(todoService.getOverdue());
    }
}
```

---

## 10. 统一响应与异常处理

### PageResult.java

```java
package com.example.todoapi.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrev;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        int totalPages = (int) Math.ceil((double) total / size);
        result.setTotalPages(totalPages);
        result.setHasNext(page < totalPages);
        result.setHasPrev(page > 1);
        return result;
    }
}
```

### GlobalExceptionHandler.java

```java
package com.example.todoapi.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationFailed(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        log.warn("参数校验失败：{}", errors);
        return Result.fail(400, "参数校验失败", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        return Result.badRequest(message);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(AppException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, WebRequest request) {
        log.error("未处理异常，请求：{}", request.getDescription(false), e);
        return Result.serverError("服务器内部错误，请稍后重试");
    }
}
```

---

## 11. Postman 测试

### 推荐测试顺序

**Step 1：创建 TODO**

```
POST /api/todos
{
    "title": "学习 Spring Boot",
    "description": "完成 2-07 里程碑项目",
    "priority": "HIGH",
    "dueDate": "2026-12-31"
}
```

**Step 2：查询列表（默认分页）**

```
GET /api/todos
GET /api/todos?page=1&size=5
GET /api/todos?status=TODO
GET /api/todos?priority=HIGH
GET /api/todos?keyword=Spring
```

**Step 3：查看详情**

```
GET /api/todos/1
GET /api/todos/999  → 应该返回 404
```

**Step 4：更新内容**

```
PUT /api/todos/1
{
    "title": "深入学习 Spring Boot",
    "priority": "URGENT"
}
```

**Step 5：更新状态**

```
PATCH /api/todos/1/status
{"status": "IN_PROGRESS"}

PATCH /api/todos/1/status
{"status": "DONE"}

PATCH /api/todos/1/status
{"status": "TODO"}  → 应该返回 409（已完成不能流转）
```

**Step 6：标签操作**

```
POST /api/todos/1/tags?tag=学习
POST /api/todos/1/tags?tag=重要
DELETE /api/todos/1/tags/重要
```

**Step 7：统计**

```
GET /api/todos/stats
GET /api/todos/overdue
```

**Step 8：校验测试**

```
POST /api/todos
{}
→ 应该返回 400 + 各字段错误信息

POST /api/todos
{"title": "a", "dueDate": "2020-01-01"}
→ 截止日期是过去，应返回 400
```

---

## 12. 自测清单

**基本 CRUD：**

- [ ] 创建 TODO 返回完整信息（含自动生成的 ID、createdAt）
- [ ] 查询列表支持分页，第 2 页数据和第 1 页不重叠
- [ ] 按状态过滤只返回对应状态的 TODO
- [ ] 关键词搜索能找到标题包含关键词的 TODO
- [ ] 更新时只传部分字段，其他字段保持不变
- [ ] 删除后再查询返回 404

**状态流转：**

- [ ] TODO → IN_PROGRESS → DONE 流转成功
- [ ] DONE 状态再次流转返回 409 错误
- [ ] CANCELLED 状态再次流转返回 409 错误

**标签：**

- [ ] 添加标签成功，返回的 tags 包含新标签
- [ ] 重复添加同一标签返回 409
- [ ] 移除不存在的标签返回 404
- [ ] 按标签过滤只返回含该标签的 TODO

**校验：**

- [ ] 标题为空返回 400 + 错误提示
- [ ] 截止日期为过去日期返回 400
- [ ] 传入不存在的 status/priority 枚举值返回 400

**统计：**

- [ ] 各状态计数与实际数据一致
- [ ] 创建一个过去截止日期的 TODO（绕过校验直接构造），overdue 列表包含它

---

## 13. 扩展挑战

**⭐ 基础：**

1. 添加批量删除接口：`DELETE /api/todos/batch`，接收 ID 列表
2. 实现 TODO 复制：`POST /api/todos/{id}/copy`，复制一个 TODO（状态重置为 TODO）
3. 排序支持多字段：`?sortBy=priority,dueDate&sortDir=desc,asc`

**⭐⭐ 中级：** 4. 添加搜索高亮：查询结果中把匹配的关键词用 `<em>` 标签包裹 5. 实现 TODO 的优先级排序列表：`GET /api/todos/priority-sorted`，按优先级降序，同优先级按截止日期升序 6. 添加操作历史记录：每次状态变更记录（谁在何时改成了什么状态）

**⭐⭐⭐ 高级：** 7. 实现导入/导出：`GET /api/todos/export` 导出为 CSV，`POST /api/todos/import` 从 CSV 导入 8. 实现全文搜索：同时在标题、描述、标签中搜索关键词，支持 OR 语义

---

> 🎉 **里程碑完成！**  
> 恭喜你构建了第一个完整的 REST API，虽然数据还在内存里，但架构已经完全正规。  
> 下一节开始接入真实的 MySQL 数据库。

# 2-08 · MySQL 入门

> **目标**：安装并配置 MySQL，掌握 SQL 基础语法（DDL/DML/DQL），能独立设计数据库表并完成增删改查操作。  
> **预计时间**：4-5 小时  
> **前置要求**：完成 2-07，了解关系型数据库基本概念

---

## 目录

- [安装 MySQL](#1-安装-mysql)
- [数据库基本概念](#2-数据库基本概念)
- [DDL：数据定义语言](#3-ddl数据定义语言)
- [DML：数据操作语言](#4-dml数据操作语言)
- [DQL：数据查询语言](#5-dql数据查询语言)
- [常用函数](#6-常用函数)
- [事务基础](#7-事务基础)
- [在 IDEA 中连接数据库](#8-在-idea-中连接数据库)
- [和 JavaScript 的对比](#9-和-javascript-的对比)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. 安装 MySQL

### 1.1 方式一：Docker 安装（推荐）

Docker 安装最简单，不污染本机环境，版本可控：

```bash
# 拉取 MySQL 8 镜像
docker pull mysql:8.0

# 启动容器
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123456 \
  -e MYSQL_DATABASE=blog_dev \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

# 验证是否运行
docker ps | grep mysql8
```

### 1.2 方式二：直接安装

1. 下载 [https://dev.mysql.com/downloads/mysql/](https://dev.mysql.com/downloads/mysql/)，选 8.0.x MSI Installer
2. 安装时选 Developer Default，设置 root 密码
3. 安装完后在终端验证：

```bash
mysql -u root -p
# 输入密码后进入 MySQL 命令行
```

### 1.3 安装 MySQL Workbench（图形界面工具）

[https://dev.mysql.com/downloads/workbench/](https://dev.mysql.com/downloads/workbench/)

或者使用 DBeaver（更推荐，支持多种数据库）：
[https://dbeaver.io/download/](https://dbeaver.io/download/)

---

## 2. 数据库基本概念

```
MySQL 实例
  └── 数据库（Database）  ← 相当于一个项目的数据集合
        └── 表（Table）    ← 相当于 Excel 的一个 Sheet
              ├── 列（Column）  ← 字段，定义数据结构
              └── 行（Row）     ← 记录，实际的数据
```

### 数据类型

**数值类型：**

| 类型            | 大小   | 范围       | 场景               |
| --------------- | ------ | ---------- | ------------------ |
| `TINYINT`       | 1 字节 | -128~127   | 状态标志           |
| `INT`           | 4 字节 | -21亿~21亿 | 普通整数、ID       |
| `BIGINT`        | 8 字节 | 极大       | 分布式ID、时间戳   |
| `DECIMAL(10,2)` | 可变   | 精确小数   | 金额（不用 FLOAT） |
| `DOUBLE`        | 8 字节 | 近似小数   | 坐标、比例         |

**字符串类型：**

| 类型         | 说明                    | 场景           |
| ------------ | ----------------------- | -------------- |
| `CHAR(n)`    | 定长，最多 255 字符     | 身份证号、邮编 |
| `VARCHAR(n)` | 变长，最多 65535 字符   | 姓名、标题     |
| `TEXT`       | 大文本，最多 65535 字符 | 文章内容       |
| `LONGTEXT`   | 超大文本，4GB           | 超长内容       |

**日期时间：**

| 类型        | 格式                  | 场景               |
| ----------- | --------------------- | ------------------ |
| `DATE`      | `2026-06-19`          | 生日、截止日期     |
| `DATETIME`  | `2026-06-19 10:30:00` | 创建时间、更新时间 |
| `TIMESTAMP` | `2026-06-19 10:30:00` | 自动更新时间       |

---

## 3. DDL：数据定义语言

DDL（Data Definition Language）用于定义数据库结构。

### 3.1 数据库操作

```sql
-- 创建数据库
CREATE DATABASE blog_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 查看所有数据库
SHOW DATABASES;

-- 选择数据库
USE blog_dev;

-- 查看当前数据库
SELECT DATABASE();

-- 删除数据库（慎用！）
DROP DATABASE IF EXISTS blog_dev;
```

### 3.2 建表

```sql
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL                  COMMENT '用户名',
    `email`       VARCHAR(100) NOT NULL                  COMMENT '邮箱',
    `password_hash` VARCHAR(255) NOT NULL                COMMENT '密码哈希',
    `nickname`    VARCHAR(50)  DEFAULT NULL              COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL              COMMENT '头像URL',
    `status`      TINYINT      NOT NULL DEFAULT 1        COMMENT '状态：1=正常 0=禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**关键点说明：**

```sql
AUTO_INCREMENT          → 自增主键
NOT NULL                → 非空约束
DEFAULT CURRENT_TIMESTAMP → 默认当前时间
ON UPDATE CURRENT_TIMESTAMP → 更新时自动更新时间
PRIMARY KEY             → 主键
UNIQUE KEY              → 唯一约束（会自动创建索引）
INDEX                   → 普通索引
ENGINE=InnoDB           → 存储引擎（支持事务）
CHARSET=utf8mb4         → 字符集（支持 emoji）
```

### 3.3 文章表

```sql
CREATE TABLE `article` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '文章ID',
    `title`       VARCHAR(200) NOT NULL                  COMMENT '标题',
    `content`     LONGTEXT     NOT NULL                  COMMENT '内容',
    `summary`     VARCHAR(500) DEFAULT NULL              COMMENT '摘要',
    `author_id`   BIGINT       NOT NULL                  COMMENT '作者ID',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'  COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    `view_count`  INT          NOT NULL DEFAULT 0        COMMENT '浏览量',
    `like_count`  INT          NOT NULL DEFAULT 0        COMMENT '点赞数',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_author_id` (`author_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';
```

### 3.4 修改表结构

```sql
-- 添加列
ALTER TABLE `article` ADD COLUMN `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图' AFTER `summary`;

-- 修改列定义
ALTER TABLE `article` MODIFY COLUMN `title` VARCHAR(300) NOT NULL COMMENT '标题';

-- 重命名列（MySQL 8.0+）
ALTER TABLE `article` RENAME COLUMN `like_count` TO `likes`;

-- 删除列
ALTER TABLE `article` DROP COLUMN `likes`;

-- 添加索引
ALTER TABLE `article` ADD INDEX `idx_title` (`title`);

-- 删除索引
ALTER TABLE `article` DROP INDEX `idx_title`;

-- 删除表
DROP TABLE IF EXISTS `article`;
```

---

## 4. DML：数据操作语言

DML（Data Manipulation Language）用于操作数据。

### 4.1 INSERT 插入

```sql
-- 插入单条
INSERT INTO `user` (username, email, password_hash, nickname)
VALUES ('zhangsan', 'zs@example.com', '$2b$12$hash...', '张三');

-- 插入多条
INSERT INTO `user` (username, email, password_hash)
VALUES
    ('lisi', 'ls@example.com', '$2b$12$hash1'),
    ('wangwu', 'ww@example.com', '$2b$12$hash2'),
    ('zhaoliu', 'zl@example.com', '$2b$12$hash3');

-- 插入或更新（主键/唯一键冲突时更新）
INSERT INTO `user` (username, email, password_hash)
VALUES ('zhangsan', 'zs@example.com', '$2b$12$hash')
ON DUPLICATE KEY UPDATE email = VALUES(email);
```

### 4.2 UPDATE 更新

```sql
-- 更新单个字段
UPDATE `user` SET nickname = '张大三' WHERE id = 1;

-- 更新多个字段
UPDATE `user`
SET nickname = '张大三', avatar = 'https://example.com/avatar.jpg'
WHERE id = 1;

-- 批量更新
UPDATE `article`
SET status = 'ARCHIVED'
WHERE author_id = 1 AND status = 'PUBLISHED';

-- 自增/自减
UPDATE `article` SET view_count = view_count + 1 WHERE id = 1;
```

> ⚠️ **UPDATE 一定要加 WHERE！** 没有 WHERE 会更新所有行，这是最常见的生产事故之一。

### 4.3 DELETE 删除

```sql
-- 按条件删除
DELETE FROM `user` WHERE id = 10;

-- 删除多条
DELETE FROM `article` WHERE status = 'ARCHIVED' AND created_at < '2024-01-01';

-- 清空表（保留表结构）
TRUNCATE TABLE `article`;  -- 比 DELETE 快，但不能回滚
```

> ⚠️ **DELETE 也要加 WHERE！** 实际项目通常用"软删除"（加 `is_deleted` 字段，DELETE 改为 UPDATE）。

---

## 5. DQL：数据查询语言

DQL（Data Query Language）是 SQL 中最常用、最复杂的部分。

### 5.1 基础查询

```sql
-- 查所有字段
SELECT * FROM `user`;

-- 查指定字段
SELECT id, username, email FROM `user`;

-- 带条件
SELECT * FROM `user` WHERE status = 1;

-- 去重
SELECT DISTINCT status FROM `article`;

-- 别名
SELECT username AS name, email AS mail FROM `user`;
```

### 5.2 WHERE 条件

```sql
-- 比较运算符
WHERE age > 18
WHERE status != 0
WHERE created_at >= '2026-01-01'

-- 逻辑运算符
WHERE status = 1 AND age > 18
WHERE status = 0 OR email IS NULL

-- IN / NOT IN
WHERE status IN (1, 2, 3)
WHERE id NOT IN (1, 5, 10)

-- BETWEEN
WHERE age BETWEEN 18 AND 60
WHERE created_at BETWEEN '2026-01-01' AND '2026-12-31'

-- LIKE 模糊查询
WHERE username LIKE '张%'      -- 以"张"开头
WHERE email LIKE '%@gmail.com' -- 以 @gmail.com 结尾
WHERE title LIKE '%Spring%'    -- 包含"Spring"

-- IS NULL / IS NOT NULL
WHERE nickname IS NULL
WHERE avatar IS NOT NULL

-- CASE WHEN（条件表达式）
SELECT id, username,
    CASE status
        WHEN 1 THEN '正常'
        WHEN 0 THEN '禁用'
        ELSE '未知'
    END AS status_label
FROM `user`;
```

### 5.3 ORDER BY 排序

```sql
-- 单字段升序（默认）
SELECT * FROM `article` ORDER BY created_at ASC;

-- 单字段降序
SELECT * FROM `article` ORDER BY created_at DESC;

-- 多字段排序
SELECT * FROM `article`
ORDER BY status ASC, created_at DESC;

-- 按表达式排序
SELECT * FROM `article`
ORDER BY (like_count + view_count) DESC;
```

### 5.4 LIMIT 分页

```sql
-- 取前 10 条
SELECT * FROM `article` LIMIT 10;

-- 跳过 20 条，取 10 条（第 3 页，每页 10 条）
SELECT * FROM `article` LIMIT 20, 10;
-- 或者（MySQL 5.0+ 推荐写法）
SELECT * FROM `article` LIMIT 10 OFFSET 20;

-- 分页公式
-- offset = (page - 1) * size
-- 第 1 页：LIMIT 10 OFFSET 0
-- 第 2 页：LIMIT 10 OFFSET 10
-- 第 3 页：LIMIT 10 OFFSET 20
```

### 5.5 聚合函数与 GROUP BY

```sql
-- 聚合函数
SELECT COUNT(*) FROM `user`;                          -- 总数
SELECT COUNT(nickname) FROM `user`;                   -- 非 NULL 的数量
SELECT MAX(view_count) FROM `article`;                -- 最大值
SELECT MIN(created_at) FROM `article`;                -- 最小值
SELECT SUM(view_count) FROM `article`;                -- 求和
SELECT AVG(view_count) FROM `article`;                -- 平均值

-- GROUP BY 分组统计
SELECT status, COUNT(*) AS count
FROM `article`
GROUP BY status;

-- HAVING（对分组结果过滤，相当于 GROUP BY 后的 WHERE）
SELECT author_id, COUNT(*) AS article_count
FROM `article`
GROUP BY author_id
HAVING article_count >= 5
ORDER BY article_count DESC;
```

### 5.6 JOIN 关联查询

```sql
-- INNER JOIN：只返回两表都有匹配的行
SELECT a.id, a.title, u.username AS author_name
FROM `article` a
INNER JOIN `user` u ON a.author_id = u.id
WHERE a.status = 'PUBLISHED'
ORDER BY a.created_at DESC;

-- LEFT JOIN：返回左表所有行，右表没有匹配则为 NULL
SELECT u.username, COUNT(a.id) AS article_count
FROM `user` u
LEFT JOIN `article` a ON u.id = a.author_id
GROUP BY u.id, u.username
ORDER BY article_count DESC;

-- 多表关联
SELECT a.title, u.username, c.name AS category_name
FROM `article` a
JOIN `user` u ON a.author_id = u.id
LEFT JOIN `category` c ON a.category_id = c.id
WHERE a.status = 'PUBLISHED';
```

### 5.7 子查询

```sql
-- WHERE 子查询：找出发过文章的用户
SELECT * FROM `user`
WHERE id IN (
    SELECT DISTINCT author_id FROM `article`
);

-- 相关子查询：找出每个用户发表的最新文章
SELECT a.* FROM `article` a
WHERE a.created_at = (
    SELECT MAX(a2.created_at)
    FROM `article` a2
    WHERE a2.author_id = a.author_id
);

-- FROM 子查询
SELECT t.status, t.count FROM (
    SELECT status, COUNT(*) AS count
    FROM `article`
    GROUP BY status
) t
WHERE t.count > 1;
```

---

## 6. 常用函数

### 字符串函数

```sql
SELECT LENGTH('hello');                    -- 5（字节数）
SELECT CHAR_LENGTH('你好');                -- 2（字符数）
SELECT UPPER('hello');                     -- HELLO
SELECT LOWER('HELLO');                     -- hello
SELECT TRIM('  hello  ');                  -- 'hello'
SELECT SUBSTRING('hello world', 7, 5);    -- 'world'
SELECT CONCAT('hello', ' ', 'world');     -- 'hello world'
SELECT REPLACE('hello world', 'world', 'java'); -- 'hello java'
SELECT LIKE('%hello%', 'say hello world'); -- 1（true）
```

### 日期函数

```sql
SELECT NOW();                              -- 2026-06-19 10:30:00（当前时间）
SELECT CURDATE();                          -- 2026-06-19（当前日期）
SELECT DATE(NOW());                        -- 2026-06-19
SELECT YEAR(NOW());                        -- 2026
SELECT MONTH(NOW());                       -- 6
SELECT DAY(NOW());                         -- 19
SELECT DATE_FORMAT(NOW(), '%Y年%m月%d日'); -- '2026年06月19日'
SELECT DATE_ADD(NOW(), INTERVAL 7 DAY);   -- 7天后
SELECT DATEDIFF('2026-12-31', NOW());     -- 距离年底的天数
```

### 数值函数

```sql
SELECT ABS(-10);       -- 10
SELECT CEIL(3.1);      -- 4
SELECT FLOOR(3.9);     -- 3
SELECT ROUND(3.567, 2); -- 3.57
SELECT RAND();         -- 0~1 的随机数
SELECT MOD(10, 3);     -- 1（取余）
```

---

## 7. 事务基础

```sql
-- 开启事务
START TRANSACTION;

-- 执行操作
UPDATE account SET balance = balance - 1000 WHERE id = 1;
UPDATE account SET balance = balance + 1000 WHERE id = 2;

-- 提交（所有操作生效）
COMMIT;

-- 或者回滚（所有操作撤销）
ROLLBACK;
```

**事务四大特性（ACID）：**

| 特性                  | 含义                       |
| --------------------- | -------------------------- |
| Atomicity（原子性）   | 要么全部成功，要么全部失败 |
| Consistency（一致性） | 事务前后数据保持一致状态   |
| Isolation（隔离性）   | 事务之间互不干扰           |
| Durability（持久性）  | 提交的事务永久保存         |

---

## 8. 在 IDEA 中连接数据库

IDEA Ultimate 内置 Database 工具，Community 版可以安装 Database Navigator 插件。

### 8.1 连接步骤

1. 右侧边栏 → Database → `+` → Data Source → MySQL
2. 填写：
   - Host: `localhost`
   - Port: `3306`
   - User: `root`
   - Password: `root123456`
   - Database: `blog_dev`
3. 点击 Test Connection，成功后点 OK
4. 在 Database 面板可以直接写 SQL 并执行

### 8.2 初始化脚本

在项目的 `src/main/resources/` 下创建 `schema.sql`，把建表语句放进去，方便团队共享：

```sql
-- schema.sql
CREATE DATABASE IF NOT EXISTS blog_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE blog_dev;

CREATE TABLE IF NOT EXISTS `user` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `username`     VARCHAR(50) NOT NULL,
    `email`        VARCHAR(100) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname`     VARCHAR(50)  DEFAULT NULL,
    `status`       TINYINT     NOT NULL DEFAULT 1,
    `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据
INSERT INTO `user` (username, email, password_hash, nickname) VALUES
('admin', 'admin@example.com', '$2b$12$placeholder', '管理员'),
('zhangsan', 'zs@example.com', '$2b$12$placeholder', '张三'),
('lisi', 'ls@example.com', '$2b$12$placeholder', '李四');
```

---

## 9. 和 JavaScript 的对比

如果用过 Sequelize 或 Prisma，SQL 的概念基本一致：

```javascript
// Sequelize / Prisma 查询（ORM）
const users = await User.findAll({
    where: { status: 1 },
    order: [['createdAt', 'DESC']],
    limit: 10,
    offset: 0,
});

// 等价的 SQL
SELECT * FROM `user`
WHERE status = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;
```

```javascript
// Sequelize 关联查询
const articles = await Article.findAll({
    include: [{ model: User, as: 'author' }],
    where: { status: 'PUBLISHED' },
});

// 等价的 SQL
SELECT a.*, u.username, u.nickname
FROM article a
JOIN user u ON a.author_id = u.id
WHERE a.status = 'PUBLISHED';
```

ORM 帮你生成 SQL，但底层还是 SQL。理解 SQL 才能在出问题时排查、优化慢查询。

---

## 练习

### 练习 1：建表与初始化

为博客系统创建完整的数据库表：

1. 按本节示例创建 `user` 和 `article` 表
2. 创建 `category`（分类）表和 `tag`（标签）表
3. 创建 `article_tag`（文章标签多对多关联）表
4. 插入至少 3 个用户、10 篇文章、5 个标签、分配标签关系的测试数据

### 练习 2：基础 SQL 查询

基于练习 1 的数据，完成以下查询：

1. 查询所有已发布的文章（标题、作者名、创建时间）
2. 查询浏览量最高的 5 篇文章
3. 统计每个作者发布的文章数量，按数量降序
4. 查找本月创建的文章
5. 查找标题中包含某关键词的文章

### 练习 3：复杂查询

1. 查询每个用户的文章数量（包括没有文章的用户，数量显示 0）
2. 查询拥有某个特定标签的所有文章（需要 JOIN article_tag 和 tag 表）
3. 查询每种状态的文章数量和平均浏览量
4. 实现分页查询：第 2 页，每页 5 条，按创建时间倒序

### 练习 4：DML 实战

1. 批量更新：把某作者 2025 年之前的文章状态改为 ARCHIVED
2. 安全删除：用 is_deleted 字段实现软删除（先给 article 表加 is_deleted 列，再写软删除语句）
3. 统计更新：文章浏览量 +1（UPDATE 语句）
4. 事务演示：用 START TRANSACTION 模拟文章发布流程（更新文章状态 + 更新作者发文统计）

---

## 常见问题

**Q：CHAR 和 VARCHAR 怎么选？**  
A：长度固定的用 CHAR（如手机号 11 位、MD5 哈希 32 位），长度可变的用 VARCHAR（如姓名、标题）。CHAR 读取速度略快但占空间，VARCHAR 节省空间但有额外长度开销。

**Q：为什么要用 utf8mb4 而不是 utf8？**  
A：MySQL 的 `utf8` 实际上只支持最多 3 字节的 UTF-8 字符，无法存储 emoji（emoji 是 4 字节）。`utf8mb4` 才是真正完整的 UTF-8，支持所有 Unicode 字符包括 emoji。新项目统一用 utf8mb4。

**Q：金额为什么用 DECIMAL 而不是 FLOAT/DOUBLE？**  
A：FLOAT 和 DOUBLE 是二进制浮点数，存在精度丢失问题（如 `0.1 + 0.2 ≠ 0.3`）。DECIMAL 是十进制精确数值，不会有精度问题。金额计算必须用 DECIMAL，Java 对应 `BigDecimal`。

**Q：软删除是什么，为什么要用软删除？**  
A：软删除是不真正删除数据，而是标记 `is_deleted = 1`。好处：可以恢复数据、保留操作记录、防止误删。代价：查询时需要加 `WHERE is_deleted = 0`，MyBatis Plus 提供了全局逻辑删除支持，2-12 会讲。

**Q：什么时候建索引？**  
A：WHERE 条件中频繁出现的字段、JOIN 的关联字段、ORDER BY 的排序字段。但索引会影响写入性能（每次写入都要维护索引），所以不是越多越好。主键自动有索引，唯一约束自动有索引。

---

## 下一步

下一节 **[2-09 · SQL 进阶](../2-09/README.md)** 深入 JOIN 的各种用法、子查询优化、索引原理与慢查询分析，为 MyBatis 开发打好 SQL 基础。

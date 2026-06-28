# 2-12 · MyBatis Plus 进阶

> **目标**：掌握 MyBatis Plus 的进阶特性——XML 自定义 SQL、逻辑删除、乐观锁、多表关联查询，能处理实际项目中的复杂数据库操作。  
> **预计时间**：4-5 小时  
> **前置要求**：完成 2-11，MyBatis Plus 基础 CRUD 已掌握

---

## 目录

- [XML 自定义 SQL](#1-xml-自定义-sql)
- [注解方式自定义 SQL](#2-注解方式自定义-sql)
- [多表关联查询](#3-多表关联查询)
- [逻辑删除详解](#4-逻辑删除详解)
- [乐观锁](#5-乐观锁)
- [自动填充详解](#6-自动填充详解)
- [分页进阶](#7-分页进阶)
- [批量操作优化](#8-批量操作优化)
- [练习](#练习)
- [常见问题](#常见问题)
- [下一步](#下一步)

---

## 1. XML 自定义 SQL

当条件构造器无法满足需求时（复杂 JOIN、子查询、窗口函数），用 XML Mapper 手写 SQL。

### 1.1 创建 XML 文件

在 `src/main/resources/mapper/` 目录下创建 `ArticleMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.blogapi.mapper.ArticleMapper">

    <!-- 结果映射：文章详情（含作者、分类、标签信息）-->
    <resultMap id="ArticleDetailMap" type="com.example.blogapi.model.vo.ArticleDetailVO">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="summary" column="summary"/>
        <result property="coverImage" column="cover_image"/>
        <result property="status" column="status"/>
        <result property="viewCount" column="view_count"/>
        <result property="likeCount" column="like_count"/>
        <result property="commentCount" column="comment_count"/>
        <result property="publishedAt" column="published_at"/>
        <result property="createdAt" column="created_at"/>
        <!-- 作者信息 -->
        <result property="authorId" column="author_id"/>
        <result property="authorName" column="author_name"/>
        <result property="authorAvatar" column="author_avatar"/>
        <!-- 分类信息 -->
        <result property="categoryId" column="category_id"/>
        <result property="categoryName" column="category_name"/>
        <!-- 标签列表（collection 处理一对多）-->
        <collection property="tags" ofType="String">
            <constructor>
                <arg column="tag_name"/>
            </constructor>
        </collection>
    </resultMap>

    <!-- 查询文章详情（含关联数据）-->
    <select id="selectArticleDetail" resultMap="ArticleDetailMap">
        SELECT
            a.id, a.title, a.content, a.summary, a.cover_image,
            a.status, a.view_count, a.like_count, a.comment_count,
            a.published_at, a.created_at,
            a.author_id, u.nickname AS author_name, u.avatar AS author_avatar,
            a.category_id, c.name AS category_name,
            t.name AS tag_name
        FROM article a
        LEFT JOIN user u         ON a.author_id   = u.id   AND u.is_deleted = 0
        LEFT JOIN category c     ON a.category_id = c.id   AND c.is_deleted = 0
        LEFT JOIN article_tag at ON a.id          = at.article_id
        LEFT JOIN tag t          ON at.tag_id      = t.id
        WHERE a.id = #{id} AND a.is_deleted = 0
    </select>

    <!-- 文章列表（支持动态条件）-->
    <select id="selectArticleList" resultType="com.example.blogapi.model.vo.ArticleVO">
        SELECT
            a.id, a.title, a.summary, a.cover_image,
            a.author_id, a.author_name, u.avatar AS author_avatar,
            a.category_id, c.name AS category_name,
            a.status, a.view_count, a.like_count, a.comment_count,
            a.is_top, a.published_at
        FROM article a
        LEFT JOIN user u     ON a.author_id   = u.id AND u.is_deleted = 0
        LEFT JOIN category c ON a.category_id = c.id AND c.is_deleted = 0
        <where>
            a.is_deleted = 0
            <if test="status != null and status != ''">
                AND a.status = #{status}
            </if>
            <if test="authorId != null">
                AND a.author_id = #{authorId}
            </if>
            <if test="categoryId != null">
                AND a.category_id = #{categoryId}
            </if>
            <if test="keyword != null and keyword != ''">
                AND a.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
        </where>
        ORDER BY a.is_top DESC, a.published_at DESC
    </select>

    <!-- 统计各状态文章数量 -->
    <select id="countByStatus" resultType="com.example.blogapi.model.vo.ArticleStatusCountVO">
        SELECT status, COUNT(*) AS count
        FROM article
        WHERE is_deleted = 0
        <if test="authorId != null">
            AND author_id = #{authorId}
        </if>
        GROUP BY status
    </select>

    <!-- 更新浏览量（原子操作）-->
    <update id="incrementViewCount">
        UPDATE article SET view_count = view_count + 1 WHERE id = #{id} AND is_deleted = 0
    </update>

</mapper>
```

### 1.2 Mapper 接口对应方法

```java
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    // 查询文章详情（含关联信息）
    ArticleDetailVO selectArticleDetail(@Param("id") Long id);

    // 文章列表（动态条件）
    List<ArticleVO> selectArticleList(@Param("status") String status,
                                       @Param("authorId") Long authorId,
                                       @Param("categoryId") Integer categoryId,
                                       @Param("keyword") String keyword);

    // 带分页的文章列表
    IPage<ArticleVO> selectArticleListPage(IPage<ArticleVO> page,
                                            @Param("status") String status,
                                            @Param("keyword") String keyword);

    // 统计各状态数量
    List<ArticleStatusCountVO> countByStatus(@Param("authorId") Long authorId);

    // 更新浏览量
    int incrementViewCount(@Param("id") Long id);
}
```

### 1.3 动态 SQL 标签

```xml
<!-- if：条件判断 -->
<if test="keyword != null and keyword != ''">
    AND title LIKE CONCAT('%', #{keyword}, '%')
</if>

<!-- where：自动处理多余的 AND/OR -->
<where>
    <if test="status != null">AND status = #{status}</if>
    <if test="authorId != null">AND author_id = #{authorId}</if>
</where>

<!-- choose/when/otherwise：类似 Java 的 if-else if-else -->
<choose>
    <when test="sortBy == 'viewCount'">ORDER BY view_count DESC</when>
    <when test="sortBy == 'likeCount'">ORDER BY like_count DESC</when>
    <otherwise>ORDER BY published_at DESC</otherwise>
</choose>

<!-- foreach：IN 条件、批量操作 -->
<where>
    id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</where>

<!-- set：UPDATE 语句的动态 SET -->
<update id="updateArticle">
    UPDATE article
    <set>
        <if test="title != null">title = #{title},</if>
        <if test="content != null">content = #{content},</if>
        <if test="status != null">status = #{status},</if>
    </set>
    WHERE id = #{id}
</update>

<!-- trim：自定义前缀/后缀处理 -->
<trim prefix="WHERE" prefixOverrides="AND|OR">
    <if test="status != null">AND status = #{status}</if>
    <if test="keyword != null">AND title LIKE CONCAT('%', #{keyword}, '%')</if>
</trim>
```

---

## 2. 注解方式自定义 SQL

简单 SQL 可以直接用注解，不需要 XML 文件：

```java
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    // 简单查询用注解
    @Select("SELECT * FROM article WHERE author_id = #{authorId} AND is_deleted = 0")
    List<Article> findByAuthorId(@Param("authorId") Long authorId);

    // 动态 SQL 注解（用 <script> 标签）
    @Select("""
        <script>
        SELECT id, title, author_name, view_count, published_at
        FROM article
        WHERE is_deleted = 0
        <if test="status != null">AND status = #{status}</if>
        ORDER BY published_at DESC
        </script>
        """)
    List<Article> findByStatusDynamic(@Param("status") String status);

    // 查询结果映射到 Map
    @Select("SELECT status, COUNT(*) as cnt FROM article GROUP BY status")
    @MapKey("status")
    Map<String, Map<String, Object>> groupByStatus();

    // 更新注解
    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementView(@Param("id") Long id);

    // 插入后返回主键（注解方式）
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO article(title, content, author_id) VALUES(#{title}, #{content}, #{authorId})")
    int insertArticle(Article article);
}
```

**XML vs 注解的选择：**

| 场景                                    | 推荐                   |
| --------------------------------------- | ---------------------- |
| 简单 SQL（单表、无动态条件）            | 注解                   |
| 复杂 SQL（多表 JOIN、动态条件、子查询） | XML                    |
| 团队项目                                | 统一用 XML（更易维护） |

---

## 3. 多表关联查询

MyBatis Plus 的 BaseMapper 只支持单表，多表查询需要自定义 SQL。

### 3.1 结果对象设计

```java
// 文章详情 VO（含关联信息）
@Data
public class ArticleDetailVO {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    // 作者信息（来自 user 表）
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    // 分类信息（来自 category 表）
    private Integer categoryId;
    private String categoryName;

    // 标签列表（来自 tag 表，一对多）
    private List<String> tags;

    // 是否已点赞（需要传入当前用户ID才能判断）
    private Boolean liked;
    private Boolean favorited;
}
```

### 3.2 Service 中调用

```java
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticleService {

    @Override
    public ArticleDetailVO getArticleDetail(Long id, Long currentUserId) {
        ArticleDetailVO vo = baseMapper.selectArticleDetail(id);
        if (vo == null) {
            throw new AppException(ErrorCode.ARTICLE_NOT_FOUND, "id=" + id);
        }

        // 浏览量 +1（异步，不影响主流程）
        baseMapper.incrementViewCount(id);

        // 如果用户已登录，查询是否点赞/收藏
        if (currentUserId != null) {
            vo.setLiked(articleLikeMapper.exists(
                new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, currentUserId)
                    .eq(ArticleLike::getArticleId, id)
            ));
            vo.setFavorited(articleFavoriteMapper.exists(
                new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, currentUserId)
                    .eq(ArticleFavorite::getArticleId, id)
            ));
        }

        return vo;
    }
}
```

### 3.3 分页多表查询（XML + Page）

```xml
<!-- ArticleMapper.xml -->
<select id="selectArticleListPage" resultType="com.example.blogapi.model.vo.ArticleVO">
    SELECT
        a.id, a.title, a.summary, a.cover_image,
        a.author_id, a.author_name, u.avatar AS author_avatar,
        c.name AS category_name,
        a.view_count, a.like_count, a.comment_count,
        a.is_top, a.published_at
    FROM article a
    LEFT JOIN user u ON a.author_id = u.id AND u.is_deleted = 0
    LEFT JOIN category c ON a.category_id = c.id
    <where>
        a.is_deleted = 0 AND a.status = 'PUBLISHED'
        <if test="keyword != null and keyword != ''">
            AND a.title LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
    ORDER BY a.is_top DESC, a.published_at DESC
</select>
```

```java
// Mapper 接口
IPage<ArticleVO> selectArticleListPage(IPage<ArticleVO> page,
                                        @Param("keyword") String keyword);

// Service 调用
Page<ArticleVO> result = baseMapper.selectArticleListPage(
    new Page<>(page, size), keyword
);
```

> ⚠️ **注意**：带标签 tags 的一对多查询，如果用分页，会因为 GROUP_CONCAT/collection 导致分页数量不对。推荐分两步：先分页查主数据，再批量查标签，然后在 Java 代码中组装。

---

## 4. 逻辑删除详解

### 4.1 全局配置（已在 2-11 配置）

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 4.2 实体类注解

```java
@TableLogic  // 标记逻辑删除字段
private Integer isDeleted;
```

### 4.3 效果验证

```java
// 执行 removeById(1L)
// 实际 SQL：UPDATE user SET is_deleted=1 WHERE id=1 AND is_deleted=0

// 执行 selectById(1L)
// 实际 SQL：SELECT * FROM user WHERE id=1 AND is_deleted=0
// 已删除的数据查不到

// 执行 selectList(null)
// 实际 SQL：SELECT * FROM user WHERE is_deleted=0
// 自动过滤已删除数据
```

### 4.4 查询已删除数据

```java
// 需要包含已删除数据时，需要关闭逻辑删除过滤
List<User> allUsers = userMapper.selectList(
    new QueryWrapper<User>().apply("1=1")  // 不推荐
);

// 推荐：在 QueryWrapper 上显式处理
// 方式：直接写 SQL，不走 MP 的逻辑删除
@Select("SELECT * FROM user WHERE id = #{id}")  // 包含已删除的
User selectByIdIgnoreDeleted(@Param("id") Long id);
```

---

## 5. 乐观锁

**场景**：多个用户同时修改同一条记录，防止后一个修改覆盖前一个。

### 5.1 数据库添加版本字段

```sql
ALTER TABLE article ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '版本号（乐观锁）';
```

### 5.2 实体类配置

```java
@Data
@TableName("article")
public class Article {
    // ...其他字段...

    @Version  // 标记乐观锁字段
    private Integer version;
}
```

### 5.3 配置乐观锁插件

```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()); // 乐观锁
        return interceptor;
    }
}
```

### 5.4 使用乐观锁

```java
// 先查询（获取当前 version）
Article article = articleMapper.selectById(id);
// article.getVersion() = 1

// 修改（MP 自动在 WHERE 加上 version = 1，并更新为 version = 2）
article.setTitle("新标题");
int rows = articleMapper.updateById(article);
// 实际 SQL：UPDATE article SET title='新标题', version=2
//           WHERE id=? AND version=1 AND is_deleted=0

if (rows == 0) {
    // 更新失败：说明在这期间有其他请求修改了这条记录，version 已变
    throw new AppException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
}
```

---

## 6. 自动填充详解

### 6.1 完整的自动填充处理器

```java
@Slf4j
@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 严格模式：只有字段存在且值为 null 时才填充
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 状态默认值
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "status", Integer.class, 1);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时只填充 updatedAt
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 6.2 实体类注解

```java
@TableField(fill = FieldFill.INSERT)           // 只在插入时填充
private LocalDateTime createdAt;

@TableField(fill = FieldFill.INSERT_UPDATE)    // 插入和更新时都填充
private LocalDateTime updatedAt;

@TableField(fill = FieldFill.INSERT)
private Integer isDeleted;

@TableField(fill = FieldFill.INSERT)
private Integer status;
```

---

## 7. 分页进阶

### 7.1 自定义分页（XML + Page 组合）

```java
// Mapper 接口
IPage<ArticleVO> selectHotArticles(IPage<ArticleVO> page,
                                    @Param("days") int days);
```

```xml
<!-- XML 里的 count 查询 MP 自动生成，不需要额外写 COUNT SQL -->
<select id="selectHotArticles" resultType="com.example.blogapi.model.vo.ArticleVO">
    SELECT a.id, a.title, a.author_name, a.view_count, a.like_count
    FROM article a
    WHERE a.is_deleted = 0
      AND a.status = 'PUBLISHED'
      AND a.published_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
    ORDER BY (a.view_count * 0.6 + a.like_count * 0.4) DESC
</select>
```

### 7.2 游标分页（深分页优化）

```java
// 传统分页（OFFSET 很大时性能差）
Page<Article> page = new Page<>(10000, 10);  // OFFSET 99990，扫描大量行

// 游标分页（记录上一页最后一条的 ID）
@Select("""
    SELECT * FROM article
    WHERE is_deleted = 0 AND status = 'PUBLISHED'
    AND id < #{lastId}
    ORDER BY id DESC
    LIMIT #{size}
    """)
List<Article> selectByScrolling(@Param("lastId") Long lastId,
                                 @Param("size") Integer size);

// 使用：首次传 Long.MAX_VALUE，后续传上一页最后一条的 ID
List<Article> firstPage = articleMapper.selectByScrolling(Long.MAX_VALUE, 10);
Long lastId = firstPage.get(firstPage.size() - 1).getId();
List<Article> nextPage = articleMapper.selectByScrolling(lastId, 10);
```

---

## 8. 批量操作优化

### 8.1 批量插入

```java
// MP 的 saveBatch 默认是循环单条插入（性能差）
// 需要配置 rewriteBatchedStatements=true 才能真正批量
```

```yaml
# application.yml：在 URL 里加参数
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_dev?rewriteBatchedStatements=true&...
```

```java
// Service 中批量保存
List<Article> articles = buildArticles();
saveBatch(articles, 500);  // 每批 500 条提交一次
```

### 8.2 批量更新

```java
// 方式一：循环 updateById（小数量可以，大数量不推荐）
articles.forEach(a -> updateById(a));

// 方式二：update + IN（推荐，单条 SQL）
update(new LambdaUpdateWrapper<Article>()
    .in(Article::getId, ids)
    .set(Article::getStatus, "ARCHIVED")
);

// 方式三：XML 批量更新（每条记录更新不同值，用 foreach）
```

```xml
<update id="batchUpdateStatus">
    <foreach collection="list" item="item" separator=";">
        UPDATE article SET status = #{item.status} WHERE id = #{item.id}
    </foreach>
</update>
```

> 注意：MySQL 默认不支持一次执行多条语句，需要在 URL 加 `allowMultiQueries=true`。

---

## 练习

### 练习 1：XML 自定义查询

为博客系统的文章模块创建 `ArticleMapper.xml`，实现：

1. 查询文章详情（含作者昵称、头像、分类名称、标签列表）
2. 文章列表（支持按 status、categoryId、keyword 动态过滤，带分页）
3. 查询某作者的统计数据：文章总数、总浏览量、总点赞数（单条 SQL）
4. 查询最近 7 天发布的热门文章（按 view_count + like_count 综合排序）

### 练习 2：乐观锁实践

1. 给 article 表加 version 字段，配置乐观锁
2. 实现文章点赞接口：先查询当前点赞数，加 1 后用乐观锁更新，并同步更新 article_like 表
3. 模拟并发场景：用两个线程同时点赞同一篇文章，观察乐观锁的效果（其中一个会失败）

### 练习 3：批量操作

1. 实现批量发布文章接口：`POST /api/articles/batch-publish`，接收 ID 列表，批量更新 status 为 PUBLISHED
2. 实现数据初始化接口：读取一个 JSON 文件（模拟），批量插入 100 篇文章，用 saveBatch 并测试性能
3. 实现软删除恢复接口：`POST /api/articles/{id}/restore`，把 is_deleted=1 的文章恢复（需要绕过逻辑删除过滤）

### 练习 4（进阶）：完整的文章模块

综合 2-10 到 2-12 所有知识，完成文章模块的完整实现：

```
GET  /api/articles              文章列表（分页 + 过滤）
GET  /api/articles/hot          热门文章（按综合分排序）
GET  /api/articles/{id}         文章详情（含关联数据）
POST /api/articles              创建文章
PUT  /api/articles/{id}         更新文章
POST /api/articles/{id}/publish 发布文章
DELETE /api/articles/{id}       软删除
POST /api/articles/{id}/like    点赞/取消点赞（切换）
GET  /api/articles/stats        当前用户的文章统计
```

---

## 常见问题

**Q：XML 中的 `#{}` 和 `${}` 有什么区别？**  
A：`#{}` 是预编译参数（PreparedStatement），防止 SQL 注入，值会被当作字符串处理。`${}` 是字符串替换，直接拼入 SQL，有 SQL 注入风险。只在动态 SQL 中需要拼接列名、表名时才用 `${}`，其他情况一律用 `#{}`。

**Q：乐观锁更新失败后应该怎么处理？**  
A：常见策略有三种：① 直接返回错误，让用户刷新后重试；② 自动重试（捕获异常后重新查询再更新，重试 N 次后放弃）；③ 记录冲突日志，人工处理。点赞、浏览量这种非核心数据用重试，关键业务操作（如库存扣减）返回错误让用户决策。

**Q：一对多查询（文章+标签）用 collection 还是分两次查询？**  
A：数据量小时 collection 方便；分页场景推荐分两次查询：① 分页查主表数据；② 用 IN 批量查关联数据；③ Java 代码组装。这样避免了分页数量不准的问题，SQL 也更简单易维护。

**Q：`ServiceImpl` 里的 `baseMapper` 和注入的 `Mapper` 是同一个吗？**  
A：是同一个。`ServiceImpl<M, T>` 的泛型 M 就是 Mapper 类型，Spring 会自动注入。你可以用 `this.baseMapper` 访问，也可以额外 `@Autowired` 注入（效果相同，但后者多余）。

---

## 下一步

下一节 **[2-13 · RESTful API 设计原则](../2-13/README.md)** 回到 API 层，系统整理 REST 设计的最佳实践，包括版本化、幂等性、状态码规范和 API 设计中的常见坑，为博客 API 的最终完善做准备。

package org.example.aispingboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.ArticleSaveCommandDTO;
import org.example.aispingboot.DTO.command.PageQuery;
import org.example.aispingboot.DTO.response.ArticleResponseDTO;
import org.example.aispingboot.common.PageResult;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.KnowledgeArticleMapper;
import org.example.aispingboot.mapper.KnowledgeCategoryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识文章 / 分类服务
 */
@Service
public class KnowledgeService {

    /** 文章状态：草稿 */
    private static final int STATUS_DRAFT = 0;
    /** 文章状态：已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 文章状态：已下线 */
    private static final int STATUS_OFFLINE = 2;

    @Resource
    private KnowledgeCategoryMapper categoryMapper;

    @Resource
    private KnowledgeArticleMapper articleMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 分类树（仅启用状态），子分类挂在 children 字段上
     */
    public List<KnowledgeCategory> categoryTree() {
        List<KnowledgeCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategory>()
                        .eq(KnowledgeCategory::getStatus, 1)
                        .orderByAsc(KnowledgeCategory::getSortOrder)
                        .orderByAsc(KnowledgeCategory::getId));

        Map<Long, List<KnowledgeCategory>> childrenByParent = all.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() != 0L)
                .collect(Collectors.groupingBy(KnowledgeCategory::getParentId));

        for (KnowledgeCategory category : all) {
            category.setChildren(childrenByParent.getOrDefault(category.getId(), new ArrayList<>()));
        }
        return all.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0L)
                .collect(Collectors.toList());
    }

    /**
     * 文章分页
     */
    public PageResult<ArticleResponseDTO> pageArticles(PageQuery pageQuery, String title, Long categoryId,
                                                       Integer status, String sortField, String sortDirection) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(title)) {
            wrapper.like(KnowledgeArticle::getTitle, title.trim());
        }
        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(KnowledgeArticle::getStatus, status);
        }
        wrapper.orderBy(true, "asc".equalsIgnoreCase(sortDirection), resolveSortField(sortField));
        wrapper.orderByDesc(KnowledgeArticle::getId);

        Page<KnowledgeArticle> page = articleMapper.selectPage(
                new Page<>(pageQuery.resolveCurrent(), pageQuery.resolveSize()), wrapper);

        List<ArticleResponseDTO> records = page.getRecords().stream()
                .map(article -> toResponse(article, false))
                .collect(Collectors.toList());
        fillCategoryAndAuthorNames(records);

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 文章详情
     */
    public ArticleResponseDTO getArticle(String id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        ArticleResponseDTO response = toResponse(article, true);
        fillCategoryAndAuthorNames(List.of(response));
        return response;
    }

    /**
     * 新增文章，返回文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createArticle(ArticleSaveCommandDTO command, Long authorId) {
        requireCategoryExists(command.getCategoryId());

        // 前端在有上传封面时会用 crypto.randomUUID() 生成ID，没有则在这里补齐
        String articleId = StrUtil.isNotBlank(command.getId())
                ? command.getId()
                : UUID.randomUUID().toString();

        if (articleMapper.selectById(articleId) != null) {
            throw new BusinessException("文章ID已存在，请刷新页面后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(articleId)
                .categoryId(command.getCategoryId())
                .title(command.getTitle())
                .summary(command.getSummary())
                .content(command.getContent())
                .coverImage(command.getCoverImage())
                .tags(command.getTags())
                .authorId(authorId)
                .readCount(0)
                // 新建的文章先落为草稿，由管理端手动发布
                .status(STATUS_DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        articleMapper.insert(article);
        return articleId;
    }

    /**
     * 编辑文章
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(String id, ArticleSaveCommandDTO command) {
        KnowledgeArticle existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }
        requireCategoryExists(command.getCategoryId());

        KnowledgeArticle update = KnowledgeArticle.builder()
                .id(id)
                .categoryId(command.getCategoryId())
                .title(command.getTitle())
                .summary(command.getSummary())
                .content(command.getContent())
                .tags(command.getTags())
                .updatedAt(LocalDateTime.now())
                .build();
        // 封面允许被清空（前端“移除封面”会传空字符串），因此仅当字段为 null 时才保留原值
        update.setCoverImage(command.getCoverImage() != null ? command.getCoverImage() : existing.getCoverImage());

        articleMapper.updateById(update);
    }

    /**
     * 发布 / 下线文章
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeArticleStatus(String id, Integer status) {
        if (status == null || status < STATUS_DRAFT || status > STATUS_OFFLINE) {
            throw new BusinessException("文章状态不合法");
        }
        KnowledgeArticle existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }

        KnowledgeArticle update = KnowledgeArticle.builder()
                .id(id)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
        // 首次发布时记录发布时间
        if (status == STATUS_PUBLISHED && existing.getPublishedAt() == null) {
            update.setPublishedAt(LocalDateTime.now());
        }

        articleMapper.updateById(update);
    }

    /**
     * 删除文章
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(String id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        articleMapper.deleteById(id);
    }

    private void requireCategoryExists(Long categoryId) {
        if (categoryId != null && categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException("所选文章分类不存在");
        }
    }

    private SFunction<KnowledgeArticle, ?> resolveSortField(String sortField) {
        if ("readCount".equals(sortField)) {
            return KnowledgeArticle::getReadCount;
        }
        if ("publishedAt".equals(sortField)) {
            return KnowledgeArticle::getPublishedAt;
        }
        if ("createdAt".equals(sortField)) {
            return KnowledgeArticle::getCreatedAt;
        }
        return KnowledgeArticle::getUpdatedAt;
    }

    private ArticleResponseDTO toResponse(KnowledgeArticle article, boolean withContent) {
        return ArticleResponseDTO.builder()
                .id(article.getId())
                .categoryId(article.getCategoryId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(withContent ? article.getContent() : null)
                .coverImage(article.getCoverImage())
                .tags(article.getTags())
                .tagArray(StrUtil.splitTrim(article.getTags(), ','))
                .authorId(article.getAuthorId())
                .readCount(article.getReadCount())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    /**
     * 批量补齐分类名与作者名，避免逐条查询
     */
    private void fillCategoryAndAuthorNames(List<ArticleResponseDTO> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = records.stream()
                .map(ArticleResponseDTO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> authorIds = records.stream()
                .map(ArticleResponseDTO::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getCategoryName, (a, b) -> a));
        Map<Long, String> authorNames = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName, (a, b) -> a));

        for (ArticleResponseDTO record : records) {
            String categoryName = record.getCategoryId() == null ? null : categoryNames.get(record.getCategoryId());
            String authorName = record.getAuthorId() == null ? null : authorNames.get(record.getAuthorId());
            record.setCategoryName(categoryName);
            record.setAuthorName(authorName != null ? authorName : "系统");
        }
    }
}

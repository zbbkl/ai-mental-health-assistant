package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ArticleSaveCommandDTO;
import org.example.aispingboot.DTO.command.PageQuery;
import org.example.aispingboot.DTO.response.ArticleResponseDTO;
import org.example.aispingboot.common.PageResult;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.service.KnowledgeService;
import org.example.aispingboot.util.CurrentUserUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class Knowledge {

    /** 已发布状态，前台浏览只允许看到已发布文章 */
    private static final int STATUS_PUBLISHED = 1;

    @Resource
    private KnowledgeService knowledgeService;

    // 分类树
    @GetMapping("/category/tree")
    public Result<List<KnowledgeCategory>> categoryTree() {
        return Result.ok(knowledgeService.categoryTree());
    }

    // 文章分页
    @GetMapping("/article/page")
    public Result<PageResult<ArticleResponseDTO>> articlePage(
            PageQuery pageQuery,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        // 管理员可以按状态筛选（含草稿、已下线），其它访问者一律只看已发布
        // 注意：这里不能写成 `admin ? status : STATUS_PUBLISHED` 的三元表达式，
        // Integer 与 int 混用会触发拆箱，status 为 null 时直接 NPE
        Integer effectiveStatus = status;
        if (!CurrentUserUtil.currentUserIsAdmin()) {
            effectiveStatus = STATUS_PUBLISHED;
        }
        PageResult<ArticleResponseDTO> page = knowledgeService.pageArticles(
                pageQuery, title, categoryId, effectiveStatus, sortField, sortDirection);
        return Result.ok(page);
    }

    // 文章详情
    @GetMapping("/article/{id}")
    public Result<ArticleResponseDTO> articleDetail(@PathVariable String id) {
        return Result.ok(knowledgeService.getArticle(id));
    }

    // 新增文章
    @PostMapping("/article")
    public Result<String> createArticle(@Valid @RequestBody ArticleSaveCommandDTO command) {
        Long authorId = CurrentUserUtil.requireUserId();
        return Result.ok(knowledgeService.createArticle(command, authorId));
    }

    // 编辑文章
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable String id,
                                      @Valid @RequestBody ArticleSaveCommandDTO command) {
        knowledgeService.updateArticle(id, command);
        return Result.ok();
    }

    // 发布 / 下线
    @PutMapping("/article/{id}/status")
    public Result<Void> changeArticleStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        knowledgeService.changeArticleStatus(id, body.get("status"));
        return Result.ok();
    }

    // 删除文章
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable String id) {
        knowledgeService.deleteArticle(id);
        return Result.ok();
    }
}

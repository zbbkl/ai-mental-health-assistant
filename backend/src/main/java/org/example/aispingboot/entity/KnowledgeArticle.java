package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识文章
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_article")
public class KnowledgeArticle {

    /** 文章ID，由前端上传封面时生成的 UUID 或后端自动生成 */
    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("category_id")
    private Long categoryId;

    private String title;

    private String summary;

    private String content;

    @TableField("cover_image")
    private String coverImage;

    /** 标签，逗号分隔 */
    private String tags;

    @TableField("author_id")
    private Long authorId;

    @TableField("read_count")
    private Integer readCount;

    /** 状态 0:草稿 1:已发布 2:已下线 */
    private Integer status;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

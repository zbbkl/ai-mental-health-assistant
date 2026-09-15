package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文章响应对象
 * 列表接口不返回 content（列表页用不到，避免响应体过大），详情接口返回。
 */
@Data
@Builder
public class ArticleResponseDTO {
    private String id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String tags;
    /** 标签数组，详情页直接遍历渲染 */
    private List<String> tagArray;
    private Long authorId;
    private String authorName;
    private Integer readCount;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

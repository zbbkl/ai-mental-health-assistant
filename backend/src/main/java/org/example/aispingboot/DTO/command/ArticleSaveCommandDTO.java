package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识文章新增 / 编辑参数
 */
@Data
public class ArticleSaveCommandDTO {

    /** 文章ID，前端新增时可能为空（未上传封面则没有生成 UUID），由后端补齐 */
    private String id;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    @NotNull(message = "请选择文章分类")
    private Long categoryId;

    @Size(max = 1000, message = "文章摘要最多1000个字符")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String coverImage;

    /** 标签，逗号分隔 */
    @Size(max = 500, message = "标签最多500个字符")
    private String tags;
}

package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识文章状态变更参数（发布 / 下线）
 */
@Data
public class ArticleStatusCommandDTO {

    /** 0:草稿 1:已发布 2:已下线 */
    @NotNull(message = "文章状态不能为空")
    private Integer status;
}

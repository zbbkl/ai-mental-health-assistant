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
 * AI 分析任务
 * 用于跟踪情绪日记的异步分析过程（状态、重试次数、错误信息）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_analysis_task")
public class AiAnalysisTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("diary_id")
    private Long diaryId;

    @TableField("user_id")
    private Long userId;

    /** PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;

    /** AUTO-自动触发 MANUAL-手动触发 ADMIN-管理员触发 BATCH-批量触发 */
    @TableField("task_type")
    private String taskType;

    /** 优先级 1-低 2-正常 3-高 4-紧急 */
    private Integer priority;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

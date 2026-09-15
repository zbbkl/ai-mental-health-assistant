package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记响应对象
 * 管理端列表会在日记字段之外补充用户信息（username / nickname）。
 */
@Data
@Builder
public class EmotionDiaryResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String aiEmotionAnalysis;
    private LocalDateTime aiAnalysisUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

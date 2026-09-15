package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 情绪日记提交参数（用户端）
 */
@Data
public class EmotionDiaryCreateCommandDTO {

    /** 记录日期，不传时按当天处理 */
    private LocalDate diaryDate;

    @NotNull(message = "请选择情绪评分")
    @Min(value = 1, message = "情绪评分不能小于1")
    @Max(value = 10, message = "情绪评分不能大于10")
    private Integer moodScore;

    @Size(max = 50, message = "主要情绪最多50个字符")
    private String dominantEmotion;

    @Size(max = 1000, message = "情绪触发因素最多1000个字符")
    private String emotionTriggers;

    @Size(max = 2000, message = "日记内容最多2000个字符")
    private String diaryContent;

    @Min(value = 1, message = "睡眠质量不能小于1")
    @Max(value = 5, message = "睡眠质量不能大于5")
    private Integer sleepQuality;

    @Min(value = 1, message = "压力水平不能小于1")
    @Max(value = 5, message = "压力水平不能大于5")
    private Integer stressLevel;
}

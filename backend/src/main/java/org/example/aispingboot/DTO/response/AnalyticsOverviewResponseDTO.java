package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理端数据分析总览
 */
@Data
@Builder
public class AnalyticsOverviewResponseDTO {

    private SystemOverview systemOverview;

    private List<EmotionTrendItem> emotionTrend;

    private ConsultationStats consultationStats;

    private List<UserActivityItem> userActivity;

    @Data
    @Builder
    public static class SystemOverview {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalDiaries;
        private Long todayNewDiaries;
        private Long totalSessions;
        private Long todayNewSessions;
        private Double avgMoodScore;
    }

    @Data
    @Builder
    public static class EmotionTrendItem {
        private String date;
        private Double avgMoodScore;
        private long recordCount;
    }

    @Data
    @Builder
    public static class ConsultationStats {
        private Long totalSessions;
        private Double avgDurationMinutes;
        private List<DailySessionItem> dailyTrend;
    }

    @Data
    @Builder
    public static class DailySessionItem {
        private String date;
        private long sessionCount;
        private long userCount;
    }

    @Data
    @Builder
    public static class UserActivityItem {
        private String date;
        private long activeUsers;
        private long newUsers;
        private long diaryUsers;
        private long consultationUsers;
    }
}

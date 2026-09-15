package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.AnalyticsOverviewResponseDTO;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端数据分析服务
 */
@Service
public class DataAnalyticsService {

    /** 趋势窗口天数 */
    private static final int TREND_DAYS = 7;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmotionDiaryMapper diaryMapper;

    @Resource
    private ConsultationSessionMapper sessionMapper;

    @Resource
    private ConsultationMessageMapper messageMapper;

    public AnalyticsOverviewResponseDTO overview() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = resolveWindowEnd(today);
        LocalDate windowStart = windowEnd.minusDays(TREND_DAYS - 1L);

        List<EmotionDiary> windowDiaries = diaryMapper.selectList(new LambdaQueryWrapper<EmotionDiary>()
                .ge(EmotionDiary::getDiaryDate, windowStart)
                .le(EmotionDiary::getDiaryDate, windowEnd));
        List<ConsultationSession> windowSessions = sessionMapper.selectList(new LambdaQueryWrapper<ConsultationSession>()
                .ge(ConsultationSession::getStartedAt, windowStart.atStartOfDay())
                .lt(ConsultationSession::getStartedAt, windowEnd.plusDays(1).atStartOfDay()));
        List<User> windowNewUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, windowStart.atStartOfDay())
                .lt(User::getCreatedAt, windowEnd.plusDays(1).atStartOfDay()));

        Map<LocalDate, List<EmotionDiary>> diariesByDate = windowDiaries.stream()
                .filter(diary -> diary.getDiaryDate() != null)
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate));
        Map<LocalDate, List<ConsultationSession>> sessionsByDate = windowSessions.stream()
                .filter(session -> session.getStartedAt() != null)
                .collect(Collectors.groupingBy(session -> session.getStartedAt().toLocalDate()));

        List<LocalDate> dates = windowStart.datesUntil(windowEnd.plusDays(1)).collect(Collectors.toList());

        return AnalyticsOverviewResponseDTO.builder()
                .systemOverview(buildSystemOverview(today, windowDiaries, windowSessions))
                .emotionTrend(buildEmotionTrend(dates, diariesByDate))
                .consultationStats(buildConsultationStats(windowSessions, sessionsByDate, dates))
                .userActivity(buildUserActivity(dates, diariesByDate, sessionsByDate, windowNewUsers))
                .build();
    }

    private AnalyticsOverviewResponseDTO.SystemOverview buildSystemOverview(LocalDate today,
                                                                           List<EmotionDiary> windowDiaries,
                                                                           List<ConsultationSession> windowSessions) {
        Set<Long> activeUserIds = new HashSet<>();
        windowDiaries.stream().map(EmotionDiary::getUserId).filter(Objects::nonNull).forEach(activeUserIds::add);
        windowSessions.stream().map(ConsultationSession::getUserId).filter(Objects::nonNull).forEach(activeUserIds::add);

        List<Integer> moodScores = diaryMapper.selectList(new LambdaQueryWrapper<EmotionDiary>()
                        .select(EmotionDiary::getMoodScore))
                .stream()
                .map(EmotionDiary::getMoodScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LocalDateTime todayStart = today.atStartOfDay();
        return AnalyticsOverviewResponseDTO.SystemOverview.builder()
                .totalUsers(userMapper.selectCount(null))
                .activeUsers((long) activeUserIds.size())
                .totalDiaries(diaryMapper.selectCount(null))
                .todayNewDiaries(diaryMapper.selectCount(new LambdaQueryWrapper<EmotionDiary>()
                        .ge(EmotionDiary::getCreatedAt, todayStart)))
                .totalSessions(sessionMapper.selectCount(null))
                .todayNewSessions(sessionMapper.selectCount(new LambdaQueryWrapper<ConsultationSession>()
                        .ge(ConsultationSession::getStartedAt, todayStart)))
                .avgMoodScore(averageScore(moodScores))
                .build();
    }

    private List<AnalyticsOverviewResponseDTO.EmotionTrendItem> buildEmotionTrend(
            List<LocalDate> dates, Map<LocalDate, List<EmotionDiary>> diariesByDate) {
        List<AnalyticsOverviewResponseDTO.EmotionTrendItem> trend = new ArrayList<>();
        for (LocalDate date : dates) {
            List<EmotionDiary> dayDiaries = diariesByDate.getOrDefault(date, List.of());
            List<Integer> scores = dayDiaries.stream()
                    .map(EmotionDiary::getMoodScore)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            trend.add(AnalyticsOverviewResponseDTO.EmotionTrendItem.builder()
                    .date(date.toString())
                    // 当天没有记录时返回 null，折线图会自动断开而不是掉到 0
                    .avgMoodScore(scores.isEmpty() ? null : averageScore(scores))
                    .recordCount(dayDiaries.size())
                    .build());
        }
        return trend;
    }

    private AnalyticsOverviewResponseDTO.ConsultationStats buildConsultationStats(
            List<ConsultationSession> windowSessions,
            Map<LocalDate, List<ConsultationSession>> sessionsByDate,
            List<LocalDate> dates) {
        List<AnalyticsOverviewResponseDTO.DailySessionItem> dailyTrend = new ArrayList<>();
        for (LocalDate date : dates) {
            List<ConsultationSession> daySessions = sessionsByDate.getOrDefault(date, List.of());
            long userCount = daySessions.stream()
                    .map(ConsultationSession::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            dailyTrend.add(AnalyticsOverviewResponseDTO.DailySessionItem.builder()
                    .date(date.toString())
                    .sessionCount(daySessions.size())
                    .userCount(userCount)
                    .build());
        }
        return AnalyticsOverviewResponseDTO.ConsultationStats.builder()
                .totalSessions(sessionMapper.selectCount(null))
                .avgDurationMinutes(calcAvgDurationMinutes(windowSessions))
                .dailyTrend(dailyTrend)
                .build();
    }

    /**
     * 平均会话时长：会话表没有时长字段，用会话内首尾消息时间差近似
     */
    private Double calcAvgDurationMinutes(List<ConsultationSession> sessions) {
        List<Long> sessionIds = sessions.stream()
                .map(ConsultationSession::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (sessionIds.isEmpty()) {
            return 0.0;
        }

        List<ConsultationMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ConsultationMessage>()
                .select(ConsultationMessage::getSessionId, ConsultationMessage::getCreatedAt)
                .in(ConsultationMessage::getSessionId, sessionIds));

        Map<Long, List<ConsultationMessage>> messagesBySession = messages.stream()
                .filter(message -> message.getSessionId() != null && message.getCreatedAt() != null)
                .collect(Collectors.groupingBy(ConsultationMessage::getSessionId));

        double totalMinutes = 0;
        int countedSessions = 0;
        for (List<ConsultationMessage> group : messagesBySession.values()) {
            LocalDateTime first = group.stream().map(ConsultationMessage::getCreatedAt)
                    .min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime last = group.stream().map(ConsultationMessage::getCreatedAt)
                    .max(LocalDateTime::compareTo).orElse(null);
            if (first != null && last != null && last.isAfter(first)) {
                totalMinutes += Duration.between(first, last).toSeconds() / 60.0;
                countedSessions++;
            }
        }
        return countedSessions == 0 ? 0.0 : round1(totalMinutes / countedSessions);
    }

    private List<AnalyticsOverviewResponseDTO.UserActivityItem> buildUserActivity(
            List<LocalDate> dates,
            Map<LocalDate, List<EmotionDiary>> diariesByDate,
            Map<LocalDate, List<ConsultationSession>> sessionsByDate,
            List<User> windowNewUsers) {
        Map<LocalDate, Long> newUsersByDate = windowNewUsers.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(user -> user.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<AnalyticsOverviewResponseDTO.UserActivityItem> activity = new ArrayList<>();
        for (LocalDate date : dates) {
            Set<Long> diaryUsers = diariesByDate.getOrDefault(date, List.of()).stream()
                    .map(EmotionDiary::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Long> consultationUsers = sessionsByDate.getOrDefault(date, List.of()).stream()
                    .map(ConsultationSession::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Long> activeUsers = new HashSet<>(diaryUsers);
            activeUsers.addAll(consultationUsers);

            activity.add(AnalyticsOverviewResponseDTO.UserActivityItem.builder()
                    .date(date.toString())
                    .activeUsers(activeUsers.size())
                    .newUsers(newUsersByDate.getOrDefault(date, 0L))
                    .diaryUsers(diaryUsers.size())
                    .consultationUsers(consultationUsers.size())
                    .build());
        }
        return activity;
    }

    /**
     * 统计窗口默认截止到今天；若最近 7 天内完全没有数据（例如库中是历史演示数据），
     * 则把窗口锚定到最近一次数据日期，避免趋势图全空。
     */
    private LocalDate resolveWindowEnd(LocalDate today) {
        LocalDate defaultStart = today.minusDays(TREND_DAYS - 1L);
        boolean hasRecentDiary = diaryMapper.selectCount(new LambdaQueryWrapper<EmotionDiary>()
                .ge(EmotionDiary::getDiaryDate, defaultStart)
                .le(EmotionDiary::getDiaryDate, today)) > 0;
        boolean hasRecentSession = sessionMapper.selectCount(new LambdaQueryWrapper<ConsultationSession>()
                .ge(ConsultationSession::getStartedAt, defaultStart.atStartOfDay())
                .lt(ConsultationSession::getStartedAt, today.plusDays(1).atStartOfDay())) > 0;
        if (hasRecentDiary || hasRecentSession) {
            return today;
        }

        LocalDate latest = latestActivityDate();
        if (latest == null || !latest.isBefore(today)) {
            return today;
        }
        return latest;
    }

    private LocalDate latestActivityDate() {
        EmotionDiary latestDiary = diaryMapper.selectOne(new LambdaQueryWrapper<EmotionDiary>()
                .select(EmotionDiary::getDiaryDate)
                .orderByDesc(EmotionDiary::getDiaryDate)
                .last("limit 1"));
        ConsultationSession latestSession = sessionMapper.selectOne(new LambdaQueryWrapper<ConsultationSession>()
                .select(ConsultationSession::getStartedAt)
                .orderByDesc(ConsultationSession::getStartedAt)
                .last("limit 1"));

        LocalDate diaryDate = latestDiary != null ? latestDiary.getDiaryDate() : null;
        LocalDate sessionDate = latestSession != null && latestSession.getStartedAt() != null
                ? latestSession.getStartedAt().toLocalDate()
                : null;
        if (diaryDate == null) {
            return sessionDate;
        }
        if (sessionDate == null) {
            return diaryDate;
        }
        return diaryDate.isAfter(sessionDate) ? diaryDate : sessionDate;
    }

    private Double averageScore(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return round1(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

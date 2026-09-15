package org.example.aispingboot.service;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.AiService.EmotionAnalysisService;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateCommandDTO;
import org.example.aispingboot.DTO.command.PageQuery;
import org.example.aispingboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispingboot.common.PageResult;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 情绪日记服务
 */
@Service
public class EmotionDiaryService {

    @Resource
    private EmotionDiaryMapper diaryMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmotionAnalysisService emotionAnalysisService;

    /**
     * 保存用户当天情绪日记
     * 表上存在 (user_id, diary_date) 唯一索引，同一天重复提交按更新处理。
     */
    @Transactional(rollbackFor = Exception.class)
    public EmotionDiaryResponseDTO saveDiary(Long userId, EmotionDiaryCreateCommandDTO command) {
        LocalDate diaryDate = command.getDiaryDate() != null ? command.getDiaryDate() : LocalDate.now();
        if (diaryDate.isAfter(LocalDate.now())) {
            throw new BusinessException("不能提交未来日期的情绪日记");
        }

        EmotionDiary existing = diaryMapper.selectOne(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, diaryDate));

        if (existing == null) {
            LocalDateTime now = LocalDateTime.now();
            EmotionDiary diary = EmotionDiary.builder()
                    .userId(userId)
                    .diaryDate(diaryDate)
                    .moodScore(command.getMoodScore())
                    .dominantEmotion(command.getDominantEmotion())
                    .emotionTriggers(command.getEmotionTriggers())
                    .diaryContent(command.getDiaryContent())
                    .sleepQuality(command.getSleepQuality())
                    .stressLevel(command.getStressLevel())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            diaryMapper.insert(diary);
            triggerAnalysisAfterCommit(diary.getId());
            return toResponse(diary, null);
        }

        EmotionDiary update = EmotionDiary.builder()
                .id(existing.getId())
                .moodScore(command.getMoodScore())
                .dominantEmotion(command.getDominantEmotion())
                .emotionTriggers(command.getEmotionTriggers())
                .diaryContent(command.getDiaryContent())
                .sleepQuality(command.getSleepQuality())
                .stressLevel(command.getStressLevel())
                .updatedAt(LocalDateTime.now())
                .build();
        diaryMapper.updateById(update);

        // 内容变了，重新分析（旧的分析结果会被覆盖）
        triggerAnalysisAfterCommit(existing.getId());
        return toResponse(diaryMapper.selectById(existing.getId()), null);
    }

    /**
     * 在事务提交后再触发异步分析
     * 否则异步线程可能先于事务提交去查这条日记，读到旧数据或查不到。
     */
    private void triggerAnalysisAfterCommit(Long diaryId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emotionAnalysisService.analyzeDiaryAsync(diaryId);
                }
            });
        } else {
            emotionAnalysisService.analyzeDiaryAsync(diaryId);
        }
    }

    /**
     * 管理端情绪日志分页
     */
    public PageResult<EmotionDiaryResponseDTO> pageDiaries(PageQuery pageQuery, Long userId, String moodScoreRange) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(EmotionDiary::getUserId, userId);
        }
        applyMoodScoreRange(wrapper, moodScoreRange);
        wrapper.orderByDesc(EmotionDiary::getDiaryDate).orderByDesc(EmotionDiary::getId);

        Page<EmotionDiary> page = diaryMapper.selectPage(
                new Page<>(pageQuery.resolveCurrent(), pageQuery.resolveSize()), wrapper);

        List<EmotionDiaryResponseDTO> records = page.getRecords().stream()
                .map(diary -> toResponse(diary, null))
                .collect(Collectors.toList());
        fillUserInfo(records);

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 管理端删除情绪日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiary(Long id) {
        if (diaryMapper.selectById(id) == null) {
            throw new BusinessException("情绪日志不存在");
        }
        diaryMapper.deleteById(id);
    }

    /**
     * 前端筛选值是 "1-3" / "4-6" / "7-10" 这类区间字符串
     */
    private void applyMoodScoreRange(LambdaQueryWrapper<EmotionDiary> wrapper, String moodScoreRange) {
        if (StrUtil.isBlank(moodScoreRange)) {
            return;
        }
        List<String> parts = StrUtil.split(moodScoreRange, '-');
        if (parts.size() != 2) {
            return;
        }
        try {
            int min = NumberUtil.parseInt(parts.get(0).trim());
            int max = NumberUtil.parseInt(parts.get(1).trim());
            wrapper.between(EmotionDiary::getMoodScore, min, max);
        } catch (NumberFormatException ignored) {
            // 区间格式非法时忽略该筛选条件，不影响其它条件生效
        }
    }

    private void fillUserInfo(List<EmotionDiaryResponseDTO> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(EmotionDiaryResponseDTO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        for (EmotionDiaryResponseDTO record : records) {
            User user = record.getUserId() == null ? null : users.get(record.getUserId());
            if (user != null) {
                record.setUsername(user.getUsername());
                record.setNickname(user.getNickname());
            }
        }
    }

    private EmotionDiaryResponseDTO toResponse(EmotionDiary diary, User user) {
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .username(user != null ? user.getUsername() : null)
                .nickname(user != null ? user.getNickname() : null)
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(diary.getEmotionTriggers())
                .diaryContent(diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .aiEmotionAnalysis(diary.getAiEmotionAnalysis())
                .aiAnalysisUpdatedAt(diary.getAiAnalysisUpdatedAt())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}

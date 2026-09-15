package org.example.aispingboot.AiService;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.aispingboot.entity.AiAnalysisTask;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.AiAnalysisTaskMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 情绪日记 AI 分析服务
 * 提交日记后异步调用大模型生成情绪分析结果，写入 emotion_diary.ai_emotion_analysis，
 * 并在 ai_analysis_task 中记录任务的执行状态。
 *
 * 设计前提：分析失败不能影响用户提交日记，因此全部异常都在本服务内消化。
 */
@Slf4j
@Service
public class EmotionAnalysisService {

    private static final String TASK_TYPE_AUTO = "AUTO";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Resource
    private EmotionDiaryMapper diaryMapper;

    @Resource
    private AiAnalysisTaskMapper taskMapper;

    /**
     * 异步分析指定情绪日记
     */
    @Async("emotionAnalysisExecutor")
    public void analyzeDiaryAsync(Long diaryId) {
        AiAnalysisTask task = null;
        try {
            EmotionDiary diary = diaryMapper.selectById(diaryId);
            if (diary == null) {
                log.warn("情绪日记不存在，跳过 AI 分析: diaryId={}", diaryId);
                return;
            }

            task = createTask(diary);
            markProcessing(task);

            String analysisJson = requestAnalysis(diary);
            diaryMapper.updateById(EmotionDiary.builder()
                    .id(diaryId)
                    .aiEmotionAnalysis(analysisJson)
                    .aiAnalysisUpdatedAt(LocalDateTime.now())
                    .build());
            markCompleted(task);
            log.info("情绪日记 AI 分析完成: diaryId={}, taskId={}", diaryId, task.getId());
        } catch (Exception e) {
            log.error("情绪日记 AI 分析失败: diaryId={}（日记内容不受影响，可在管理端重试）", diaryId, e);
            markFailed(task, e);
        }
    }

    private String requestAnalysis(EmotionDiary diary) {
        String content = chatClient.prompt()
                .system(PromptManage.EMOTION_ANALYSIS_SYSTEM_PROMPT)
                .user(buildUserContent(diary))
                .call()
                .content();

        if (StrUtil.isBlank(content)) {
            throw new BusinessException("AI 未返回分析结果");
        }
        return normalize(extractJson(content)).toString();
    }

    private String buildUserContent(EmotionDiary diary) {
        StringBuilder content = new StringBuilder("请分析下面这条情绪日记，并按要求的 JSON 格式返回：\n");
        content.append("日期：").append(diary.getDiaryDate()).append('\n');
        content.append("情绪评分（1-10）：").append(diary.getMoodScore()).append('\n');
        if (StrUtil.isNotBlank(diary.getDominantEmotion())) {
            content.append("用户自评主要情绪：").append(diary.getDominantEmotion()).append('\n');
        }
        if (diary.getSleepQuality() != null) {
            content.append("睡眠质量（1-5）：").append(diary.getSleepQuality()).append('\n');
        }
        if (diary.getStressLevel() != null) {
            content.append("压力水平（1-5）：").append(diary.getStressLevel()).append('\n');
        }
        if (StrUtil.isNotBlank(diary.getEmotionTriggers())) {
            content.append("情绪触发因素：").append(diary.getEmotionTriggers()).append('\n');
        }
        if (StrUtil.isNotBlank(diary.getDiaryContent())) {
            content.append("日记内容：").append(diary.getDiaryContent()).append('\n');
        }
        return content.toString();
    }

    /**
     * 模型有时会把 JSON 包在 ```json 代码块或说明文字里，这里截取最外层对象
     */
    private JSONObject extractJson(String rawContent) {
        int start = rawContent.indexOf('{');
        int end = rawContent.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("AI 返回内容中未找到 JSON: " + StrUtil.maxLength(rawContent, 200));
        }
        try {
            return JSONUtil.parseObj(rawContent.substring(start, end + 1));
        } catch (Exception e) {
            throw new BusinessException("AI 返回的 JSON 无法解析: " + e.getMessage());
        }
    }

    /**
     * 补齐缺失字段并做范围约束，保证前端渲染不会因为字段缺失而报错
     */
    private JSONObject normalize(JSONObject raw) {
        JSONObject result = new JSONObject();
        result.set("primaryEmotion", StrUtil.blankToDefault(raw.getStr("primaryEmotion"), "中性"));
        result.set("emotionScore", clamp(Convert.toInt(raw.get("emotionScore"), 50), 0, 100));
        result.set("isNegative", Convert.toBool(raw.get("isNegative"), false));
        result.set("riskLevel", clamp(Convert.toInt(raw.get("riskLevel"), 0), 0, 3));
        result.set("keywords", toStringArray(raw.get("keywords")));
        result.set("suggestion", StrUtil.blankToDefault(raw.getStr("suggestion"), "多留意自己的情绪变化，适当放松"));
        result.set("riskDescription", StrUtil.blankToDefault(raw.getStr("riskDescription"), "情绪稳定"));
        result.set("improvementSuggestions", toStringArray(raw.get("improvementSuggestions")));
        result.set("timestamp", System.currentTimeMillis());
        return result;
    }

    private JSONArray toStringArray(Object value) {
        JSONArray array = new JSONArray();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StrUtil.isNotBlank(item.toString())) {
                    array.add(item.toString().trim());
                }
            }
        } else if (value != null && StrUtil.isNotBlank(value.toString())) {
            array.add(value.toString().trim());
        }
        return array;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private AiAnalysisTask createTask(EmotionDiary diary) {
        LocalDateTime now = LocalDateTime.now();
        AiAnalysisTask task = AiAnalysisTask.builder()
                .diaryId(diary.getId())
                .userId(diary.getUserId())
                .status(STATUS_PENDING)
                .taskType(TASK_TYPE_AUTO)
                .priority(2)
                .retryCount(0)
                .maxRetryCount(3)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskMapper.insert(task);
        return task;
    }

    private void markProcessing(AiAnalysisTask task) {
        taskMapper.updateById(AiAnalysisTask.builder()
                .id(task.getId())
                .status(STATUS_PROCESSING)
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void markCompleted(AiAnalysisTask task) {
        taskMapper.updateById(AiAnalysisTask.builder()
                .id(task.getId())
                .status(STATUS_COMPLETED)
                .completedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void markFailed(AiAnalysisTask task, Exception e) {
        if (task == null) {
            return;
        }
        try {
            taskMapper.updateById(AiAnalysisTask.builder()
                    .id(task.getId())
                    .status(STATUS_FAILED)
                    .errorMessage(StrUtil.maxLength(e.getMessage(), 500))
                    .completedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        } catch (Exception updateError) {
            log.warn("更新 AI 分析任务状态失败: taskId={}", task.getId(), updateError);
        }
    }
}

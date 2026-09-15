package org.example.aispingboot.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.PageQuery;
import org.example.aispingboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispingboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispingboot.common.PageResult;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 咨询会话查询服务（会话列表 / 消息记录 / 删除 / 情绪分析结果）
 */
@Service
public class ConsultationQueryService {

    /** 会话还没有情绪分析结果时返回的默认值，字段与前端“情绪花园”保持一致 */
    private static final Map<String, Object> DEFAULT_EMOTION = buildDefaultEmotion();

    @Resource
    private ConsultationSessionMapper sessionMapper;

    @Resource
    private ConsultationMessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 会话分页：管理员看全部，普通用户只看自己的
     */
    public PageResult<ConsultationSessionResponseDTO> pageSessions(PageQuery pageQuery, Long currentUserId, boolean admin) {
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        if (!admin) {
            wrapper.eq(ConsultationSession::getUserId, currentUserId);
        }
        wrapper.orderByDesc(ConsultationSession::getStartedAt).orderByDesc(ConsultationSession::getId);

        Page<ConsultationSession> page = sessionMapper.selectPage(
                new Page<>(pageQuery.resolveCurrent(), pageQuery.resolveSize()), wrapper);

        return PageResult.of(toSessionResponses(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 会话消息记录（按时间正序）
     */
    public List<ConsultationMessageResponseDTO> listMessages(Long sessionId, Long currentUserId, boolean admin) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        checkAccessible(session, currentUserId, admin, "会话不存在");

        List<ConsultationMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt)
                .orderByAsc(ConsultationMessage::getId));

        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    /**
     * 删除会话，消息表外键为 ON DELETE CASCADE，会一并清理
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long currentUserId, boolean admin) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        checkAccessible(session, currentUserId, admin, "会话不存在");
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 会话最近一次情绪分析结果
     * 前端传入的 sessionId 可能带 "session_" 前缀，这里统一做兼容。
     */
    public Object getSessionEmotion(String rawSessionId, Long currentUserId, boolean admin) {
        Long sessionId = parseSessionId(rawSessionId);
        ConsultationSession session = sessionMapper.selectById(sessionId);
        checkAccessible(session, currentUserId, admin, "会话不存在");

        if (StrUtil.isNotBlank(session.getLastEmotionAnalysis())) {
            try {
                return JSONUtil.parseObj(session.getLastEmotionAnalysis());
            } catch (Exception ignored) {
                // 历史数据格式异常时退回默认值，不影响页面展示
            }
        }
        return new LinkedHashMap<>(DEFAULT_EMOTION);
    }

    private Long parseSessionId(String rawSessionId) {
        if (StrUtil.isBlank(rawSessionId)) {
            throw new BusinessException("会话ID不能为空");
        }
        String idPart = rawSessionId.trim();
        if (idPart.startsWith("session_")) {
            idPart = idPart.substring("session_".length());
        }
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new BusinessException("会话ID格式错误");
        }
    }

    private void checkAccessible(ConsultationSession session, Long currentUserId, boolean admin, String notFoundMessage) {
        if (session == null) {
            throw new BusinessException(notFoundMessage);
        }
        if (!admin && !Objects.equals(session.getUserId(), currentUserId)) {
            throw new BusinessException("无权访问该会话");
        }
    }

    private List<ConsultationSessionResponseDTO> toSessionResponses(List<ConsultationSession> sessions) {
        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> sessionIds = sessions.stream()
                .map(ConsultationSession::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<ConsultationMessage>> messagesBySession = sessionIds.isEmpty() ? Map.of()
                : messageMapper.selectList(new LambdaQueryWrapper<ConsultationMessage>()
                        .select(ConsultationMessage::getSessionId, ConsultationMessage::getContent, ConsultationMessage::getCreatedAt)
                        .in(ConsultationMessage::getSessionId, sessionIds)
                        .orderByAsc(ConsultationMessage::getCreatedAt)
                        .orderByAsc(ConsultationMessage::getId))
                .stream()
                .filter(message -> message.getSessionId() != null)
                .collect(Collectors.groupingBy(ConsultationMessage::getSessionId));

        Set<Long> userIds = sessions.stream()
                .map(ConsultationSession::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        List<ConsultationSessionResponseDTO> result = new ArrayList<>();
        for (ConsultationSession session : sessions) {
            List<ConsultationMessage> messages = messagesBySession.getOrDefault(session.getId(), List.of());
            ConsultationMessage lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
            User user = session.getUserId() == null ? null : users.get(session.getUserId());

            result.add(ConsultationSessionResponseDTO.builder()
                    .id(session.getId())
                    .userId(session.getUserId())
                    .userNickname(user != null ? user.getDisplayName() : null)
                    .sessionTitle(session.getSessionTitle())
                    .startedAt(session.getStartedAt())
                    .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
                    .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                    .messageCount(messages.size())
                    .durationMinutes(calcDurationMinutes(session.getStartedAt(),
                            lastMessage != null ? lastMessage.getCreatedAt() : null))
                    .build());
        }
        return result;
    }

    private Integer calcDurationMinutes(java.time.LocalDateTime startedAt, java.time.LocalDateTime lastMessageAt) {
        if (startedAt == null || lastMessageAt == null || !lastMessageAt.isAfter(startedAt)) {
            return 0;
        }
        return (int) Duration.between(startedAt, lastMessageAt).toMinutes();
    }

    private ConsultationMessageResponseDTO toMessageResponse(ConsultationMessage message) {
        ConsultationMessageResponseDTO response = new ConsultationMessageResponseDTO();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSenderType(message.getSenderType());
        response.setSenderTypeDesc(message.getSenderTypeDesc());
        response.setMessageType(message.getMessageType());
        response.setMessageTypeDesc(message.getMessageTypeDesc());
        response.setContent(message.getContent());
        response.setEmotionTag(message.getEmotionTag());
        response.setAiModel(message.getAiModel());
        response.setCreatedAt(message.getCreatedAt());
        response.calculateContentLength();
        return response;
    }

    private static Map<String, Object> buildDefaultEmotion() {
        Map<String, Object> emotion = new LinkedHashMap<>();
        emotion.put("primaryEmotion", "中性");
        emotion.put("emotionScore", 50);
        emotion.put("isNegative", false);
        emotion.put("riskLevel", 0);
        emotion.put("label", "平静");
        emotion.put("icon", "😐");
        emotion.put("suggestion", "情绪状态平稳，慢慢来就好");
        emotion.put("riskDescription", "当前情绪状态稳定，无需特别关注");
        emotion.put("improvementSuggestions", List.of("保持规律作息", "适当运动", "与朋友交流"));
        return emotion;
    }
}

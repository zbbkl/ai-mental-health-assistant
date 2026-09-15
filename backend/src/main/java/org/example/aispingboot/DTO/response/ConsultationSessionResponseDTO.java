package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询会话列表项
 * 同时满足用户端会话侧边栏和管理端咨询记录表格的字段需求。
 */
@Data
@Builder
public class ConsultationSessionResponseDTO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String sessionTitle;
    private LocalDateTime startedAt;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer messageCount;
    private Integer durationMinutes;
}

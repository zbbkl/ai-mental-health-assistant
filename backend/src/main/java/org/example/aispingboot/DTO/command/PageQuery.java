package org.example.aispingboot.DTO.command;

import lombok.Data;

/**
 * 分页查询参数
 * 前端各页面的分页字段命名并不统一（管理员页用 currentPage/size，
 * 情绪日志页用 current/size，用户端会话列表用 pageNum/pageSize），
 * 这里把所有别名都接收下来，再统一解析。
 */
@Data
public class PageQuery {
    private Integer currentPage;
    private Integer pageNum;
    private Integer current;
    private Integer size;
    private Integer pageSize;

    public long resolveCurrent() {
        Integer value = currentPage != null ? currentPage : (pageNum != null ? pageNum : current);
        return value == null || value < 1 ? 1L : value;
    }

    public long resolveSize() {
        Integer value = size != null ? size : pageSize;
        if (value == null || value < 1) {
            return 10L;
        }
        // 防止前端传入过大的分页尺寸
        return Math.min(value, 100L);
    }
}

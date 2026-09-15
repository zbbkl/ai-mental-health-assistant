package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传响应对象
 * 前端通过 filePath 拼接文件服务地址后直接作为图片地址使用。
 */
@Data
@Builder
public class FileUploadResponseDTO {
    private Long id;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String businessType;
    private String businessId;
    private String businessField;
}

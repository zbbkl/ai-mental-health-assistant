package org.example.aispingboot.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.FileUploadResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.SysFileInfo;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.SysFileInfoMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 文件存储服务
 * 文件落盘到 app.upload.root 下，数据库记录 /files/... 相对访问路径，
 * 由 WebMvcConfig 把 /files/** 映射到该目录。
 */
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "txt", "zip");

    /** 扩展名 -> 文件类型，对应 sys_file_info.file_type 的取值 */
    private static final Map<String, String> FILE_TYPES = Map.ofEntries(
            Map.entry("jpg", "IMG"),
            Map.entry("jpeg", "IMG"),
            Map.entry("png", "IMG"),
            Map.entry("gif", "IMG"),
            Map.entry("bmp", "IMG"),
            Map.entry("webp", "IMG"),
            Map.entry("pdf", "PDF"),
            Map.entry("doc", "DOC"),
            Map.entry("docx", "DOC"),
            Map.entry("xls", "XLS"),
            Map.entry("xlsx", "XLS"),
            Map.entry("txt", "TXT"),
            Map.entry("zip", "ZIP"));

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Resource
    private SysFileInfoMapper fileInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponseDTO upload(MultipartFile file, String businessType, String businessId,
                                       String businessField, Long uploadUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED.getMsg());
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED.getMsg());
        }

        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "unnamed");
        String extension = StrUtil.subAfter(originalName, '.', true).toLowerCase();
        if (StrUtil.isBlank(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED.getMsg());
        }

        // 未指定业务类型时按临时文件存放在 /files/temp/ 下
        boolean temp = StrUtil.isBlank(businessType);
        String directory = temp ? "files/temp/" : "files/bussiness/" + sanitize(businessType) + "/";
        String fileName = System.currentTimeMillis() + RandomUtil.randomString(6) + "." + extension;

        String relativePath = directory + fileName;
        Path baseDir = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path target = baseDir.resolve(relativePath).normalize();
        if (!target.startsWith(baseDir)) {
            // 业务类型里带 ../ 之类的路径穿越字符时直接拒绝
            throw new BusinessException(ResultCode.FILE_NAME_INVALID.getMsg());
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED.getMsg());
        }

        SysFileInfo fileInfo = SysFileInfo.builder()
                .originalName(originalName)
                .filePath("/" + relativePath)
                .fileSize(file.getSize())
                .fileType(FILE_TYPES.getOrDefault(extension, "OTHER"))
                .businessType(StrUtil.blankToDefault(businessType, null))
                .businessId(StrUtil.blankToDefault(businessId, null))
                .businessField(StrUtil.blankToDefault(businessField, null))
                .uploadUserId(uploadUserId)
                .isTemp(temp ? 1 : 0)
                .status(1)
                .createTime(LocalDateTime.now())
                .expireTime(temp ? LocalDateTime.now().plusDays(7) : null)
                .build();
        fileInfoMapper.insert(fileInfo);

        return FileUploadResponseDTO.builder()
                .id(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .filePath(fileInfo.getFilePath())
                .fileSize(fileInfo.getFileSize())
                .fileType(fileInfo.getFileType())
                .businessType(fileInfo.getBusinessType())
                .businessId(fileInfo.getBusinessId())
                .businessField(fileInfo.getBusinessField())
                .build();
    }

    /** 业务类型只允许字母、数字、下划线和短横线，避免拼出非法目录名 */
    private String sanitize(String businessType) {
        String cleaned = businessType.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (StrUtil.isBlank(cleaned)) {
            throw new BusinessException(ResultCode.FILE_NAME_INVALID.getMsg());
        }
        return cleaned;
    }
}

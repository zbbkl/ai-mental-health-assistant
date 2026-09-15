package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.FileUploadResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.FileStorageService;
import org.example.aispingboot.util.CurrentUserUtil;
import org.example.aispingboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileUpload {

    @Resource
    private FileStorageService fileStorageService;

    // 通用文件上传（文章封面、头像等）
    @PostMapping("/upload")
    public Result<FileUploadResponseDTO> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String businessType,
                                              @RequestParam(required = false) String businessId,
                                              @RequestParam(required = false) String businessField) {
        JwtTokenUtil.TokenVerificationResult currentUser = CurrentUserUtil.currentUserOrNull();
        Long uploadUserId = currentUser != null ? currentUser.getUserId() : null;
        FileUploadResponseDTO result = fileStorageService.upload(file, businessType, businessId, businessField, uploadUserId);
        return Result.ok(result);
    }
}

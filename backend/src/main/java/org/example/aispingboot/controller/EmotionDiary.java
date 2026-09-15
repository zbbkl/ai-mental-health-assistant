package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateCommandDTO;
import org.example.aispingboot.DTO.command.PageQuery;
import org.example.aispingboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispingboot.common.PageResult;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.EmotionDiaryService;
import org.example.aispingboot.util.CurrentUserUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiary {

    @Resource
    private EmotionDiaryService emotionDiaryService;

    // 用户端提交情绪日记
    @PostMapping
    public Result<EmotionDiaryResponseDTO> saveDiary(@Valid @RequestBody EmotionDiaryCreateCommandDTO command) {
        Long userId = CurrentUserUtil.requireUserId();
        return Result.ok(emotionDiaryService.saveDiary(userId, command));
    }

    // 管理端情绪日志分页
    // 注意：评分区间参数名沿用前端 formItem 的 moodScreRange（前端拼写如此）
    @GetMapping("/admin/page")
    public Result<PageResult<EmotionDiaryResponseDTO>> adminPage(
            PageQuery pageQuery,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String moodScreRange) {
        return Result.ok(emotionDiaryService.pageDiaries(pageQuery, userId, moodScreRange));
    }

    // 管理端删除情绪日志
    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteDiary(@PathVariable Long id) {
        emotionDiaryService.deleteDiary(id);
        return Result.ok();
    }
}

package org.example.aispingboot.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查仍是占位/默认的敏感配置并给出告警
 * 这些值不设置应用也能启动，但相关功能会静默失效或存在安全风险，提前提示比事后排查省事。
 */
@Slf4j
@Component
public class InsecureDefaultsWarner implements ApplicationRunner {

    private static final String PLACEHOLDER_AI_KEY = "you-key";
    private static final String DEFAULT_JWT_SECRET = "MySecretKeyForJWT2025!@#$%^&*()_+SecureKeyHere";

    @Value("${spring.ai.openai.api-key:}")
    private String aiApiKey;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (StrUtil.isBlank(aiApiKey) || PLACEHOLDER_AI_KEY.equals(aiApiKey)) {
            log.warn("检测到 AI API Key 仍是占位值，AI 对话与情绪日记分析将不可用；"
                    + "请在 application.yml 中把 spring.ai.openai.api-key 换成真实 Key 后重启"
                    + "（申请地址 https://siliconflow.cn）");
        }
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            log.warn("检测到正在使用配置文件里的默认 JWT 密钥；"
                    + "该密钥可被用于伪造任意用户身份，生产环境请通过环境变量 JWT_SECRET 覆盖");
        }
    }
}

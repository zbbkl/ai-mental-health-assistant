package org.example.aispingboot.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * 统一 JSON 时间格式
 * 默认的 ISO 格式（2025-09-01T10:00:00）直接渲染在管理端表格里可读性较差，
 * 这里统一输出为 yyyy-MM-dd HH:mm:ss；只改序列化，不影响入参解析。
 */
@Configuration
public class JacksonConfig {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonDateTimeCustomizer() {
        return builder -> {
            builder.serializerByType(java.time.LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            builder.serializerByType(java.time.LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
        };
    }
}

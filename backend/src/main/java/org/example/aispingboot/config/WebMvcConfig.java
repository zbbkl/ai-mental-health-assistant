package org.example.aispingboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射
 * 上传接口把文件写在本地上传目录下，并通过 /files/** 对外提供访问，
 * 数据库 sys_file_info.file_path 中保存的也是 /files/... 这种相对路径。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 请求 /files/** 时，通配符部分为 "bussiness/article/xxx.png"，
        // 因此 location 要指向上传根目录下的 files 目录（与 sys_file_info.file_path 去掉 /files/ 后一致）
        String location = Paths.get(uploadRoot).toAbsolutePath().normalize()
                .resolve("files")
                .toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}

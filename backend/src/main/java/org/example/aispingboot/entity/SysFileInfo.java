package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统文件信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file_info")
public class SysFileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("original_name")
    private String originalName;

    /** 访问路径，形如 /files/bussiness/article/1756963989972.png */
    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    /** IMG/PDF/DOC/XLS/TXT/ZIP/OTHER */
    @TableField("file_type")
    private String fileType;

    /** 业务类型，如 ARTICLE、USER_AVATAR */
    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private String businessId;

    @TableField("business_field")
    private String businessField;

    @TableField("upload_user_id")
    private Long uploadUserId;

    /** 是否临时文件 0:否 1:是 */
    @TableField("is_temp")
    private Integer isTemp;

    /** 状态 0:删除 1:正常 */
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;
}

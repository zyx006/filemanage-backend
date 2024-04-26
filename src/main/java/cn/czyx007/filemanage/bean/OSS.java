package cn.czyx007.filemanage.bean;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@TableName("oss")
public class OSS implements Serializable {
    /**
     * 唯一ID
     */
    private String id;

    /**
     * 存储策略名
     */
    private String name;

    /**
     * 存储策略类型-0:阿里云 1:腾讯云 2:七牛云 3:本地
     */
    private Integer type;

    /**
     * 存储策略配置信息
     */
    private String config;

    /**
     *  存储策略创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 存储策略更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 存储策略创建者ID
     */
    private String creator;

    /**
     * 存储策略更新者ID
     */
    private String updater;

    /**
     * 是否启用
     */
    private Integer isActive;
}

package cn.czyx007.filemanage.bean;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Files implements Serializable {
    /**
     * 唯一ID
     */
    private String id;

    /**
     * 文件名
     */
    private String name;

    /*
     * 文件存储名(UUID形式)
     */
    private String storeName;

    /**
     * 文件存储路径
     */
    private String path;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件类型-0:图片 1:视频 2:音频 3:文档 4:其他
     */
    private Integer type;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 所属用户ID
     */
    private String userId;

    /**
     * 存储策略ID
     */
    private String ossId;
}

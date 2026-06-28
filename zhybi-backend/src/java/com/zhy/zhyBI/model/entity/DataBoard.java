package com.zhy.zhyBI.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("data_board")
public class DataBoard {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String boardName;

    private Long userId;

    private Integer isShared;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
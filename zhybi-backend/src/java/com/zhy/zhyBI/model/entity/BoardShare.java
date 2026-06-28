package com.zhy.zhyBI.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("board_share")
public class BoardShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 显式指定数据库列名，绕过自动转换配置问题
    @TableField("board_id")
    private Long boardId;

    @TableField("share_code")
    private String shareCode;

    @TableField("share_password")
    private String sharePassword;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("create_user")
    private Long createUser;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_delete")
    private Integer isDelete;
}
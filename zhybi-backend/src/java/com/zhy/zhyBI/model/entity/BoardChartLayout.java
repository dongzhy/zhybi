package com.zhy.zhyBI.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("board_chart_layout")
public class BoardChartLayout {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long boardId;

    private Long chartId;

    private String chartTitle;

    private String chartType;

    private Integer layoutX;
    private Integer layoutY;
    private Integer layoutW;
    private Integer layoutH;
    private Integer minW;
    private Integer minH;

    @TableField("gen_chart")
    private String genChart;

    @TableField("item_key")
    private String itemKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
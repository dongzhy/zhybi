package com.zhy.zhyBI.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class BoardDTO {
    // 看板信息
    private Long boardId;
    private String boardName;
    private Integer isShared;

    // 看板下所有图表+布局
    private List<BoardChartDTO> chartList;
}
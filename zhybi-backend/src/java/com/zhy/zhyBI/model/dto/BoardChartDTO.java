package com.zhy.zhyBI.model.dto;

import lombok.Data;

@Data
public class BoardChartDTO {
    private String itemKey;
    private Long chartId;
    private String chartTitle;
    private String chartType;
    private String genChart;

    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private Integer minW;
    private Integer minH;
}
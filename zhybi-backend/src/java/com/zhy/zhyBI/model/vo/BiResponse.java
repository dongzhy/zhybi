package com.zhy.zhyBI.model.vo;

import lombok.Data;

//bi返回结果
@Data
public class BiResponse {
    private Long chartId;
    private String genChart;
    private String genResult;
}

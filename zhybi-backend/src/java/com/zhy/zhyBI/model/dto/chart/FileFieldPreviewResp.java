package com.zhy.zhyBI.model.dto.chart;

import lombok.Data;
import java.util.List;

@Data
public class FileFieldPreviewResp {
    // 文件名
    private String fileName;
    // 当前文件所有表头字段
    private List<String> fieldList;
}
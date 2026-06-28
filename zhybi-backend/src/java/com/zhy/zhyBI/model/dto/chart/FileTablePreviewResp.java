// 表格预览响应DTO
package com.zhy.zhyBI.model.dto.chart;
import lombok.Data;
import java.util.List;

@Data
public class FileTablePreviewResp {
    private String fileName;
    private List<String> headerList; // 表头列名
    private List<List<Object>> rowDataList; // 行数据
    private int totalRows; // 表格总行数
}
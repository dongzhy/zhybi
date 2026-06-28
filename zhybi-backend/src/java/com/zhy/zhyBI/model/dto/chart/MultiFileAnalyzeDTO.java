// 多文件分析请求DTO（修改版）
package com.zhy.zhyBI.model.dto.chart;
import lombok.Data;
import java.util.Map;

@Data
public class MultiFileAnalyzeDTO {
    private String chartName;
    private String goal;
    private Map<String, TableSelectConfig> selectConfigMap; // 按文件名的勾选配置
}
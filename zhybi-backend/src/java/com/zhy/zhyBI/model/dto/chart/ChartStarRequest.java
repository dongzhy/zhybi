package com.zhy.zhyBI.model.dto.chart;
import lombok.Data;
import javax.validation.constraints.NotNull;
@Data
public class ChartStarRequest {
    @NotNull(message = "图表id不能为空")
    private Long id;
}
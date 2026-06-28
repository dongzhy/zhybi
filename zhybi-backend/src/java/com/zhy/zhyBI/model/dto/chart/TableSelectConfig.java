package com.zhy.zhyBI.model.dto.chart;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TableSelectConfig {
    private List<String> selectColumns = new ArrayList<>();
    private List<Integer> selectRowIndexes = new ArrayList<>();
    private boolean selectAll = false;

    public boolean isEmpty() {
        return !selectAll && selectColumns.isEmpty() && selectRowIndexes.isEmpty();
    }
}
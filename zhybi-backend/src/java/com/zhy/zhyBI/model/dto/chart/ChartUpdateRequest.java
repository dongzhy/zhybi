package com.zhy.zhyBI.model.dto.chart;


import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
* @author <a href="https://github.com/dongzhy">程序员zhy</a>
 */
@Data
public class ChartUpdateRequest implements Serializable {

    /**
     * id
     */

    private Long id;

    /**
     * 名称
     *
     */
    private String name;


    private static final long serialVersionUID = 1L;
}
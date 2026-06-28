package com.zhy.zhyBI.model.dto.chart;

import com.zhy.zhyBI.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


/**
 * 查询请求
 *
* @author <a href="https://github.com/dongzhy">程序员zhy</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */

    private Long id;

    /**
     * 名称
     *
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 创建者ID
     */
    private Long userId;


    /**
     * 创建图标类型
     */
    private String chartType;


    /**
     * 收藏
     */
    private Integer isStar;

    /**
     * 置顶
     */
    private Integer isTop;


    private static final long serialVersionUID = 1L;
}
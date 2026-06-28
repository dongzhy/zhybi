package com.zhy.zhyBI.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhy.zhyBI.model.entity.Chart;
import io.lettuce.core.dynamic.annotation.Param;

/**
* @author 15922
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2026-02-09 13:32:38
* @Entity generator.domain.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    /**
     * 查询回收站分页数据（绕过逻辑删除拦截）
     */
    IPage<Chart> listRecyclePage(Page<Chart> page,
                                 @Param("userId") Long userId,
                                 @Param("name") String name);

}





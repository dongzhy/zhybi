package com.zhy.zhyBI.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zhy.zhyBI.mapper.ChartMapper;
import com.zhy.zhyBI.model.entity.Chart;
import com.zhy.zhyBI.service.ChartService;

import org.springframework.stereotype.Service;

/**
* @author 15922
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2026-02-09 13:32:38
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}





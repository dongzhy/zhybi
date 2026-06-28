package com.zhy.zhyBI.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhy.zhyBI.model.entity.BoardChartLayout;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BoardChartLayoutMapper extends BaseMapper<BoardChartLayout> {

    /**
     * 根据看板ID 查询所有图表布局
     */
    List<BoardChartLayout> selectByBoardId(@Param("boardId") Long boardId);

    /**
     * 根据看板ID 批量删除布局
     */
    int deleteByBoardId(@Param("boardId") Long boardId);
}
package com.zhy.zhyBI.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.mapper.BoardChartLayoutMapper;
import com.zhy.zhyBI.mapper.DataBoardMapper;
import com.zhy.zhyBI.model.dto.BoardChartDTO;
import com.zhy.zhyBI.model.dto.BoardDTO;
import com.zhy.zhyBI.model.entity.BoardChartLayout;
import com.zhy.zhyBI.model.entity.DataBoard;
import com.zhy.zhyBI.service.BoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class BoardServiceImpl extends ServiceImpl<DataBoardMapper, DataBoard>
        implements BoardService {

    @Resource
    private DataBoardMapper dataBoardMapper;

    @Resource
    private BoardChartLayoutMapper layoutMapper;

    @Override
    public List<DataBoard> getUserBoardList(Long userId) {
        LambdaQueryWrapper<DataBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataBoard::getUserId, userId);
        wrapper.eq(DataBoard::getIsDelete, 0);
        return dataBoardMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateBoard(Long userId, BoardDTO boardDTO) {
        // 1. 处理主表 data_board
        DataBoard board = new DataBoard();
        board.setBoardName(boardDTO.getBoardName());
        board.setIsShared(boardDTO.getIsShared());
        board.setUserId(userId);

        Long boardId = boardDTO.getBoardId();
        if (boardId == null) {
            // 新增
            dataBoardMapper.insert(board);
            boardId = board.getId();
        } else {
            // 更新，校验归属
            DataBoard exist = dataBoardMapper.selectById(boardId);
            if (exist == null || !exist.getUserId().equals(userId) || exist.getIsDelete() == 1) {
                throw new BusinessException(403, "无权限操作该看板");
            }
            board.setId(boardId);
            dataBoardMapper.updateById(board);

            // 删除旧布局
            LambdaQueryWrapper<BoardChartLayout> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(BoardChartLayout::getBoardId, boardId);
            layoutMapper.delete(delWrapper);
        }

        // 2. 批量插入新图表布局
        List<BoardChartDTO> chartDtoList = boardDTO.getChartList();
        if (chartDtoList != null && !chartDtoList.isEmpty()) {
            List<BoardChartLayout> layoutList = new ArrayList<>();
            for (BoardChartDTO dto : chartDtoList) {
                BoardChartLayout layout = new BoardChartLayout();
                layout.setBoardId(boardId);
                layout.setChartId(dto.getChartId());
                layout.setChartTitle(dto.getChartTitle());
                layout.setChartType(dto.getChartType());
                layout.setLayoutX(dto.getX());
                layout.setLayoutY(dto.getY());
                layout.setLayoutW(dto.getW());
                layout.setLayoutH(dto.getH());
                layout.setMinW(dto.getMinW());
                layout.setMinH(dto.getMinH());
                layout.setGenChart(dto.getGenChart());
                layout.setItemKey(dto.getItemKey());
                layoutList.add(layout);
            }
            for (BoardChartLayout item : layoutList) {
                layoutMapper.insert(item);
            }
        }
        return boardId;
    }

    @Override
    public BoardDTO getBoardById(Long boardId, Long loginUserId) {
        // 查询看板主表
        DataBoard board = dataBoardMapper.selectById(boardId);
        if (board == null || board.getIsDelete() == 1) {
            throw new BusinessException(404, "看板不存在");
        }
        if (!board.getUserId().equals(loginUserId)) {
            throw new BusinessException(403, "无权查看该看板");
        }

        // 查询关联图表布局
        LambdaQueryWrapper<BoardChartLayout> layoutWrapper = new LambdaQueryWrapper<>();
        layoutWrapper.eq(BoardChartLayout::getBoardId, boardId);
        layoutWrapper.eq(BoardChartLayout::getIsDelete, 0);
        List<BoardChartLayout> layoutList = layoutMapper.selectList(layoutWrapper);

        // 组装返回 DTO
        BoardDTO result = new BoardDTO();
        result.setBoardId(board.getId());
        result.setBoardName(board.getBoardName());
        result.setIsShared(board.getIsShared());

        List<BoardChartDTO> chartDtos = new ArrayList<>();
        for (BoardChartLayout layout : layoutList) {
            BoardChartDTO dto = new BoardChartDTO();
            dto.setItemKey(layout.getItemKey());
            dto.setChartId(layout.getChartId());
            dto.setChartTitle(layout.getChartTitle());
            dto.setChartType(layout.getChartType());
            dto.setGenChart(layout.getGenChart());

            dto.setX(layout.getLayoutX());
            dto.setY(layout.getLayoutY());
            dto.setW(layout.getLayoutW());
            dto.setH(layout.getLayoutH());
            dto.setMinW(layout.getMinW());
            dto.setMinH(layout.getMinH());
            chartDtos.add(dto);
        }
        result.setChartList(chartDtos);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBoard(Long userId, Long boardId) {
        DataBoard board = dataBoardMapper.selectById(boardId);
        if (board == null || board.getIsDelete() == 1 || !board.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限删除或看板不存在");
        }
        // 逻辑删除主表
        board.setIsDelete(1);
        dataBoardMapper.updateById(board);
        // 逻辑删除布局（或物理删除，根据业务）
        LambdaQueryWrapper<BoardChartLayout> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BoardChartLayout::getBoardId, boardId);
        List<BoardChartLayout> layouts = layoutMapper.selectList(wrapper);
        for (BoardChartLayout l : layouts) {
            l.setIsDelete(1);
            layoutMapper.updateById(l);
        }
    }

    @Override
    public boolean checkBoardOwner(Long boardId, Long userId) {
        // 实体创建人字段为 userId，使用 getUserId()
        long count = this.lambdaQuery()
                .eq(DataBoard::getId, boardId)
                .eq(DataBoard::getUserId, userId)
                .count();
        return count > 0;
    }
}
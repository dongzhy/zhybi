package com.zhy.zhyBI.service;

import com.zhy.zhyBI.model.dto.BoardDTO;
import com.zhy.zhyBI.model.entity.DataBoard;
import java.util.List;

public interface BoardService {

    List<DataBoard> getUserBoardList(Long userId);

    Long saveOrUpdateBoard(Long userId, BoardDTO boardDTO);

    BoardDTO getBoardById(Long boardId, Long loginUserId);

    void deleteBoard(Long userId, Long boardId);



    /**
     * 校验看板是否属于当前登录用户
     * @param boardId 看板ID
     * @param userId 当前用户ID
     * @return true=是所有者；false=无权限
     */
    boolean checkBoardOwner(Long boardId, Long userId);
}
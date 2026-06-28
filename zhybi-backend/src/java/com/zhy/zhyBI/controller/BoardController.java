package com.zhy.zhyBI.controller;

import com.zhy.zhyBI.common.BaseResponse;
import com.zhy.zhyBI.common.ResultUtils;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.model.dto.BoardDTO;
import com.zhy.zhyBI.model.entity.DataBoard;
import com.zhy.zhyBI.model.entity.User;
import com.zhy.zhyBI.service.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.zhy.zhyBI.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 数据看板控制器
 */
@RestController
@RequestMapping("/board")
@Slf4j
public class BoardController {

    @Resource
    private BoardService boardService;

    /**
     * 获取当前用户看板简易列表
     */
    @GetMapping("/list")
    public BaseResponse<List<DataBoard>> getBoardList(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(401, "用户未登录，请先登录");
        }
        List<DataBoard> boardList = boardService.getUserBoardList(loginUser.getId());
        return ResultUtils.success(boardList);
    }

    /**
     * 新增/更新看板
     */
    @PostMapping("/save")
    public BaseResponse<Long> saveBoard(@RequestBody BoardDTO boardDTO, HttpServletRequest request) {
        log.info("保存看板入参: {}", boardDTO);
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(401, "用户未登录，请先登录");
        }
        if (boardDTO == null) {
            throw new BusinessException(400, "看板数据不能为空");
        }
        String name = boardDTO.getBoardName();
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "看板名称不能为空");
        }
        Long boardId = boardService.saveOrUpdateBoard(loginUser.getId(), boardDTO);
        return ResultUtils.success(boardId);
    }

    /**
     * 根据ID查询完整看板（含图表布局+echarts配置）
     */
    @GetMapping("/{id}")
    public BaseResponse<BoardDTO> getBoardInfo(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(401, "用户未登录，请先登录");
        }
        if (id == null || id <= 0) {
            throw new BusinessException(400, "看板ID非法");
        }
        BoardDTO boardDTO = boardService.getBoardById(id, loginUser.getId());
        return ResultUtils.success(boardDTO);
    }

    /**
     * 删除看板（级联删除图表布局）
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteBoard(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(401, "用户未登录，请先登录");
        }
        if (id == null || id <= 0) {
            throw new BusinessException(400, "看板ID非法");
        }
        boardService.deleteBoard(loginUser.getId(), id);
        return ResultUtils.success(null);
    }
}
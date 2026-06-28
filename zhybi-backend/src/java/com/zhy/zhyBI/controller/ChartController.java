package com.zhy.zhyBI.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.zhy.zhyBI.annotation.AuthCheck;
import com.zhy.zhyBI.bizmq.BiMessageProducer;
import com.zhy.zhyBI.common.BaseResponse;
import com.zhy.zhyBI.common.DeleteRequest;
import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.common.ResultUtils;
import com.zhy.zhyBI.constant.CommonConstant;
import com.zhy.zhyBI.constant.UserConstant;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.exception.ThrowUtils;
import com.zhy.zhyBI.manager.AiManage;
import com.zhy.zhyBI.manager.ChartWebSocketServer;
import com.zhy.zhyBI.manager.RedisLimiterManage;
import com.zhy.zhyBI.mapper.ChartMapper;
import com.zhy.zhyBI.model.dto.chart.*;
import com.zhy.zhyBI.model.dto.file.UploadFileRequest;
import com.zhy.zhyBI.model.entity.Chart;
import com.zhy.zhyBI.model.entity.User;
import com.zhy.zhyBI.model.enums.FileUploadBizEnum;
import com.zhy.zhyBI.model.vo.BiResponse;
import com.zhy.zhyBI.model.vo.ChartVO;
import com.zhy.zhyBI.service.ChartService;
import com.zhy.zhyBI.service.UserService;
import com.zhy.zhyBI.utils.ExcelUtils;
import com.zhy.zhyBI.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
* @author <a href="https://github.com/dongzhy">程序员zhy</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManage redisLimiterManage;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private AiManage aiManage;

    @Resource
    private BiMessageProducer biMessageProducer;


    @Resource
    private ChartMapper chartMapper;

    private final static Gson gson = new Gson();

    // region 增删改查

    /**
     * 创建
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        // 新增字段默认0
        chart.setIsStar(0);
        chart.setIsTop(0);
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除（逻辑删除 isDelete=1）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 逻辑删除，不物理删除
        Chart update = new Chart();
        update.setId(id);
        update.setIsDelete(1);
        boolean b = chartService.updateById(update);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（管理员）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                         HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表（我的历史图表）
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    // ===================== 新增：收藏、置顶、回收站、重生成 =====================

    /**
     * 切换收藏状态
     */
    @PostMapping("/star/toggle")
    public BaseResponse<Boolean> toggleStar(@RequestBody ChartStarRequest starRequest,
                                              HttpServletRequest request) {
        if (starRequest == null || starRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long chartId = starRequest.getId();
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(chart.getIsDelete() == 1, ErrorCode.OPERATION_ERROR, "回收站图表无法收藏");
        // 校验所有权
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 取反收藏状态
        int newStar = chart.getIsStar() == 1 ? 0 : 1;
        Chart update = new Chart();
        update.setId(chartId);
        update.setIsStar(newStar);
        boolean res = chartService.updateById(update);
        return ResultUtils.success(res);
    }

    /**
     * 切换置顶状态
     */
    @PostMapping("/top/toggle")
    public BaseResponse<Boolean> toggleTop(@RequestBody ChartTopRequest topRequest,
                                             HttpServletRequest request) {
        if (topRequest == null || topRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long chartId = topRequest.getId();
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(chart.getIsDelete() == 1, ErrorCode.OPERATION_ERROR, "回收站图表无法置顶");
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        int newTop = chart.getIsTop() == 1 ? 0 : 1;
        Chart update = new Chart();
        update.setId(chartId);
        update.setIsTop(newTop);
        boolean res = chartService.updateById(update);
        return ResultUtils.success(res);
    }

    /**
     * 查询回收站（已逻辑删除 isDelete = 1）
     */
    @PostMapping("/recycle/list/page/vo")
    public BaseResponse<Page<Chart>> listRecycleChart(@RequestBody ChartQueryRequest chartQueryRequest,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 1. 构造分页参数
        Page<Chart> pageParam = new Page<>(current, size);

        // 2. 调用自定义的 Mapper 方法查询 isDelete = 1 的数据
        Page<Chart> page = (Page<Chart>) chartMapper.listRecyclePage(pageParam, loginUser.getId(), chartQueryRequest.getName());

        return ResultUtils.success(page);
    }
    /**
     * 恢复回收站图表（取消逻辑删除）
     */
    @PostMapping("/recycle/restore")
    public BaseResponse<Boolean> restoreRecycleChart(@RequestBody DeleteRequest req,
                                                         HttpServletRequest request) {
        if (req == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long chartId = req.getId();
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        ThrowUtils.throwIf(chart.getIsDelete() == 0, ErrorCode.OPERATION_ERROR, "该图表未删除，无需恢复");
        // 恢复：isDelete = 0
        Chart update = new Chart();
        update.setId(chartId);
        update.setIsDelete(0);
        boolean res = chartService.updateById(update);
        return ResultUtils.success(res);
    }

    /**
     * 彻底物理删除回收站图表（危险操作）
     */
    @PostMapping("/recycle/realDelete")
    public BaseResponse<Boolean> realDeleteChart(@RequestBody DeleteRequest req,
                                                   HttpServletRequest request) {
        if (req == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long chartId = req.getId();
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 仅允许删除已放入回收站的数据
        ThrowUtils.throwIf(chart.getIsDelete() == 0, ErrorCode.OPERATION_ERROR, "仅回收站数据可彻底删除");
        boolean res = chartService.removeById(chartId);
        return ResultUtils.success(res);
    }

    /**
     * 一键重新生成AI图表（复用MQ异步生成逻辑，使用已有数据，无需上传文件）
     */
    @PostMapping("/reGen/{id}")
    public BaseResponse<BiResponse> reGenChart(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Chart chart = chartService.getById(id);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        ThrowUtils.throwIf(chart.getIsDelete() == 1, ErrorCode.OPERATION_ERROR, "图表已删除，无法重新生成");
        if (!chart.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 校验基础数据完整性
        String csvData = chart.getChartData();
        String goal = chart.getGoal();
        if (StringUtils.isBlank(csvData) || StringUtils.isBlank(goal)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图表缺少原始数据/分析目标，无法重生成");
        }
        // 限流
        redisLimiterManage.doRateLimit("genChartByAi_" + loginUser.getId());
        // 更新状态为等待
        Chart updateWait = new Chart();
        updateWait.setId(id);
        updateWait.setStatus("wait");
        chartService.updateById(updateWait);
        // 组装AI输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chart.getChartType())) {
            userGoal += ",请使用" + chart.getChartType();
        }
        userInput.append("分析目标:").append(userGoal).append("\n");
        userInput.append("原始数据：\n");
        userInput.append("数据：").append(csvData).append("\n");

        // 发送MQ异步生成
        try {
            biMessageProducer.sendMessage(String.valueOf(id));
            ChartWebSocketServer.sendChartStatus(loginUser.getId().toString(), id, "wait", "重新生成任务已提交");
        } catch (Exception e) {
            // MQ发送失败，回滚状态
            Chart failChart = new Chart();
            failChart.setId(id);
            failChart.setStatus("failed");
            failChart.setExecMessage("重生成消息队列发送失败：" + e.getMessage());
            chartService.updateById(failChart);
            ChartWebSocketServer.sendChartStatus(loginUser.getId().toString(), id, "failed", failChart.getExecMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重生成任务提交失败");
        }
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(id);
        return ResultUtils.success(biResponse);
    }

    // ===================== 仪表盘统计接口 =====================

    /**
     * 【仪表盘】全局统计（总数、成功、运行中）
     */
    @GetMapping("/dashboard/statistics")
    public BaseResponse<Map<String, Object>> getDashboardStatistics(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        Map<String, Object> statistics = new HashMap<>();
        // 图表总数（未删除）
        statistics.put("total", chartService.count(new QueryWrapper<Chart>()
                .eq("userId", userId)
                .eq("isDelete", 0)));
        // 成功数量
        statistics.put("successCount", chartService.count(new QueryWrapper<Chart>()
                .eq("userId", userId)
                .eq("status", "success")
                .eq("isDelete", 0)));
        // 运行中/排队
        statistics.put("runningCount", chartService.count(new QueryWrapper<Chart>()
                .eq("userId", userId)
                .in("status", "running", "wait")
                .eq("isDelete", 0)));

        return ResultUtils.success(statistics);
    }

    /**
     * 【仪表盘】近7天图表生成统计（修复SQL注入，使用参数绑定）
     */
    @GetMapping("/dashboard/7days")
    public BaseResponse<Map<String, Object>> getLast7DaysStatistics(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        Map<String, Object> result = new HashMap<>();
        List<String> dateList = new ArrayList<>();
        List<Integer> countList = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd"));

            QueryWrapper<Chart> qw = new QueryWrapper<>();
            qw.eq("userId", userId);
            qw.eq("isDelete", 0);
            // 修复：参数绑定，无SQL注入风险
            qw.apply("DATE(createTime) = ?", date);

            long count = chartService.count(qw);
            dateList.add(i == 0 ? "今天" : dateStr);
            countList.add((int) count);
        }

        result.put("dates", dateList);
        result.put("counts", countList);
        return ResultUtils.success(result);
    }

    @GetMapping("/dashboard/type")
    public BaseResponse<List<Map<String, Object>>> getChartTypeStatistics(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.select("chartType", "COUNT(*) as count");
        queryWrapper.groupBy("chartType");

        List<Map<String, Object>> mapList = chartService.listMaps(queryWrapper);

        Map<String, Integer> typeCountMap = new HashMap<>();
        for (Map<String, Object> item : mapList) {
            String chartType = (String) item.get("chartType");
            Integer count = ((Number) item.get("count")).intValue();

            if (StringUtils.isBlank(chartType)) {
                chartType = "未知";
            }

            typeCountMap.put(chartType, typeCountMap.getOrDefault(chartType, 0) + count);
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            Map<String, Object> resultItem = new HashMap<>();
            resultItem.put("chartType", entry.getKey());
            resultItem.put("count", entry.getValue());
            resultList.add(resultItem);
        }

        return ResultUtils.success(resultList);
    }

    /**
     * 编辑（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类（新增isStar、isTop筛选）
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        // 新增筛选条件
        Integer isStar = chartQueryRequest.getIsStar();
        Integer isTop = chartQueryRequest.getIsTop();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 新增收藏、置顶筛选
        queryWrapper.eq(isStar != null, "isStar", isStar);
        queryWrapper.eq(isTop != null, "isTop", isTop);

        queryWrapper.eq("isDelete", false);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    /**
     * 清理图表配置字符串
     */
    public static String cleanChartString(String rawString) {
        if (rawString == null) {
            return "";
        }
        return rawString.replace("\\n", "\n").trim();
    }

    /**
     * 清理分析结果字符串
     */
    public static String cleanResultString(String rawString) {
        if (rawString == null) {
            return "";
        }
        String temp = rawString.replace("\\n", "\n");
        int suffixStartIndex = temp.indexOf("\"},\"Index\"");
        if (suffixStartIndex > 0) {
            temp = temp.substring(0, suffixStartIndex);
        }
        return temp.trim();
    }

    /**
     * 智能分析 同步
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartAi(@RequestPart("file") MultipartFile multipartFile,
                                                  GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        if (StringUtils.isBlank(name) || name.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
        if (StringUtils.isBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标为空");
        }

        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        if (size > ONE_MB) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件超过1M");
        }

        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        if (!validFileSuffix.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀非法");
        }

        User loginUser = userService.getLoginUser(request);
        redisLimiterManage.doRateLimit("genChartByAi_" + loginUser.getId());

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append("分析目标:").append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("数据：").append(csvData).append("\n");

        long biModeId = 123456688L;
        String result = aiManage.doChat(biModeId, userInput.toString());

        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        String cleanGenChart = cleanChartString(genChart);
        String cleanGenResult = cleanResultString(genResult);

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(cleanGenChart);
        chart.setGenResult(cleanGenResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus("success");
        chart.setIsStar(0);
        chart.setIsTop(0);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(cleanGenChart);
        biResponse.setGenResult(cleanGenResult);
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析(异步线程池)
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                        GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        if (StringUtils.isBlank(name) || name.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
        if (StringUtils.isBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标为空");
        }

        User loginUser = userService.getLoginUser(request);
        long todayCount = getUserTodayChartCount(loginUser.getId());
        if (todayCount >= 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "今日生成次数已达上限（20条/天）");
        }

        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        if (size > ONE_MB) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件超过1M");
        }

        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        if (!validFileSuffix.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀非法");
        }

        redisLimiterManage.doRateLimit("genChartByAi_" + loginUser.getId());

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append("分析目标:").append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("数据：").append(csvData).append("\n");

        long biModeId = 123456688L;
        String result = aiManage.doChat(biModeId, userInput.toString());

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setIsStar(0);
        chart.setIsTop(0);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        CompletableFuture.runAsync(() -> {
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }

            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(), "Ai生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            String cleanGenChart = cleanChartString(genChart);
            String cleanGenResult = cleanResultString(genResult);

            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(cleanGenChart);
            updateChartResult.setGenResult(cleanGenResult);
            updateChartResult.setStatus("success");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析(异步消息队列 MQ)
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartAiAsyncMq(
            @RequestPart("file") MultipartFile multipartFile,
            GenChartByAiRequest genChartByAiRequest,
            HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        if (StringUtils.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表名称不能为空");
        }
        if (name.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表名称过长（最多100个字符）");
        }
        if (StringUtils.isBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分析目标不能为空");
        }

        if (multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的文件为空");
        }
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        if (size > ONE_MB) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过1M限制");
        }

        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        if (StringUtils.isBlank(suffix) || !validFileSuffix.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持xlsx/xls格式的Excel文件");
        }

        User loginUser = userService.getLoginUser(request);
        long todayCount = getUserTodayChartCount(loginUser.getId());
        if (todayCount >= 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "今日生成次数已达上限（20条/天）");
        }
        redisLimiterManage.doRateLimit("genChartByAi_" + loginUser.getId());

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append("分析目标:").append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        String csvData;
        try {
            csvData = ExcelUtils.excelToCsv(multipartFile);
            if (StringUtils.isBlank(csvData)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Excel文件内容为空");
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Excel文件解析失败：" + e.getMessage());
        }
        userInput.append("数据：").append(csvData).append("\n");

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setIsStar(0);
        chart.setIsTop(0);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表数据保存失败");
        Long newChartId = chart.getId();

        String userIdStr = loginUser.getId().toString();

        try {
            biMessageProducer.sendMessage(String.valueOf(newChartId));
            ChartWebSocketServer.sendChartStatus(userIdStr, newChartId, "wait", "任务已提交，等待处理....");
        } catch (Exception e) {
            chartService.removeById(newChartId);
            ChartWebSocketServer.sendChartStatus(userIdStr, newChartId, "failed", "消息队列发送失败，生成任务取消");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息队列发送失败，生成任务取消：" + e.getMessage());
        }

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        Chart chart = chartService.getById(chartId);
        if (chart != null) {
            ChartWebSocketServer.sendChartStatus(
                    chart.getUserId().toString(),
                    chartId,
                    "failed",
                    execMessage
            );
        }
        if (!updateResult) {
            log.error("更新图表状态失败" + chartId + "," + execMessage);
        }
    }

    /**
     * 管理员分页查询所有图表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPageAdmin(
            @RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> page = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 管理员删除图表
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteChart(
            @RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = chartService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 管理员编辑图表
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateChart(
            @RequestBody ChartUpdateRequest request) {
        if (request == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(request, chart);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 查询用户今日已生成的图表数量
     */
    private long getUserTodayChartCount(long userId) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.apply("DATE(createTime) = CURDATE()");
        return chartService.count(queryWrapper);
    }

    /**
     * 获取当前用户所有图表简易列表（大屏下拉选择AI图表）
     */
    @GetMapping("/list")
    public BaseResponse<List<AiChartSimple>> listSimpleChart(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        QueryWrapper<Chart> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", userId);
        wrapper.eq("isDelete", 0);
        wrapper.select("id", "name", "goal");
        List<Chart> chartList = chartService.list(wrapper);

        List<AiChartSimple> voList = new ArrayList<>();
        for (Chart chart : chartList) {
            AiChartSimple vo = new AiChartSimple();
            vo.setId(chart.getId());
            vo.setName(chart.getName());
            vo.setGoal(chart.getGoal());
            voList.add(vo);
        }
        return ResultUtils.success(voList);
    }
}
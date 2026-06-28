package com.zhy.zhyBI.controller;

import com.zhy.zhyBI.bizmq.ChartTaskProducer;
import com.zhy.zhyBI.common.BaseResponse;
import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.common.ResultUtils;
import com.zhy.zhyBI.constant.UserConstant;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.model.dto.chart.*;
import com.zhy.zhyBI.model.entity.Chart;
import com.zhy.zhyBI.model.entity.User;
import com.zhy.zhyBI.service.ChartService;
import com.zhy.zhyBI.utils.FileParseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/smart")
@Slf4j
public class SmartAnalysisController {
    private static final int MAX_FILES = 5;
    private static final int MAX_DATA_LEN = 8000;
    private static final int PREVIEW_MAX_ROWS = 100;
    private static final String REDIS_LOCK_PREFIX = "smart_analysis:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    // Session 键：存储 文件名 -> 文件字节数组
    private static final String SESSION_FILE_BYTES = "session_excel_file_bytes";

    @Resource
    private ChartService chartService;
    @Resource
    private ChartTaskProducer chartTaskProducer;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 分布式锁
    private boolean tryLock(String lockKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_WAIT_TIME, TimeUnit.SECONDS));
    }

    private void releaseLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    /**
     * 文件上传 & 预览
     * 逻辑：读取字节 -> 存入Session -> 立即释放MultipartFile
     */
    @PostMapping("/previewTable")
    public BaseResponse<List<FileTablePreviewResp>> previewTable(
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest request,
            HttpSession session) {
        User loginUser = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传文件");
        }
        if (files.length > MAX_FILES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多上传5个文件");
        }

        Long userId = loginUser.getId();
        String lockKey = REDIS_LOCK_PREFIX + userId;
        if (!tryLock(lockKey)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        try {
            Map<String, byte[]> fileBytesMap = new HashMap<>();
            List<FileTablePreviewResp> respList = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                if (fileName == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名为空");
                }
                String lowerName = fileName.toLowerCase();
                if (!lowerName.endsWith(".xls") && !lowerName.endsWith(".xlsx")) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 .xls / .xlsx 格式Excel");
                }
                if (file.getSize() <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件【" + fileName + "】为空文件");
                }

                // 【关键】一次性读取字节，此时文件在内存，无磁盘依赖
                byte[] bytes = file.getBytes();
                fileBytesMap.put(fileName, bytes);

                // 解析预览（纯字节数组）
                FileTablePreviewResp resp = FileParseUtils.parseExcelTablePreview(bytes, PREVIEW_MAX_ROWS);
                resp.setFileName(fileName);
                respList.add(resp);
            }

            // 字节数组存入Session，后续只用字节，彻底不用MultipartFile
            session.setAttribute(SESSION_FILE_BYTES, fileBytesMap);
            return ResultUtils.success(respList);
        } catch (IOException e) {
            log.error("读取文件字节异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        } catch (Exception e) {
            log.error("Excel预览解析异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析文件失败");
        } finally {
            releaseLock(lockKey);
        }
    }

    /**
     * 提交分析
     * 全程只使用 Session 中的 byte[]，不触碰任何 MultipartFile
     */
    @PostMapping("/multi-analyze")
    public BaseResponse<Long> multiAnalyze(
            @RequestBody MultiFileAnalyzeDTO dto,
            HttpServletRequest request,
            HttpSession session) {
        User loginUser = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = loginUser.getId();

        // 从Session取出字节数组Map
        @SuppressWarnings("unchecked")
        Map<String, byte[]> fileBytesMap = (Map<String, byte[]>) session.getAttribute(SESSION_FILE_BYTES);
        if (fileBytesMap == null || fileBytesMap.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先上传文件，或文件已过期");
        }

        // 参数校验
        if (StringUtils.isBlank(dto.getChartName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表名称不能为空");
        }
        Map<String, TableSelectConfig> selectConfigMap = dto.getSelectConfigMap();
        if (selectConfigMap == null || selectConfigMap.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须勾选分析数据");
        }

        // 拼接Excel数据
        StringBuilder dataSb = new StringBuilder();
        try {
            for (Map.Entry<String, TableSelectConfig> entry : selectConfigMap.entrySet()) {
                String fileName = entry.getKey();
                TableSelectConfig config = entry.getValue();
                if (config.isEmpty()) {
                    continue;
                }
                byte[] fileBytes = fileBytesMap.get(fileName);
                if (fileBytes == null) {
                    continue;
                }
                // 纯字节解析，无文件依赖
                String content = FileParseUtils.parseFileBySelectConfig(fileBytes, config);
                dataSb.append("\n=== ").append(fileName).append(" ===\n");
                dataSb.append(content);
            }
        } catch (Exception e) {
            log.error("Excel数据解析异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析文件失败");
        }

        // 截断超长文本
        String chartData = dataSb.length() > MAX_DATA_LEN
                ? dataSb.substring(0, MAX_DATA_LEN)
                : dataSb.toString();

        // 入库：修复 id=0 问题（mybatis 自增主键正常使用）
        Chart chart = new Chart();
        chart.setName(dto.getChartName());
        chart.setGoal(StringUtils.isBlank(dto.getGoal()) ? "分析数据" : dto.getGoal());
        chart.setChartData(chartData);
        chart.setStatus("wait");
        chart.setUserId(userId);
        // 保存数据
        chartService.save(chart);
        Long chartId = chart.getId();
        log.info("图表任务入库成功，ID:{} , 用户ID:{}", chartId, userId);

        // 发送MQ异步任务
        chartTaskProducer.sendTask(chartId);

        // 用完清空Session，释放内存
        session.removeAttribute(SESSION_FILE_BYTES);
        return ResultUtils.success(chartId);
    }

    /**
     * 查询任务结果
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Chart> getChart(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务ID非法");
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }
        return ResultUtils.success(chart);
    }
}
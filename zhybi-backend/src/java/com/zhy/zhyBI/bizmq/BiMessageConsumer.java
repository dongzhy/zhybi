package com.zhy.zhyBI.bizmq;

import com.rabbitmq.client.Channel;
import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.manager.AiManage;
import com.zhy.zhyBI.manager.ChartWebSocketServer;
import com.zhy.zhyBI.model.entity.Chart;
import com.zhy.zhyBI.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.zhy.zhyBI.controller.ChartController.cleanChartString;
import static com.zhy.zhyBI.controller.ChartController.cleanResultString;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;
    @Resource
    private AiManage aiManage;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"bi_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("开始处理MQ消息，message = {}", message);
        // 1. 校验消息合法性
        if (StringUtils.isBlank(message)) {
            log.error("MQ消息为空，拒绝确认");
            channel.basicNack(deliveryTag, false, true);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        Long chartId;
        try {
            chartId = Long.parseLong(message);
        } catch (NumberFormatException e) {
            log.error("chartId格式错误：{}", message, e);
            channel.basicNack(deliveryTag, false, true);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chartId格式错误");
        }

        // 2. 查询图表
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.error("图表不存在，chartId：{}", chartId);
            channel.basicNack(deliveryTag, false, true);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        String userIdStr = chart.getUserId().toString();

        try{
            // ========== 推送：执行中 ==========
            ChartWebSocketServer.sendChartStatus(userIdStr, chartId, "running", "正在生成图表");
            // 3. 更新图表状态为执行中
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean updateRunningStatus = chartService.updateById(updateChart);
            if (!updateRunningStatus) {
                log.error("更新图表{}为running状态失败", chartId);
                channel.basicNack(deliveryTag, false, true);
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }
            log.info("图表{}状态已更新为running", chartId);
        }catch (BusinessException e){
            // 异常也推送失败
            handleChartUpdateError(chartId, e.getMessage());
        }

        try {
            // 拼接用户输入
            String userInput = buildUserInput(chart);
            // 调用AI
            long biModeId = 123456688L;
            String result = aiManage.doChat(biModeId, userInput);
            // 切割结果
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成错误");
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            // 清理
            String cleanGenChart = cleanChartString(genChart);
            String cleanGenResult = cleanResultString(genResult);

            // 4. 更新数据库
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(cleanGenChart);
            updateChartResult.setGenResult(cleanGenResult);
            updateChartResult.setStatus("success");
            boolean updateSuccessStatus = chartService.updateById(updateChartResult);
            if (!updateSuccessStatus) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表成功状态失败");
            }

            // 5. 确认MQ消息
            channel.basicAck(deliveryTag, false);
            log.info("图表{}处理完成，状态更新为success，MQ消息已确认", chartId);

            // ========== 推送：成功（前端收到会取消loading） ==========
            ChartWebSocketServer.sendChartStatus(
                    chart.getUserId().toString(),
                    chartId,
                    "success",
                    "图表生成完成"
            );

        } catch (Exception e) {
            // 失败处理
            log.error("处理图表{}失败", chartId, e);
            handleChartUpdateError(chart.getId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 构造用户输入
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        String userGoal = goal;
        // 拼接核心目标
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append("分析目标:").append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append("数据：").append(csvData).append("\n");
        return userInput.toString();
    }

    /**
     * 处理图表更新失败的逻辑
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        // ========== 推送：失败（前端取消loading） ==========
        Chart chart = chartService.getById(chartId);
        if (chart != null) {
            ChartWebSocketServer.sendChartStatus(
                    chart.getUserId().toString(),
                    chartId,
                    "failed",
                    execMessage
            );
            if (!updateResult) {
                log.error("更新图表{}失败状态失败，错误信息：{}", chartId, execMessage);
            } else {
                log.info("图表{}状态已更新为failed，错误信息：{}", chartId, execMessage);
            }
        }
    }
}
package com.zhy.zhyBI.bizmq;

import com.alibaba.fastjson2.JSON;
import com.zhy.zhyBI.manager.AiManage;
import com.zhy.zhyBI.model.entity.Chart;
import com.zhy.zhyBI.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class MultiFileConsumer {
    // 固定AI提示词（沿用你原有规则）
    private final String systemPrompt = "你是专业数据分析师，根据用户多文件数据进行数据分析，严格遵守以下强制规则：\n" +
            "1. 按照用户给定分析目标进行数据挖掘，无自定义目标则自主挖掘数据规律、异常、统计结论；\n" +
            "2. 数据中存在表格、结构化数据必须生成完整ECharts option配置；纯文本无可用图表数据，genChart固定为{}空对象；\n" +
            "3. 输出**只能是纯净JSON字符串**，禁止```、注释、多余中文说明、markdown格式；\n" +
            "4. JSON固定结构：{\"genChart\":{},\"genResult\":\"文字分析结论\"}，genChart值为标准echarts option JS对象。";

    @Resource
    private ChartService chartService;
    @Resource
    private AiManage aiManage;

    @RabbitListener(queues = MqConfig.MULTI_QUEUE)
    public void consume(Long chartId){
        Chart chart = chartService.getById(chartId);
        if(chart == null){
            log.error("任务不存在:{}",chartId);
            return;
        }
        // 已处理任务跳过，防止重复消费
        if("success".equals(chart.getStatus()) || "fail".equals(chart.getStatus())) return;
        try {
            // 拼接用户数据
            String userInput = "【分析目标】："+chart.getGoal()+"\n【筛选后数据】：\n"+chart.getChartData();
            // 调用AI
            String aiJson = aiManage.doChatWithSysPrompt(0L,systemPrompt,userInput);
            // 解析固定JSON
            Map<String,Object> result = JSON.parseObject(aiJson);
            String genChart = JSON.toJSONString(result.get("genChart"));
            String genResult = String.valueOf(result.get("genResult"));

            // 更新数据库
            chart.setGenChart(genChart);
            chart.setGenResult(genResult);
            chart.setStatus("success");
            chartService.updateById(chart);
            log.info("{} 生成成功入库",chartId);
        }catch (Exception e){
            log.error("生成失败:{}",chartId,e);
            chart.setStatus("fail");
            chart.setGenResult("AI生成异常："+e.getMessage());
            chartService.updateById(chart);
        }
    }
}
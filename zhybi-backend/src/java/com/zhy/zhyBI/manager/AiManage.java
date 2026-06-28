package com.zhy.zhyBI.manager;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AiManage {

    @Resource
    private ZhipuAiClient zhipuClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 原有老方法：内置固定系统提示词（旧图表【【【分隔格式】，老接口继续使用不动
     */
    public String doChat(long modelId, String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI对话内容不能为空");
        }

        log.info("老方法AI对话，模型ID：{}，消息长度：{}", modelId, message.length());

        final String systemPrompt = "你是一个数据分析师和前端开发专家。请严格按照以下固定格式输出\n" +
                "分析需求:\n" +
                "{数据分析的需求或目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端Echarts v5的option配置对象js代码，合理将数据进行可视化，不要生成任何多余内容，比如注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论，越详细越好，不要生成多余的注释}";

        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(systemPrompt)
                .build();
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(message)
                .build();
        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model("glm-4.5-air")
                .messages(messages)
                .stream(false)
                .temperature(0.6f)
                .maxTokens(1024)
                .build();

        try {
            long startTime = System.currentTimeMillis();
            ChatCompletionResponse response = zhipuClient.chat().createChatCompletion(request);
            long endTime = System.currentTimeMillis();

            // 提取AI原生回答文本（修复原BUG：不再返回完整api外层json）
            if (response.getData() == null || response.getData().getChoices() == null || response.getData().getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型无返回内容");
            }
            String aiContent = response.getData().getChoices().get(0).getMessage().getContent().toString();
            log.info("老接口调用成功，耗时{}ms", endTime - startTime);
            return aiContent;

        } catch (Exception e) {
            log.error("老方法AI异常", e);
            throw wrapAiException(e);
        }
    }


    // ===================== 新增：Controller自定义系统提示词专用方法 =====================
    /**
     * 新方法：系统提示词+用户消息全部由Controller传入，适配智能多文件分析
     * @param modelId 预留模型id
     * @param sysPrompt 控制器传入的自定义系统指令（JSON格式约束）
     * @param userContent 用户数据+分析目标
     * @return AI原生返回字符串（纯JSON {genChart,genResult}）
     */
    public String doChatWithSysPrompt(long modelId, String sysPrompt, String userContent) {
        // 参数校验
        if ((sysPrompt == null || sysPrompt.isBlank()) || (userContent == null || userContent.isBlank())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统提示词/用户内容不能为空");
        }

        log.info("新方法AI请求｜模型ID:{}｜系统词长:{}｜用户内容长:{}",
                modelId, sysPrompt.length(), userContent.length());

        // 组装消息
        ChatMessage systemMsg = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(sysPrompt)
                .build();
        ChatMessage userMsg = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(userContent)
                .build();
        List<ChatMessage> msgList = Arrays.asList(systemMsg, userMsg);

        // 调大maxTokens适配多文件大数据
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("glm-4.5-air")
                .messages(msgList)
                .stream(false)
                .temperature(0.6f)
                .maxTokens(8192)
                .build();

        try {
            long start = System.currentTimeMillis();
            ChatCompletionResponse resp = zhipuClient.chat().createChatCompletion(params);
            long cost = System.currentTimeMillis() - start;

            // ✅ 1. 打印完整响应 JSON，排查 API 原始返回
            try {
                log.info("AI原始完整响应: {}", objectMapper.writeValueAsString(resp));
            } catch (JsonProcessingException e) {
                log.warn("序列化AI响应失败", e);
            }

            if (resp.getData() == null || resp.getData().getChoices() == null || resp.getData().getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回空数据");
            }

            // ✅ 2. 打印结束原因，如果是 content_filter 说明被风控拦截了
            String finishReason = resp.getData().getChoices().get(0).getFinishReason();
            log.info("AI FinishReason: {}", finishReason);

            // ✅ 3. 安全获取 content，防止空指针
            Object contentObj = resp.getData().getChoices().get(0).getMessage().getContent();
            String aiResult = (contentObj == null) ? "" : contentObj.toString();

            log.info("新接口调用成功，耗时{}ms，返回长度{}", cost, aiResult.length());

            if (aiResult.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI返回内容为空，finishReason: " + finishReason);
            }

            return aiResult;

        } catch (Exception e) {
            log.error("自定义Prompt调用AI异常", e);
            throw wrapAiException(e);
        }
    }

    /**
     * 统一异常包装工具
     */
    private BusinessException wrapAiException(Exception e) {
        String errMsg = "AI服务异常";
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("Authentication")) {
            errMsg = "API密钥配置错误";
        } else if (msg.contains("Rate limit")) {
            errMsg = "接口调用超限，请稍后重试";
        } else {
            errMsg = "AI服务异常：" + msg;
        }
        return new BusinessException(ErrorCode.OPERATION_ERROR, errMsg);
    }

}
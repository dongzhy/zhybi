package com.zhy.zhyBI.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TemplatePromptRenderer {

    /**
     * 渲染模板 Prompt
     * @param promptTemplate 模板预设，如："分析各{region}的{sales}趋势，输出折线图"
     * @param fieldMapping   映射关系，如：{"region": "所属省份", "sales": "销售总额"}
     * @return 渲染后的 Prompt，如："分析各所属省份的销售总额趋势，输出折线图"
     */
    public String renderPrompt(String promptTemplate, Map<String, String> fieldMapping) {
        String renderedPrompt = promptTemplate;
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}"; // {region}
            String realColumnName = entry.getValue();       // 所属省份
            renderedPrompt = renderedPrompt.replace(placeholder, realColumnName);
        }
        return renderedPrompt;
    }
}

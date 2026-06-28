import React from 'react';
import { Card, Typography, Space, Collapse } from 'antd';
import ReactEcharts from "echarts-for-react";
// @ts-ignore
import echarts from 'echarts';

const { Paragraph } = Typography;

interface ChartRendererProps {
  aiResult: string;
}

const ChartRenderer: React.FC<ChartRendererProps> = ({ aiResult }) => {
  // 万能解析echarts option
  const extractEchartsOption = (rawGenChartStr: string) => {
    if (!rawGenChartStr || !rawGenChartStr.trim()) return null;
    try {
      let code = rawGenChartStr.trim()
        .replace(/[\n\t\r]/g, ' ')
        .replace(/^option\s*=\s*/i, '')
        .replace(/;+\s*$/, '');

      // 优先JSON安全解析，失败再用Function兜底
      let option: any = null;
      try {
        option = JSON.parse(code);
      } catch {
        // JSON失败才使用new Function
        option = new Function('return ' + code)();
      }

      if (!option || !option.series || !Array.isArray(option.series)) return null;
      return option;
    } catch (err) {
      console.error('图表配置解析失败：', err, rawGenChartStr);
      return null;
    }
  };

  // 【移入组件内部】aiResult变化自动重新解析
  const parseAiResult = () => {
    let chartOption: any = null;
    let analysisText = '';
    let rawChartStr = '';

    if (!aiResult) return { chartOption: null, analysisText: '' };
    let text = aiResult.trim();

    // 修复：正确匹配 ```json 代码块
    const mdCodeReg = /```(?:json)?\s*\n?([\s\S]*?)\n?```/;
    const mdMatch = text.match(mdCodeReg);
    if (mdMatch?.[1]) {
      text = mdMatch[1].trim();
    }

    // JSON解析分支
    try {
      const jsonObj = JSON.parse(text);
      if (typeof jsonObj === 'object' && jsonObj !== null) {
        // 后端固定格式 {genResult:"",genChart:"option对象字符串/对象"}
        if ('genChart' in jsonObj || 'genResult' in jsonObj) {
          analysisText = jsonObj.genResult ?? '';
          rawChartStr = typeof jsonObj.genChart === 'string'
            ? jsonObj.genChart
            : JSON.stringify(jsonObj.genChart);
        } else if (jsonObj.series) {
          // 直接是echarts option
          chartOption = jsonObj;
          rawChartStr = text;
        } else {
          // 普通JSON文本
          analysisText = text;
        }
      } else {
        analysisText = text;
      }
    } catch {
      // JSON解析失败，全文当做图表字符串尝试解析
      rawChartStr = text;
    }

    // 解析echarts配置
    if (rawChartStr && !chartOption) {
      const option = extractEchartsOption(rawChartStr);
      if (option) {
        chartOption = option;
      } else {
        // 图表解析失败，原始全量当做分析文本（修复：不丢数据）
        if (!analysisText) analysisText = aiResult;
      }
    }

    return { chartOption, analysisText };
  };

  // ✅ 放到函数内部，props变更自动重算
  const { chartOption, analysisText } = parseAiResult();

  return (
    <Card title="分析结果" style={{ marginTop: 16 }}>
      <Space direction="vertical" style={{ width: "100%" }} size="large">
        {/* 文本分析结论 */}
        {analysisText && (
          <div style={{ marginTop: 12, color: '#666' }}>
            <strong>分析结论：</strong>
            <Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0, marginTop: 4 }}>
              {analysisText}
            </Paragraph>
          </div>
        )}

        {/* ECharts图表 */}
        {chartOption ? (
          <ReactEcharts
            echarts={echarts}
            option={chartOption}
            style={{ height: 380, marginBottom: 12 }}
          />
        ) : (
          !analysisText && (
            <div style={{
              height: 320,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: '#fafafa',
              borderRadius: 8,
              color: '#999',
            }}>
              图表配置解析失败，无法显示
            </div>
          )
        )}

        {/* 原始数据折叠面板 */}
        <Collapse ghost>
          <Collapse.Panel header="查看 AI 原始返回数据" key="1">
            <pre style={{ maxHeight: '300px', overflow: 'auto', background: '#f5f5f5', padding: '10px', borderRadius: '4px', fontSize: '12px' }}>
              {aiResult}
            </pre>
          </Collapse.Panel>
        </Collapse>
      </Space>
    </Card>
  );
};

export default ChartRenderer;

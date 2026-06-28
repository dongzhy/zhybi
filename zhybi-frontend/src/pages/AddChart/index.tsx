import { UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Col, Divider, Form, Input, Row, Select, Space, Spin, Upload } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import { genChartAiUsingPost } from "@/services/swagger/chartController";
import TextArea from "antd/es/input/TextArea";
import ReactEcharts from "echarts-for-react";
// 手动引入ECharts模块（确保渲染正常）
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
// 注册ECharts模块
echarts.use([TitleComponent, TooltipComponent, GridComponent, LineChart, CanvasRenderer]);

const useStyles = createStyles(({ token }) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    overflow: 'auto',
    backgroundImage: "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
    backgroundSize: '100% 100%',
  },
}));

interface UploadFileItem {
  originFileObj: File;
  name: string;
  status: string;
  uid: string;
}

const AddChart: React.FC = () => {
  const { styles } = useStyles();
  const [form] = Form.useForm();
  const [chartOption, setChartOption] = useState<any>(null); // ECharts最终配置
  const [genResult, setGenResult] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [uploadFile, setUploadFile] = useState<UploadFileItem | null>(null);
  const [errorMsg, setErrorMsg] = useState<string>("");

  // 处理文件上传
  const handleFileChange = (fileList: UploadFileItem[]) => {
    const file = fileList.length > 0 ? fileList[fileList.length - 1] : null;
    setUploadFile(file);
    if (file) setErrorMsg("");
  };

  // 核心：提取后端返回的option配置（只保留{}内的内容）
  const extractEchartsOption = (rawStr: string) => {
    if (!rawStr) return null;
    try {
      // 第一步：匹配{}之间的所有内容（精准提取配置）
      const match = rawStr.match(/option\s*=\s*({[\s\S]*});/);
      if (!match || !match[1]) {
        setErrorMsg("未提取到有效的图表配置");
        return null;
      }
      // 第二步：解析为JS对象（兼容单引号/末尾逗号）
      const pureOption = new Function(`return ${match[1]};`)();
      // 第三步：补充基础配置（保证图表样式完整）
      return {
        title: { text: form.getFieldValue('name') || '数据可视化图表' },
        tooltip: { trigger: 'axis' }, // 鼠标悬浮提示
        grid: { left: '5%', right: '5%', bottom: '8%', top: '15%' }, // 图表边距
        ...pureOption // 合并后端返回的核心配置
      };
    } catch (error: any) {
      console.error("解析配置失败：", error);
      setErrorMsg(`图表解析失败：${error.message.slice(0, 50)}`);
      return null;
    }
  };

  // 格式化分析结论
  const formatGenResult = (resultStr: string) => {
    return resultStr ? resultStr.replace(/\\n/g, '\n').trim() : "暂无分析结论";
  };

  // 提交表单
  const onFinish = async (values: any) => {
    if (submitting || !uploadFile) {
      if (!uploadFile) setErrorMsg("请先上传CSV文件！");
      return;
    }

    setSubmitting(true);
    setErrorMsg("");
    setChartOption(null);
    setGenResult("");

    try {
      const requestParams = {
        goal: values.goal,
        name: values.name,
        chartType: values.chartType,
      };

      // 调用后端接口
      const res = await genChartAiUsingPost(
        requestParams,
        requestParams,
        uploadFile.originFileObj,
      );

      if (res?.data) {
        // 提取并设置图表配置
        const option = extractEchartsOption(res.data.genChart || '');
        if (option) setChartOption(option);
        // 设置分析结论
        setGenResult(formatGenResult(res.data.genResult || ''));
      } else {
        setErrorMsg("后端返回数据为空");
      }
    } catch (error: any) {
      console.error("请求失败：", error);
      setErrorMsg(`请求失败：${error.message || '未知错误'}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="add-chart" style={{ padding: 20  }}>
      <Row gutter={24}>
        {/* 左侧表单区域 */}
        <Col span={12}>
          <div style={{minHeight:100}}>
            <Card title="智能分析" >
              {errorMsg && <Alert message={errorMsg} type="error" style={{ marginBottom: 16 }} />}
              <Form
                form={form}
                labelCol={{ span: 4 }}
                wrapperCol={{ span: 16 }}
                onFinish={onFinish}
                initialValues={{ chartType: '折线图' }}
              >
                <Form.Item
                  name="goal"
                  label="分析目标"
                  rules={[{ required: true, message: '请输入分析目标' }]}
                >
                  <TextArea placeholder="比如：分析用户增长趋势" rows={4} />
                </Form.Item>

                <Form.Item name="name" label="图表名称">
                  <Input placeholder="可选，如：用户增长折线图" />
                </Form.Item>

                <Form.Item
                  name="chartType"
                  label="图表类型"
                  rules={[{ required: true, message: '请选择图表类型' }]}
                >
                  <Select
                    options={[
                      { label: '折线图', value: '折线图' },
                      { label: '柱状图', value: '柱状图' },
                      { label: '饼图', value: '饼图' },
                    ]}
                  />
                </Form.Item>

                <Form.Item
                  name="file"
                  label="CSV数据"
                  rules={[{ required: true, message: '请上传CSV文件' }]}
                >
                  <Upload
                    name="file"
                    maxCount={1}
                    onChange={({ fileList }) => handleFileChange(fileList as UploadFileItem[])}
                    accept=".csv"
                    beforeUpload={(file) => {
                      if (!file.name.endsWith('.csv')) {
                        setErrorMsg("只能上传CSV格式文件！");
                        return false;
                      }
                      return true;
                    }}
                  >
                    <Button icon={<UploadOutlined />}>上传CSV文件</Button>
                  </Upload>
                </Form.Item>

                <Form.Item wrapperCol={{ span: 12, offset: 6 }}>
                  <Space>
                    <Button type="primary" htmlType="submit" loading={submitting}>
                      生成图表
                    </Button>
                    <Button
                      htmlType="reset"
                      onClick={() => {
                        setUploadFile(null);
                        setErrorMsg("");
                      }}
                    >
                      重置
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </Card>

          </div>

        </Col>

        {/* 右侧结果展示 */}
        <Col span={12}>
          <Card title="分析结论" style={{ marginBottom: 16 }}>
            <div style={{ minHeight: 100, lineHeight: 1.6 }}>
              <Spin spinning={submitting} />
              {!submitting && genResult ? (
                <div style={{ whiteSpace: 'pre-wrap' }}>{genResult}</div>
              ) : !submitting && !genResult ? (
                <div style={{ color: '#999', textAlign: 'center', lineHeight: '100px' }}>
                  生成后展示分析结论
                </div>
              ) : null}
            </div>
          </Card>

          <Card title="可视化图表">
            <div style={{ height: 200, width: '100%' }}>
              <Spin spinning={submitting} />
              {!submitting && chartOption ? (
                <ReactEcharts
                  option={chartOption}
                  style={{ height: '100%', width: '100%' }}
                  opts={{ renderer: 'canvas' }}
                />
              ) : !submitting && !chartOption ? (
                <div style={{ color: '#999', textAlign: 'center', lineHeight: '400px' }}>
                  点击生成图表后展示
                </div>
              ) : null}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AddChart;

import { UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Select, Space, Upload } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import {genChartAiAsyncMqUsingPost, genChartAiAsyncUsingPost} from "@/services/swagger/chartController";
import TextArea from "antd/es/input/TextArea";
import * as echarts from 'echarts/core';
import { LineChart, BarChart, PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { useModel } from '@umijs/max';

echarts.use([TitleComponent, TooltipComponent, GridComponent, LegendComponent, LineChart, BarChart, PieChart, CanvasRenderer]);

const AddChart: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const userId = currentUser?.id + '' || '1';

  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [file, setFile] = useState<File | null>(null);
  const [errorMsg, setErrorMsg] = useState<string>("");
  const [chartId, setChartId] = useState<string>("");
  const [wsStatus, setWsStatus] = useState<string>("");

  // ========== 核心修复：持久化WebSocket连接 ==========
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!userId) return;

    // 关闭旧连接
    if (wsRef.current) {
      wsRef.current.close();
    }

    const wsUrl = `ws://localhost:8101/api/websocket/chart/${userId}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsStatus("✅ 已连接服务器");
      console.log("WebSocket连接成功");
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log("收到WebSocket消息：", data); // 新增日志

        // 修复：只要是当前用户的消息就接收（不管chartId是否匹配）
        if (data.chartId && chartId && data.chartId !== chartId) return;

        switch (data.status) {
          case 'connected':
            setWsStatus("✅ " + data.execMessage);
            break;
          case 'wait':
            setWsStatus("⏳ 任务排队中...");
            break;
          case 'running':
            setWsStatus("🔄 AI 生成中...");
            break;
          case 'succeed':
          case 'success':
            setWsStatus("✅ 生成成功！" + (data.execMessage || ""));
            setSubmitting(false);
            break;
          case 'failed':
            setWsStatus("❌ 生成失败：" + (data.execMessage || "未知错误"));
            setErrorMsg(data.execMessage || "生成失败");
            setSubmitting(false);
            break;
          default:
            setWsStatus("📶 " + (data.execMessage || "处理中..."));
            break;
        }
      } catch (e) {
        console.error("解析WebSocket消息失败：", e);
      }
    };

    ws.onerror = (error) => {
      setWsStatus("⚠️ 状态服务连接异常，不影响生成");
      console.error("WebSocket错误：", error);
    };

    ws.onclose = (event) => {
      console.log("WebSocket关闭：", event);
      // 自动重连
      if (event.code !== 1000) {
        setTimeout(() => {
          window.location.reload(); // 简单重连策略
        }, 3000);
      }
    };

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [userId]); // 只依赖userId，不依赖chartId

  // ========== 提交生成 ==========
  const onFinish = async (values: any) => {
    if (!file) {
      setErrorMsg("请上传 Excel 文件");
      return;
    }

    setSubmitting(true);
    setErrorMsg("");
    setWsStatus("📤 提交任务中...");

    try {
      const params = {
        goal: values.goal,
        name: values.name,
        chartType: values.chartType,
      };

      const res = await genChartAiAsyncMqUsingPost(params, {}, file);

      if (res?.data) {
        setChartId(res.data.chartId + "");
        console.log("获取到chartId：", res.data.chartId);
      }
    } catch (e: any) {
      setErrorMsg("提交失败：" + e?.message);
      setWsStatus("提交失败");
      setSubmitting(false);
    }
  };

  // 自动判断状态类型
  const getAlertType = () => {
    if (wsStatus.includes("失败") || wsStatus.includes("异常")) return "error";
    if (wsStatus.includes("成功")) return "success";
    return "info";
  };

  return (
    <div style={{ padding: 20 }}>
      <Card title="智能 BI 分析">

        {wsStatus && (
          <Alert
            message={wsStatus}
            type={getAlertType()}
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        {errorMsg && !wsStatus.includes("成功") && (
          <Alert message={errorMsg} type="error" showIcon style={{ marginBottom: 16 }} />
        )}

        <Form
          form={form}
          labelCol={{ span: 4 }}
          wrapperCol={{ span: 16 }}
          onFinish={onFinish}
          initialValues={{ chartType: '折线图' }}
        >
          <Form.Item name="goal" label="分析目标" rules={[{ required: true }]}>
            <TextArea rows={4} placeholder="请输入你的分析需求，例如：分析用户增长趋势" />
          </Form.Item>

          <Form.Item name="name" label="图表名称">
            <Input placeholder="给图表起个名字" />
          </Form.Item>

          <Form.Item name="chartType" label="图表类型" rules={[{ required: true }]}>
            <Select
              options={[
                { label: '折线图', value: '折线图' },
                { label: '柱状图', value: '柱状图' },
                { label: '饼图', value: '饼图' },
              ]}
            />
          </Form.Item>

          <Form.Item label="上传文件" required>
            <Upload
              maxCount={1}
              accept=".xlsx,.xls"
              beforeUpload={(file) => {
                setFile(file);
                return false;
              }}
            >
              <Button icon={<UploadOutlined />}>选择 Excel 文件</Button>
            </Upload>
          </Form.Item>

          <Form.Item wrapperCol={{ offset: 4 }}>
            <Space>
              <Button type="primary" htmlType="submit" loading={submitting}>
                生成图表
              </Button>
              <Button
                onClick={() => {
                  form.resetFields();
                  setFile(null);
                  setErrorMsg("");
                  setWsStatus("");
                  setChartId("");
                  setSubmitting(false);
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default AddChart;

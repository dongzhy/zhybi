import React, { useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Upload, Button, Card, message, Space, Alert } from 'antd';
import { UploadOutlined, FileAddOutlined } from '@ant-design/icons';
import type { UploadFile, RcFile } from 'antd/es/upload/interface';
import { request } from '@umijs/max';
import ChartRenderer from '@/components/ChartRenderer';
import TextArea from 'antd/es/input/TextArea';

const SmartAnalysis: React.FC = () => {
  // 文件列表
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  // 用户自定义分析目标
  const [goal, setGoal] = useState<string>('综合分析这些数据，找出规律并可视化');
  // AI返回原始结果
  const [aiRawResult, setAiRawResult] = useState<string>('');
  const [loading, setLoading] = useState(false);

  // 限制：最多5个文件
  const MAX_FILE_COUNT = 5;

  // 上传前校验格式
  const beforeUpload = (file: RcFile) => {
    const allowSuffix = ['.xlsx', '.xls', '.docx', '.doc', '.pdf', '.pptx', '.ppt'];
    const fileName = file.name.toLowerCase();
    const isAllow = allowSuffix.some(suf => fileName.endsWith(suf));
    if (!isAllow) {
      message.error('仅支持 xlsx/xls/doc/docx/pdf/pptx/ppt 文件');
      return false;
    }
    // 手动控制上传，不自动请求
    return false;
  };

  // 选中文件变化
  const handleFileChange = ({ fileList: newList }: { fileList: UploadFile[] }) => {
    if (newList.length > MAX_FILE_COUNT) {
      message.warning(`最多上传${MAX_FILE_COUNT}个文件`);
      return;
    }
    setFileList(newList);
  };

  // 提交分析
  const handleAnalyze = async () => {
    if (fileList.length === 0) {
      message.warning('请先上传文件');
      return;
    }
    setLoading(true);
    const formData = new FormData();
    // 遍历塞入文件
    fileList.forEach(item => {
      formData.append('files', item.originFileObj as RcFile);
    });
    formData.append('goal', goal);

    try {
      const res = await request('api/smart/multi-analyze', {
        method: 'POST',
        data: formData,
        requestType: 'form',
      });
      if (res.code === 0) {
        setAiRawResult(res.data);
        message.success('AI分析完成');
      } else {
        message.error(res.message || '分析失败');
      }
    } catch (err) {
      message.error('接口请求异常');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // 清空全部
  const handleClear = () => {
    setFileList([]);
    setAiRawResult('');
    setGoal('综合分析这些数据，找出规律并可视化');
  };

  return (
    <PageContainer title="智能多文件数据分析">
      <Card title="1. 上传文件 & 设置分析需求" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert message="支持格式：Excel/Word/PDF/PPT，单次最多5个文件" type="info" showIcon />
          <Upload
            multiple
            fileList={fileList}
            beforeUpload={beforeUpload}
            onChange={handleFileChange}
            maxCount={MAX_FILE_COUNT}
          >
            <Button icon={<UploadOutlined />}>选择本地文件</Button>
          </Upload>

          <div>
            <p style={{ marginBottom: 6 }}>自定义分析目标（为空使用默认需求）：</p>
            <TextArea
              value={goal}
              onChange={e => setGoal(e.target.value)}
              rows={3}
              placeholder="例如：统计月度销量、对比各部门营收、分析异常数据"
            />
          </div>

          <Space>
            <Button type="primary" onClick={handleAnalyze} loading={loading} icon={<FileAddOutlined />}>
              开始AI智能分析
            </Button>
            <Button onClick={handleClear}>清空全部</Button>
          </Space>
        </Space>
      </Card>

      {/* 复用你的图表渲染组件，自动解析genChart/genResult */}
      {aiRawResult && <ChartRenderer aiResult={aiRawResult} />}
    </PageContainer>
  );
};

export default SmartAnalysis;

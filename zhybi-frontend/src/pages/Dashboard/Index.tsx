import { Card, Col, Row, Statistic, Typography, List, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import {
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import ReactEcharts from 'echarts-for-react';
import { getLast7DaysStatisticsUsingGet, listMyChartVoByPageUsingPost } from '@/services/swagger/chartController';
import dayjs from 'dayjs';
import { request } from 'umi';

const { Title } = Typography;

// 🔥 前端去重工具函数（兜底保障）
const deduplicateType = (type: string) => {
  if (!type) return '未知';
  // 处理"折线图,折线图"这种重复标签
  const types = type.split(',').map(t => t.trim());
  // 去重后取第一个有效类型
  const uniqueTypes = [...new Set(types)].filter(t => t);
  return uniqueTypes.length > 0 ? uniqueTypes[0] : '未知';
};

const Dashboard: React.FC = () => {
  // 最近图表
  const [recentList, setRecentList] = useState<API.Chart[]>([]);
  // 统计数据
  const [total, setTotal] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  // 近7天数据
  const [sevenDaysData, setSevenDaysData] = useState<{ dates: string[]; counts: number[] }>({
    dates: [],
    counts: [],
  });

  // 图表类型真实统计
  const [typeData, setTypeData] = useState<any[]>([]);

  // 加载近7天数据
  const load7DaysData = async () => {
    try {
      const res = await getLast7DaysStatisticsUsingGet();
      if (res.code === 0) {
        // @ts-ignore
        setSevenDaysData(res.data);
      }
    } catch (e) {
      console.error('加载近7天数据失败', e);
    }
  };

  // 加载图表类型分布（真实统计+前端去重兜底）
  const loadTypeData = async () => {
    try {
      const res = await request('/api/chart/dashboard/type');
      if (res.code === 0) {
        let list = res.data || [];
        // 🔥 前端去重+合并，彻底解决重复标签
        const typeMap: Record<string, number> = {};
        list.forEach((item: any) => {
          const rawType = item.chartType || '未知';
          const cleanType = deduplicateType(rawType);
          const count = parseInt(item.count) || 0;
          typeMap[cleanType] = (typeMap[cleanType] || 0) + count;
        });
        // 转换为饼图数据
        const pieData = Object.entries(typeMap).map(([name, value]) => ({
          name,
          value,
        }));
        setTypeData(pieData);
      }
    } catch (e) {
      console.error('加载图表类型失败', e);
    }
  };

  // 加载主数据
  const loadData = async () => {
    setLoading(true);
    try {
      const res = await listMyChartVoByPageUsingPost({
        current: 1,
        pageSize: 5,
        sortField: 'createTime',
        sortOrder: 'desc',
      });
      const list = res?.data?.records || [];
      const totalNum = res?.data?.total || 0;
      setRecentList(list);
      setTotal(Number(totalNum) || 0);

      await load7DaysData();
      await loadTypeData(); // 加载类型分布
    } catch (e) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 图表状态标签
  const getStatusTag = (status?: string) => {
    switch (status) {
      case 'success':
        return <Tag color="success" icon={<CheckCircleOutlined />}>生成成功</Tag>;
      case 'running':
        return <Tag color="processing" icon={<ClockCircleOutlined />}>生成中</Tag>;
      case 'wait':
        return <Tag color="warning">排队中</Tag>;
      case 'failed':
        return <Tag color="error" icon={<ExclamationCircleOutlined />}>生成失败</Tag>;
      default:
        return <Tag>未知</Tag>;
    }
  };

  // 近7天趋势图
  const countOption = {
    grid: { top: 10, right: 10, bottom: 20, left: 30 },
    xAxis: {
      type: 'category',
      data: sevenDaysData.dates.length
        ? sevenDaysData.dates
        : ['6天前', '5天前', '4天前', '3天前', '2天前', '1天前', '今天'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: sevenDaysData.counts.length ? sevenDaysData.counts : [0, 0, 0, 0, 0, 0, 0],
        itemStyle: { color: '#1677ff' },
      },
    ],
  };

  // 图表类型分布（真实数据+美化标签）
  const typeOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)' // 显示名称、数量、百分比
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        type: 'pie',
        radius: '65%',
        data: typeData.length ? typeData : [
          { name: '折线图', value: 0 },
          { name: '柱状图', value: 0 },
          { name: '饼图', value: 0 },
        ],
        label: {
          formatter: '{b}: {c}个' // 标签显示：类型+数量
        }
      },
    ],
  };

  return (
    <div style={{ padding: '24px' }}>
      <Title level={4} style={{ marginBottom: 24 }}>📊 数据概览仪表盘</Title>

      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="我的图表总数"
              value={total}
              prefix={<BarChartOutlined />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="最近生成"
              value={recentList.length}
              prefix={<LineChartOutlined />}
              valueStyle={{ color: '#36cfa0' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="成功图表"
              value={recentList.filter((i) => i.status === 'success').length}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="生成中/排队"
              value={recentList.filter((i) => i.status === 'running' || i.status === 'wait').length}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 图表分析 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card title="近7天生成趋势" style={{ marginBottom: 24 }}>
            <ReactEcharts option={countOption} style={{ height: 280 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="图表类型分布" style={{ marginBottom: 24 }}>
            <ReactEcharts option={typeOption} style={{ height: 280 }} />
          </Card>
        </Col>
      </Row>

      {/* 最近使用 */}
      <Card title="最近生成的图表" loading={loading}>
        <List
          size="small"
          dataSource={recentList}
          renderItem={(item) => (
            <List.Item
              actions={[
                <span>{dayjs(item.updateTime).format('MM-DD HH:mm')}</span>,
                getStatusTag(item.status),
              ]}
            >
              <List.Item.Meta
                title={item.name || '未命名图表'}
                description={`类型：${deduplicateType(item.chartType || '未知')}`}
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};

export default Dashboard;

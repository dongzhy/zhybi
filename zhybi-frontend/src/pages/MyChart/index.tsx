import {
  Input, message, Result, Spin, Button, Space, Popconfirm, Row, Col, Grid, Radio, Tag
} from 'antd';
import { StarOutlined, StarFilled, VerticalAlignTopOutlined, UndoOutlined, WarningOutlined } from '@ant-design/icons';
import React, { useEffect, useState, useRef } from 'react';
import {
  listMyChartVoByPageUsingPost,
  deleteChartUsingPost,
  listRecycleChartUsingPost,
  toggleStarUsingPost,
  toggleTopUsingPost,
  restoreRecycleChartUsingPost,
  realDeleteChartUsingPost
} from "@/services/swagger/chartController";
import ReactEcharts from "echarts-for-react";
import { useModel } from "@umijs/max";
import { SearchOutlined, DeleteOutlined, DownloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import * as echarts from 'echarts';
import type { API } from '@/services/swagger/typings.d.ts';

const { useBreakpoint } = Grid;

type ChartSearchParams = API.ChartQueryRequest;
type ViewType = 'all' | 'star' | 'recycle';

// 提取到组件外部，避免每次渲染重新创建
const initSearchParams: ChartSearchParams = {
  current: 1,
  pageSize: 6,
  name: '',
  sortField: 'createTime',
  sortOrder: 'desc',
};

const MyChartPage: React.FC = () => {
  // 视图类型：全部 / 仅收藏 / 回收站
  const [viewType, setViewType] = useState<ViewType>('all');

  const [searchParams, setSearchParams] = useState<ChartSearchParams>({ ...initSearchParams });
  const [searchInputValue, setSearchInputValue] = useState<string>('');
  const [chartList, setChartList] = useState<API.Chart[]>([]);
  const [total, setTotal] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);

  const chartRefMap = useRef<Record<number, ReactEcharts | null>>({});

  const { initialState } = useModel('@@initialState');
  const { currentUser = {} } = initialState ?? {};
  const screens = useBreakpoint();

  // 解析 ECharts Option
  const extractEchartsOption = (rawGenChartStr: string) => {
    if (!rawGenChartStr || !rawGenChartStr.trim()) return null;
    try {
      let code = rawGenChartStr.trim();
      code = code.replace(/\n|\t|\r/g, ' ');
      code = code.replace(/^option\s*=\s*/i, '');
      code = code.replace(/;+\s*$/, '');
      const option = new Function('return ' + code)();
      if (!option || !option.series) return null;
      return option;
    } catch (err) {
      console.error('图表解析失败：', err);
      return null;
    }
  };

  // 下载图表为图片
  const downloadChartImage = (chartId: number, chartName: string) => {
    const reactEchart = chartRefMap.current[chartId];
    if (!reactEchart) {
      message.warning('图表尚未渲染完成，无法下载');
      return;
    }
    const instance = reactEchart.getEchartsInstance();
    const imgBase64 = instance.getDataURL({
      pixelRatio: 2,
      backgroundColor: '#fff'
    });

    const a = document.createElement('a');
    a.href = imgBase64;
    a.download = `${chartName || 'chart'}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    message.success('图表图片已下载');
  };

  // 加载数据，区分视图
  const loadData = async () => {
    setLoading(true);
    try {
      let res;
      if (viewType === 'recycle') {
        // 回收站接口
        res = await listRecycleChartUsingPost({
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          name: searchParams.name,
        });
      } else {
        // 全部 / 收藏
        const query: ChartSearchParams = {
          ...searchParams,
          isStar: viewType === 'star' ? 1 : undefined
        };
        res = await listMyChartVoByPageUsingPost(query);
      }

      const data = res?.data;
      setChartList(data?.records ?? []);
      setTotal(Number(data?.total) || 0);
    } catch (e: any) {
      console.error('loadData error', e);
      message.error('加载失败：' + e.message);
    } finally {
      setLoading(false);
    }
  };

  // 切换视图，重置分页
  const handleViewChange = (val: ViewType) => {
    setViewType(val);
    setSearchParams(prev => ({
      ...prev,
      current: 1
    }));
  };

  // 移入回收站（逻辑删除）
  const handleMoveRecycle = async (id: number) => {
    try {
      await deleteChartUsingPost({ id });
      message.success('已移入回收站');
      loadData();
    } catch (e: any) {
      console.error('delete error', e);
      message.error('删除失败：' + e.message);
    }
  };

  // 切换收藏
  const handleToggleStar = async (record: API.Chart) => {
    try {
      await toggleStarUsingPost({ id: record.id });
      message.success(record.isStar ? '取消收藏成功' : '收藏成功');
      loadData();
    } catch (e: any) {
      message.error(e.message || '操作收藏失败');
    }
  };

  // 切换置顶
  const handleToggleTop = async (record: API.Chart) => {
    try {
      await toggleTopUsingPost({ id: record.id });
      message.success(record.isTop ? '取消置顶成功' : '置顶成功');
      loadData();
    } catch (e: any) {
      message.error(e.message || '操作置顶失败');
    }
  };

  // 恢复回收站图表
  const handleRestore = async (id: number) => {
    try {
      await restoreRecycleChartUsingPost({ id });
      message.success('图表已恢复');
      loadData();
    } catch (e: any) {
      message.error(e.message || '恢复失败');
    }
  };

  // 彻底物理删除
  const handleRealDelete = async (id: number) => {
    try {
      await realDeleteChartUsingPost({ id });
      message.success('彻底删除成功，不可恢复');
      loadData();
    } catch (e: any) {
      message.error(e.message || '彻底删除失败');
    }
  };

  // searchParams / viewType 变化自动重新请求
  useEffect(() => {
    loadData();
  }, [searchParams, viewType]);

  // 搜索按钮触发
  const handleSearch = () => {
    setSearchParams(prev => ({
      ...prev,
      name: searchInputValue.trim(),
      current: 1,
    }));
  };

  // 分页切换
  const handlePageChange = (page: number) => {
    setSearchParams(prev => ({ ...prev, current: page }));
  };

  return (
    <div style={{ padding: '24px' }}>
      <h2 style={{ marginBottom: 20 }}>我的图表管理</h2>

      {/* 视图切换 + 搜索栏 */}
      <div style={{
        marginBottom: 20,
        display: 'flex',
        flexWrap: 'wrap',
        gap: 12,
        alignItems: 'center'
      }}>
        <Radio.Group value={viewType} onChange={(e) => handleViewChange(e.target.value)}>
          <Radio value="all">全部图表</Radio>
          <Radio value="star">仅收藏</Radio>
          <Radio value="recycle">回收站</Radio>
        </Radio.Group>

        <div style={{ display: 'flex', gap: 12 }}>
          <Input
            placeholder="搜索图表名称"
            value={searchInputValue}
            onChange={(e) => setSearchInputValue(e.target.value)}
            onPressEnter={handleSearch}
            prefix={<SearchOutlined />}
            allowClear
            style={{ width: 260, height: 40 }}
          />
          <Button type="primary" onClick={handleSearch} style={{ height: 40 }}>
            搜索
          </Button>
        </div>
      </div>

      <Spin spinning={loading} tip="加载中...">
        <Row gutter={[24, 24]}>
          {chartList.map((item) => {
            const option = extractEchartsOption(item.genChart || '');
            const isRecycleView = viewType === 'recycle';
            return (
              <Col key={item.id} xs={24} md={12}>
                <div style={{
                  padding: '16px',
                  borderRadius: 8,
                  border: '1px solid #f0f0f0',
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column'
                }}>
                  {/* 操作按钮行 */}
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
                    {!isRecycleView && (
                      <>
                        <Button
                          icon={item.isStar ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
                          onClick={() => handleToggleStar(item)}
                        >
                          {item.isStar ? '已收藏' : '收藏'}
                        </Button>
                        <Button
                          icon={<VerticalAlignTopOutlined />}
                          onClick={() => handleToggleTop(item)}
                        >
                          {item.isTop ? '取消置顶' : '置顶'}
                        </Button>
                      </>
                    )}

                    {item.status === 'success' && (
                      <Button
                        icon={<DownloadOutlined />}
                        onClick={() => downloadChartImage(item.id!, item.name || '未命名图表')}
                      >
                        下载图片
                      </Button>
                    )}

                    {!isRecycleView ? (
                      <Popconfirm
                        title="确定移入回收站？"
                        onConfirm={() => handleMoveRecycle(item.id!)}
                        okText="确定"
                        cancelText="取消"
                      >
                        <Button danger icon={<DeleteOutlined />}>移入回收站</Button>
                      </Popconfirm>
                    ) : (
                      <>
                        <Button icon={<UndoOutlined />} onClick={() => handleRestore(item.id!)}>
                          恢复图表
                        </Button>
                        <Popconfirm
                          title="警告：彻底删除后数据无法恢复！"
                          icon={<WarningOutlined style={{ color: 'red' }} />}
                          onConfirm={() => handleRealDelete(item.id!)}
                          okText="确认彻底删除"
                          cancelText="取消"
                        >
                          <Button danger icon={<DeleteOutlined />}>彻底删除</Button>
                        </Popconfirm>
                      </>
                    )}
                  </div>

                  {/* 图表区域 */}
                  {item.status === 'success' ? (
                    option ? (
                      <ReactEcharts
                        ref={(el) => {
                          if (item.id) {
                            // 修复：当组件卸载时 el 为 null，需同步清空 ref 防止内存泄漏
                            chartRefMap.current[item.id] = el;
                          }
                        }}
                        echarts={echarts}
                        option={option}
                        style={{ height: 320, flexShrink: 0 }}
                      />
                    ) : (
                      <div style={{
                        height: 320,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background: '#fafafa',
                        borderRadius: 8,
                        color: '#999',
                        flexShrink: 0
                      }}>
                        图表配置解析失败，无法显示
                      </div>
                    )
                  ) : null}

                  {/* 状态提示 */}
                  {item.status === 'wait' && (
                    <Result status="warning" title="等待生成" subTitle="排队中" style={{ padding: '20px 0' }} />
                  )}
                  {item.status === 'running' && (
                    <Result status="info" title="生成中" subTitle="AI 分析中..." style={{ padding: '20px 0' }} />
                  )}
                  {item.status === 'failed' && (
                    <Result status="error" title="生成失败" subTitle={item.execMessage} style={{ padding: '20px 0' }} />
                  )}

                  {/* 图表信息 */}
                  <div style={{ marginTop: 'auto', paddingTop: 12 }}>
                    <Space wrap>
                      <strong>{item.name || '未命名图表'}</strong>
                      <span>类型：{item.chartType}</span>
                      <span>状态：{item.status}</span>
                      {/* 修复：之前未导入 Tag 组件导致此处报错 */}
                      {!isRecycleView && item.isTop === 1 && <Tag color="gold">已置顶</Tag>}
                      {!isRecycleView && item.isStar === 1 && <Tag color="#faad14">已收藏</Tag>}
                    </Space>
                    <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>
                      更新时间：{dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss')}
                    </div>
                    {item.status === 'success' && item.genResult && (
                      <div style={{ marginTop: 8, fontSize: 13, color: '#444' }}>
                        <strong>分析结论：</strong>{item.genResult}
                      </div>
                    )}
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>

        {/* 空数据提示：移出 Row 外部或包裹在 Col 中，保证栅格布局正确 */}
        {chartList.length === 0 && !loading && (
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
            {viewType === 'recycle' ? '回收站暂无图表' : viewType === 'star' ? '暂无收藏图表' : '暂无图表数据'}
          </div>
        )}

        {/* 分页器 */}
        <div style={{ marginTop: 32, textAlign: 'center' }}>
          <Space>
            <Button disabled={searchParams.current === 1} onClick={() => handlePageChange(Number(searchParams.current) - 1)}>上一页</Button>
            <span>第 {searchParams.current} 页 / 共 {Math.ceil(total / (searchParams.pageSize || 6))} 页，总计 {total} 条</span>
            <Button disabled={Number(searchParams.current) * Number(searchParams.pageSize) >= total} onClick={() => handlePageChange(Number(searchParams.current) + 1)}>下一页</Button>
          </Space>
        </div>
      </Spin>
    </div>
  );
};

export default MyChartPage;

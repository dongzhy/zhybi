import React, { useState, useEffect, useRef } from 'react';
import {
  Button, Modal, Input, message, Space, Card, Typography,
  Dropdown, Tooltip
} from 'antd';
import {
  SaveOutlined, ExportOutlined, ImportOutlined, FullscreenOutlined,
  ReloadOutlined, PlusOutlined, DeleteOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import GridLayout, { WidthProvider, Layout } from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import { request } from '@umijs/max';

const ResponsiveGridLayout = WidthProvider(GridLayout);

type ChartType = 'pie' | 'bar' | 'line';

interface ChartItemDTO {
  itemKey: string;
  chartId: number | null;
  chartTitle: string;
  chartType: string;
  genChart: string;
  x: number;
  y: number;
  w: number;
  h: number;
  minW: number;
  minH: number;
}

interface BoardDTO {
  boardId: number | null;
  boardName: string;
  isShared: number;
  chartList: ChartItemDTO[];
}

interface BoardListItem {
  id: number;
  boardName: string;
  userId: number;
  isShared: number;
  createTime: string;
  updateTime: string;
}

interface AiChartSimple {
  id: number;
  name: string;
  goal: string;
}

const initEmptyBoard: BoardDTO = {
  boardId: null,
  boardName: '未命名大屏',
  isShared: 0,
  chartList: [],
};

/** 🔧 安全提取响应数据 */
function extractData<T>(res: any): T {
  if (res == null) return [] as unknown as T;
  if (Array.isArray(res)) return res as T;
  if (res.data !== undefined) return res.data as T;
  return res as T;
}

/**
 * ★★★ 核心：自动寻找下一个可用槽位，优先横向填充（一行放2个） ★★★
 *
 * 算法：从上到下逐行扫描，在每行中从左到右尝试放置，
 * 如果当前行有空间就放进去（横向排列），否则放到下一行。
 */
const findNextPosition = (
  chartList: ChartItemDTO[],
  newW: number,
  newH: number,
  cols: number = 12,
): { x: number; y: number } => {
  // 如果画布为空，直接放在左上角
  if (chartList.length === 0) {
    return { x: 0, y: 0 };
  }

  // 构建占用格子集合
  const occupied = new Set<string>();
  let maxY = 0;

  chartList.forEach((c) => {
    for (let dx = 0; dx < c.w; dx++) {
      for (let dy = 0; dy < c.h; dy++) {
        occupied.add(`${c.x + dx},${c.y + dy}`);
      }
    }
    maxY = Math.max(maxY, c.y + c.h);
  });

  // 从 y=0 开始逐行扫描，寻找第一个能放下的位置
  for (let y = 0; y <= maxY; y++) {
    for (let x = 0; x <= cols - newW; x++) {
      let fits = true;
      // 检查 (x, y) 开始的 newW × newH 区域是否全部空闲
      for (let dx = 0; dx < newW && fits; dx++) {
        for (let dy = 0; dy < newH && fits; dy++) {
          if (occupied.has(`${x + dx},${y + dy}`)) {
            fits = false;
          }
        }
      }
      if (fits) {
        return { x, y };
      }
    }
  }

  // 现有行都放不下，放到最底部左对齐
  return { x: 0, y: maxY };
};

const DataScreen: React.FC = () => {
  const [currentBoard, setCurrentBoard] = useState<BoardDTO>({ ...initEmptyBoard });
  const [boardList, setBoardList] = useState<BoardListItem[]>([]);
  const [aiChartList, setAiChartList] = useState<AiChartSimple[]>([]);
  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [showBoardModal, setShowBoardModal] = useState(false);
  const [showAiChartModal, setShowAiChartModal] = useState(false);

  useEffect(() => {
    loadBoardList();
    loadAiChartList();
  }, []);

  // ==================== 数据加载 ====================

  const loadBoardList = async () => {
    try {
      const res = await request('/api/board/list');
      const list = extractData<BoardListItem[]>(res);
      setBoardList(Array.isArray(list) ? list : []);
    } catch (err) {
      console.error('加载看板列表失败:', err);
      message.error('加载看板列表失败');
    }
  };

  const loadAiChartList = async () => {
    try {
      const res = await request('/api/chart/list');
      const list = extractData<AiChartSimple[]>(res);
      setAiChartList(Array.isArray(list) ? list : []);
    } catch (err) {
      console.error('获取AI图表列表失败:', err);
      message.warning('获取AI图表列表失败');
    }
  };

  const handleSwitchBoard = async (boardId: number) => {
    try {
      const res = await request(`/api/board/${boardId}`);
      const dto = extractData<BoardDTO>(res);
      if (dto) {
        setCurrentBoard(dto);
        message.success('大屏加载成功');
      }
    } catch (err) {
      message.error('加载大屏失败');
    }
  };

  // ==================== 布局与图表操作 ====================

  const handleLayoutChange = (newLayout: Layout[]) => {
    const newChartList = currentBoard.chartList.map((chartDto) => {
      const match = newLayout.find((l) => l.i === chartDto.itemKey);
      if (!match) return chartDto;
      return { ...chartDto, x: match.x, y: match.y, w: match.w, h: match.h };
    });
    setCurrentBoard((prev) => ({ ...prev, chartList: newChartList }));
  };

  const handleAddStaticChart = (type: ChartType) => {
    const newKey = `chart_${Date.now()}`;
    const opt = getChartDefaultOption(type);

    // ★ 使用智能定位，w=6 恰好占12列的一半，自动一行放2个
    const pos = findNextPosition(currentBoard.chartList, 6, 8);

    const newChartDto: ChartItemDTO = {
      itemKey: newKey,
      chartId: null,
      chartTitle: `静态${type}图表`,
      chartType: type,
      genChart: JSON.stringify(opt),
      x: pos.x,
      y: pos.y,
      w: 6,
      h: 8,
      minW: 4,
      minH: 6,
    };
    setCurrentBoard((prev) => ({
      ...prev,
      chartList: [...prev.chartList, newChartDto],
    }));
    message.success('静态图表添加成功');
  };

  const handleAddAiChart = async (aiChartId: number) => {
    if (!aiChartId) return;
    try {
      const res = await request(`/api/smart/get/${aiChartId}`);
      const chartInfo = extractData<any>(res);
      if (!chartInfo) {
        message.error('未获取到图表数据');
        return;
      }
      const genChartStr =
        typeof chartInfo.genChart === 'string'
          ? chartInfo.genChart
          : JSON.stringify(chartInfo.genChart);

      const newKey = `chart_${Date.now()}`;

      // ★ 使用智能定位
      const pos = findNextPosition(currentBoard.chartList, 6, 8);

      const newChartDto: ChartItemDTO = {
        itemKey: newKey,
        chartId: aiChartId,
        chartTitle: chartInfo.name || 'AI分析图表',
        chartType: chartInfo.chartType || 'bar',
        genChart: genChartStr,
        x: pos.x,
        y: pos.y,
        w: 6,
        h: 8,
        minW: 3,
        minH: 6,
      };
      setCurrentBoard((prev) => ({
        ...prev,
        chartList: [...prev.chartList, newChartDto],
      }));
      message.success('AI图表已添加至大屏');
    } catch (err) {
      console.error('获取AI图表配置失败:', err);
      message.error('获取AI图表配置失败');
    }
  };

  const handleDeleteChart = (itemKey: string) => {
    setCurrentBoard((prev) => ({
      ...prev,
      chartList: prev.chartList.filter((c) => c.itemKey !== itemKey),
    }));
    message.info('图表已移除');
  };

  // ==================== 保存看板 ====================

  const handleSaveBoard = async () => {
    if (!currentBoard.boardName?.trim()) {
      message.warning('请填写大屏名称');
      return;
    }
    try {
      const res = await request('/api/board/save', {
        method: 'POST',
        data: currentBoard,
      });
      const boardId = extractData<number>(res);
      if (boardId) {
        setCurrentBoard((prev) => ({ ...prev, boardId }));
      }
      setSaveModalVisible(false);
      message.success('大屏保存成功');
      loadBoardList();
    } catch (err) {
      console.error('保存异常', err);
      message.error('保存大屏失败');
    }
  };

  // ==================== JSON 导入导出 ====================

  const exportBoardJson = () => {
    const exportData: Partial<BoardDTO> = {
      boardName: currentBoard.boardName,
      chartList: currentBoard.chartList,
    };
    const jsonStr = JSON.stringify(exportData, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${currentBoard.boardName || '大屏配置'}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    message.success('JSON配置文件已导出');
  };

  const triggerFileInput = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
      fileInputRef.current.click();
    }
  };

  const handleImportJson = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const text = ev.target?.result as string;
        const json = JSON.parse(text) as Partial<BoardDTO>;
        if (!Array.isArray(json.chartList)) {
          message.error('JSON文件格式错误，缺少chartList');
          return;
        }
        setCurrentBoard({
          ...initEmptyBoard,
          boardName: json.boardName || '导入大屏',
          chartList: json.chartList,
        });
        message.success('JSON配置导入成功');
      } catch (err) {
        console.error('JSON解析失败', err);
        message.error('文件不是合法JSON');
      }
    };
    reader.readAsText(file);
  };

  // ==================== 其他操作 ====================

  const resetCurrentBoard = () => {
    setCurrentBoard({ ...initEmptyBoard });
    message.info('已重置空白大屏');
  };

  const toggleFullScreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {
        message.warning('浏览器不支持全屏');
      });
      setIsFullScreen(true);
    } else {
      document.exitFullscreen();
      setIsFullScreen(false);
    }
  };

  // ==================== 图表配置 ====================

  const getChartDefaultOption = (type: ChartType) => {
    switch (type) {
      case 'pie':
        return {
          title: { text: '产品销量占比分析', textStyle: { color: '#fff' } },
          tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
          legend: {
            orient: 'vertical',
            left: 'left',
            data: ['电子产品', '服装', '食品'],
            textStyle: { color: '#fff' },
          },
          series: [
            {
              name: '产品销量',
              type: 'pie',
              radius: '50%',
              center: ['50%', '50%'],
              data: [
                { value: 500, name: '电子产品' },
                { value: 300, name: '服装' },
                { value: 800, name: '食品' },
              ],
              itemStyle: {
                emphasis: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0,0,0,0.5)',
                },
              },
            },
          ],
        };
      case 'bar':
        return {
          title: { text: '月度销量统计', textStyle: { color: '#fff' } },
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: ['1月', '2月', '3月', '4月', '5月', '6月'],
            axisLabel: { color: '#ccc' },
          },
          yAxis: { type: 'value', axisLabel: { color: '#ccc' } },
          series: [{ name: '销量', type: 'bar', data: [120, 200, 150, 80, 70, 110] }],
        };
      case 'line':
        return {
          title: { text: '客流量趋势', textStyle: { color: '#fff' } },
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
            axisLabel: { color: '#ccc' },
          },
          yAxis: { type: 'value', axisLabel: { color: '#ccc' } },
          series: [
            { name: '客流', type: 'line', smooth: true, data: [120, 132, 101, 134, 90, 230, 210] },
          ],
        };
      default:
        return {};
    }
  };

  // ==================== 渲染函数 ====================

  const renderChartCard = (chartDto: ChartItemDTO) => {
    let chartOpt = {};
    try {
      chartOpt = JSON.parse(chartDto.genChart);
    } catch (e) {
      console.error('图表JSON解析失败', e);
    }
    return (
      <div key={chartDto.itemKey} className="chart-card">
        <Card
          size="small"
          title={chartDto.chartTitle}
          extra={
            <Tooltip title="删除图表">
              <Button
                danger
                type="text"
                size="small"
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteChart(chartDto.itemKey)}
              />
            </Tooltip>
          }
          styles={{ body: { padding: 8, height: 'calc(100% - 40px)' } }}
        >
          <ReactECharts option={chartOpt} style={{ width: '100%', height: '100%' }} lazyUpdate />
        </Card>
      </div>
    );
  };

  const staticChartMenuItems = [
    { key: 'pie', label: '静态饼图（占比）' },
    { key: 'bar', label: '静态柱状图（统计）' },
    { key: 'line', label: '静态折线图（趋势）' },
  ];

  const handleStaticMenuClick = (e: { key: string }) => {
    handleAddStaticChart(e.key as ChartType);
  };

  const gridLayout: Layout[] = currentBoard.chartList.map((c) => ({
    i: c.itemKey,
    x: c.x,
    y: c.y,
    w: c.w,
    h: c.h,
    minW: c.minW,
    minH: c.minH,
  }));

  return (
    <div style={{ minHeight: '100vh', background: '#0c1836', padding: 16, boxSizing: 'border-box' }}>
      <input
        ref={fileInputRef}
        type="file"
        accept=".json"
        style={{ display: 'none' }}
        onChange={handleImportJson}
      />

      {/* 顶部工具栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'rgba(255,255,255,0.08)',
          padding: '12px 20px',
          borderRadius: 8,
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 8,
          position: 'relative',
          zIndex: 100,
        }}
      >
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          📊 数据可视化大屏
        </Typography.Title>

        <Space size="middle" wrap>
          <Button onClick={() => setShowBoardModal(true)}>
            选择大屏 ({currentBoard.boardName})
          </Button>
          <Button onClick={() => setShowAiChartModal(true)}>
            添加AI图表 ({aiChartList.length}个可用)
          </Button>

          <Dropdown
            menu={{ items: staticChartMenuItems, onClick: handleStaticMenuClick }}
            trigger={['click']}
          >
            <Button icon={<PlusOutlined />}>新增静态图表</Button>
          </Dropdown>

          <Button icon={<SaveOutlined />} onClick={() => setSaveModalVisible(true)}>
            保存大屏
          </Button>

          <Button icon={<ExportOutlined />} onClick={exportBoardJson}>
            导出JSON
          </Button>
          <Button icon={<ImportOutlined />} onClick={triggerFileInput}>
            导入JSON
          </Button>

          <Button icon={<ReloadOutlined />} onClick={resetCurrentBoard}>
            重置大屏
          </Button>
          <Button type="primary" icon={<FullscreenOutlined />} onClick={toggleFullScreen}>
            {isFullScreen ? '退出全屏' : '大屏全屏'}
          </Button>
        </Space>
      </div>

      {/* 拖拽画布 */}
      <ResponsiveGridLayout
        className="layout-container"
        layout={gridLayout}
        onLayoutChange={handleLayoutChange}
        cols={12}
        rowHeight={40}
        margin={[16, 16]}
        draggableHandle=".ant-card-head"
        isResizable
        isDraggable
        style={{ width: '100%' }}
      >
        {currentBoard.chartList.map((item) => renderChartCard(item))}
      </ResponsiveGridLayout>

      {/* 保存弹窗 */}
      <Modal
        open={saveModalVisible}
        title="保存大屏"
        onCancel={() => setSaveModalVisible(false)}
        onOk={handleSaveBoard}
      >
        <Input
          placeholder="输入大屏名称"
          value={currentBoard.boardName}
          onChange={(e) => setCurrentBoard((prev) => ({ ...prev, boardName: e.target.value }))}
        />
      </Modal>

      {/* 看板选择模态框 */}
      <Modal
        title="选择大屏"
        open={showBoardModal}
        onCancel={() => setShowBoardModal(false)}
        footer={null}
        width={500}
        styles={{
          content: { backgroundColor: '#162349', color: '#fff' },
          header: { backgroundColor: '#162349', color: '#fff', borderBottom: '1px solid #333' },
          body: { padding: 0 },
        }}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          {boardList.length > 0 ? (
            boardList.map((board) => (
              <div
                key={board.id}
                onClick={() => {
                  handleSwitchBoard(board.id);
                  setShowBoardModal(false);
                }}
                style={{
                  padding: '12px 16px',
                  borderBottom: '1px solid rgba(255,255,255,0.1)',
                  cursor: 'pointer',
                  color: '#fff',
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLDivElement).style.backgroundColor = 'rgba(255,255,255,0.1)';
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLDivElement).style.backgroundColor = 'transparent';
                }}
              >
                {board.boardName}
              </div>
            ))
          ) : (
            <div style={{ padding: 16, color: '#aaa' }}>暂无大屏</div>
          )}
        </div>
      </Modal>

      {/* AI图表选择模态框 */}
      <Modal
        title="添加AI图表"
        open={showAiChartModal}
        onCancel={() => setShowAiChartModal(false)}
        footer={null}
        width={500}
        styles={{
          content: { backgroundColor: '#162349', color: '#fff' },
          header: { backgroundColor: '#162349', color: '#fff', borderBottom: '1px solid #333' },
          body: { padding: 0 },
        }}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          {aiChartList.length > 0 ? (
            aiChartList.map((chart) => (
              <div
                key={chart.id}
                onClick={() => {
                  handleAddAiChart(chart.id);
                  setShowAiChartModal(false);
                }}
                style={{
                  padding: '12px 16px',
                  borderBottom: '1px solid rgba(255,255,255,0.1)',
                  cursor: 'pointer',
                  color: '#fff',
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLDivElement).style.backgroundColor = 'rgba(255,255,255,0.1)';
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLDivElement).style.backgroundColor = 'transparent';
                }}
              >
                {chart.name}
              </div>
            ))
          ) : (
            <div style={{ padding: 16, color: '#aaa' }}>暂无AI图表</div>
          )}
        </div>
      </Modal>

      <style>{`
        .layout-container { width: 100% !important; }
        .chart-card { height: 100%; }
        .chart-card .ant-card {
          background: rgba(255,255,255,0.05);
          border: 1px solid rgba(66,158,255,0.2);
          border-radius: 8px;
          height: 100%;
        }
        .chart-card .ant-card-head {
          background: rgba(30,60,120,0.4);
          color: #fff;
          border-bottom: 1px solid rgba(66,158,255,0.2);
          cursor: move;
        }
        .chart-card .ant-card-head-title { color: #fff; }
        .react-grid-item.react-grid-placeholder {
          background: rgba(66,158,255,0.3);
          border-radius: 8px;
        }
      `}</style>
    </div>
  );
};

export default DataScreen;

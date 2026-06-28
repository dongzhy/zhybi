import { useState, useEffect, useRef } from 'react'
import { Upload, Button, Checkbox, Space, Input, Card, message, Spin, Table } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import axios from 'axios'
import ReactECharts from 'echarts-for-react'

// 全局开启跨域携带Cookie（Session登录必需）
axios.defaults.withCredentials = true;

const BASE_URL = 'http://localhost:8101/api/smart'

// 类型定义
type FileTablePreview = {
  fileName: string
  headerList: string[]
  rowDataList: any[][]
  totalRows: number
}

type TableSelectConfig = {
  selectColumns: string[]
  selectRowIndexes: number[]
  selectAll: boolean
}

type MultiFileAnalyzeDTO = {
  chartName: string
  goal: string
  selectConfigMap: Record<string, TableSelectConfig>
}

export default function MultiFileAnalyse() {
  const [fileList, setFileList] = useState<any[]>([])
  const [tablePreviewMap, setTablePreviewMap] = useState<Record<string, FileTablePreview>>({})
  const [selectConfigMap, setSelectConfigMap] = useState<Record<string, TableSelectConfig>>({})
  const [chartName, setChartName] = useState('')
  const [goal, setGoal] = useState('')
  const [taskId, setTaskId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [chartData, setChartData] = useState<{ genChart: string; genResult: string; status: string } | null>(null)
  const [pollTimer, setPollTimer] = useState<NodeJS.Timeout | null>(null)

  // 防抖定时器
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null)

  // 组件销毁时清理所有定时器
  useEffect(() => {
    return () => {
      if (pollTimer) clearInterval(pollTimer)
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
    }
  }, [pollTimer])

  // 解析文件并请求预览接口
  const fetchTablePreview = async (currentFileList: any[]) => {
    if (currentFileList.length === 0) {
      setTablePreviewMap({})
      setSelectConfigMap({})
      return
    }

    const formData = new FormData()
    let hasFile = false
    // 遍历所有文件，组装FormData
    currentFileList.forEach(item => {
      if (item.originFileObj) {
        formData.append('files', item.originFileObj)
        hasFile = true
      }
    })

    if (!hasFile) return

    try {
      const res = await axios.post(`${BASE_URL}/previewTable`, formData)
      const list: FileTablePreview[] = res.data.data

      const newPreviewMap: Record<string, FileTablePreview> = {}
      const newSelectConfigMap: Record<string, TableSelectConfig> = {}

      list.forEach(item => {
        newPreviewMap[item.fileName] = item
        newSelectConfigMap[item.fileName] = {
          selectAll: true,
          selectColumns: item.headerList,
          selectRowIndexes: Array.from({ length: item.rowDataList.length }, (_, i) => i + 1)
        }
      })

      setTablePreviewMap(newPreviewMap)
      setSelectConfigMap(newSelectConfigMap)
      message.success('文件解析成功，已默认全选数据')
    } catch (err: any) {
      let errMsg = '文件上传失败'
      if (err.response) {
        const data = err.response.data || {}
        errMsg = data.message || data.msg || `请求错误 ${err.response.status}`
      } else if (err.request) {
        errMsg = '后端服务无响应，请检查后端是否启动'
      } else {
        errMsg = err.message
      }
      message.error(errMsg)
    }
  }

  // 文件选择/删除触发
  const handleFileChange = ({ fileList: newFileList }: any) => {
    setFileList(newFileList)

    // 清除上一次防抖
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    if (newFileList.length === 0) {
      setTablePreviewMap({})
      setSelectConfigMap({})
      return
    }

    // 防抖 300ms 执行请求
    debounceTimerRef.current = setTimeout(() => {
      fetchTablePreview(newFileList)
    }, 300)
  }

  // 整表全选/反选
  const handleSelectAll = (fileName: string, checked: boolean) => {
    const preview = tablePreviewMap[fileName]
    if (!preview) return
    setSelectConfigMap(prev => ({
      ...prev,
      [fileName]: {
        selectAll: checked,
        selectColumns: checked ? preview.headerList : [],
        selectRowIndexes: checked ? Array.from({ length: preview.rowDataList.length }, (_, i) => i + 1) : []
      }
    }))
  }

  // 列勾选切换
  const handleColumnCheckChange = (fileName: string, column: string, checked: boolean) => {
    const config = selectConfigMap[fileName]
    if (!config) return
    const newColumns = checked
      ? [...config.selectColumns, column]
      : config.selectColumns.filter(c => c !== column)

    const allSelected = newColumns.length === tablePreviewMap[fileName].headerList.length
      && config.selectRowIndexes.length === tablePreviewMap[fileName].rowDataList.length

    setSelectConfigMap(prev => ({
      ...prev,
      [fileName]: {
        ...config,
        selectColumns: newColumns,
        selectAll: allSelected
      }
    }))
  }

  // 行勾选切换
  const handleRowCheckChange = (fileName: string, rowIndex: number, checked: boolean) => {
    const config = selectConfigMap[fileName]
    if (!config) return
    const newRows = checked
      ? [...config.selectRowIndexes, rowIndex]
      : config.selectRowIndexes.filter(r => r !== rowIndex)

    const allSelected = newRows.length === tablePreviewMap[fileName].rowDataList.length
      && config.selectColumns.length === tablePreviewMap[fileName].headerList.length

    setSelectConfigMap(prev => ({
      ...prev,
      [fileName]: {
        ...config,
        selectRowIndexes: newRows,
        selectAll: allSelected
      }
    }))
  }

  // 提交AI分析
  const submitAnalyse = async () => {
    if (!chartName.trim()) {
      return message.warning('请填写图表名称')
    }
    if (Object.keys(selectConfigMap).length === 0) {
      return message.warning('请先上传文件并勾选数据')
    }

    const hasValid = Object.values(selectConfigMap).some(c =>
      c.selectAll || c.selectColumns.length > 0 || c.selectRowIndexes.length > 0
    )
    if (!hasValid) {
      return message.warning('请至少勾选一列/一行数据')
    }

    setLoading(true)
    try {
      const res = await axios.post(`${BASE_URL}/multi-analyze`, {
        chartName,
        goal: goal || '分析数据',
        selectConfigMap
      })
      const id = res.data.data
      setTaskId(Number(id))
      setChartData(null)
      if (pollTimer) clearInterval(pollTimer)
      message.success('任务提交成功！')
      startPoll(Number(id))
    } catch (err: any) {
      let errMsg = '提交失败'
      if (err.response) {
        const data = err.response.data || {}
        errMsg = data.message || data.msg || errMsg
      }
      message.error(errMsg)
    } finally {
      setLoading(false)
    }
  }

  // 轮询任务结果
  const startPoll = (id: number) => {
    const timer = setInterval(async () => {
      try {
        const res = await axios.get(`${BASE_URL}/get/${id}`)
        const info = res.data.data
        if (info.status === 'success') {
          clearInterval(timer)
          setPollTimer(null)
          setChartData(info)
          message.success('生成完成！')
        } else if (info.status === 'fail') {
          clearInterval(timer)
          setPollTimer(null)
          message.error('生成失败')
        }
      } catch (e) {
        clearInterval(timer)
        setPollTimer(null)
      }
    }, 2000)
    setPollTimer(timer)
  }

  // 渲染表格列
  const renderTableColumns = (fileName: string) => {
    const preview = tablePreviewMap[fileName]
    const config = selectConfigMap[fileName]
    const columns: any[] = [
      {
        title: (
          <Checkbox
            checked={config?.selectAll || false}
            onChange={(e) => handleSelectAll(fileName, e.target.checked)}
          />
        ),
        key: 'rowCheck',
        width: 50,
        render: (_: any, __: any, index: number) => (
          <Checkbox
            checked={config?.selectRowIndexes.includes(index + 1) || false}
            onChange={(e) => handleRowCheckChange(fileName, index + 1, e.target.checked)}
          />
        )
      }
    ]

    preview.headerList.forEach((header, colIndex) => {
      columns.push({
        title: (
          <Space>
            <Checkbox
              checked={config?.selectColumns.includes(header) || false}
              onChange={(e) => handleColumnCheckChange(fileName, header, e.target.checked)}
            />
            {header}
          </Space>
        ),
        key: `col_${colIndex}`,
        dataIndex: colIndex,
        ellipsis: true,
        width: 150
      })
    })
    return columns
  }

  return (
    <div style={{ padding: 24 }}>
      <Card title="多文件AI分析（支持任意行列勾选）">
        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          <Upload
            multiple
            accept=".xls,.xlsx"
            fileList={fileList}
            beforeUpload={() => false} // 禁止组件自动上传
            onChange={handleFileChange}
          >
            <Button icon={<UploadOutlined />}>上传Excel</Button>
          </Upload>

          {Object.keys(tablePreviewMap).length > 0 && (
            <Card size="small" title="勾选分析数据（支持整行/整列/单个单元格）">
              {Object.entries(tablePreviewMap).map(([fileName, preview]) => (
                <div key={fileName} style={{ marginBottom: 24 }}>
                  <h4>{fileName}（共{preview.totalRows}行）</h4>
                  <Table
                    columns={renderTableColumns(fileName)}
                    dataSource={preview.rowDataList}
                    rowKey={(_, index) => `row_${index}`}
                    pagination={{ pageSize: 20 }}
                    scroll={{ x: 'max-content', y: 400 }}
                    size="small"
                  />
                </div>
              ))}
            </Card>
          )}

          <Input placeholder="图表名称" value={chartName} onChange={e => setChartName(e.target.value)} />
          <Input.TextArea placeholder="分析目标" value={goal} onChange={e => setGoal(e.target.value)} rows={3} />

          <Button type="primary" onClick={submitAnalyse} loading={loading}>
            提交AI分析
          </Button>

          {taskId && !chartData && <Spin tip="生成中..." />}
          {chartData && chartData.genChart && (
            <Card title="分析结果">
              <ReactECharts option={JSON.parse(chartData.genChart)} style={{ height: 400 }} />
              <div style={{ marginTop: 16, background: '#f5f5f5', padding: 12 }}>
                <h4>AI结论</h4>
                <p>{chartData.genResult}</p>
              </div>
            </Card>
          )}
        </Space>
      </Card>
    </div>
  )
}

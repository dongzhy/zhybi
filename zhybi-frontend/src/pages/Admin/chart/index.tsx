import { Button, Drawer, message } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import { PageContainer, FooterToolbar, ProTable } from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import ChartUpdateForm from '../chart/components/UpdateForm';
import {
  adminDeleteChartUsingPost,
  listChartByPageAdminUsingPost,
} from '@/services/swagger/chartController';

const index: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [selectedRows, setSelectedRows] = useState<API.Chart[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  // 删除
  const { run: handleDelete, loading: deleteLoading } = useRequest(adminDeleteChartUsingPost, {
    manual: true,
    onSuccess: () => {
      messageApi.success('删除成功');
      actionRef.current?.reload();
    },
  });

  const columns: ProColumns<API.Chart>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '图表名称',
      dataIndex: 'name',
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: {
        wait: { text: '等待中', status: 'Default' },
        running: { text: '执行中', status: 'Processing' },
        succeed: { text: '已完成', status: 'Success' },
        failed: { text: '失败', status: 'Error' },
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => [
        <ChartUpdateForm
          key="edit"
          trigger={<a>编辑</a>}
          values={record}
          onReload={() => actionRef.current?.reload()}
        />,
      ],
    },
  ];

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<API.Chart>
        headerTitle="图表管理"
        actionRef={actionRef}
        rowKey="id"
        request={async (params) => {
          const res = await listChartByPageAdminUsingPost({
            current: params.current,
            pageSize: params.pageSize,
            name: params.name,
            userId: params.userId,
          });
          return {
            data: res.data?.records || [],
            total: res.data?.total || 0,
            success: true,
          };
        }}
        columns={columns}
        rowSelection={{
          onChange: (_, rows) => setSelectedRows(rows),
        }}
      />

      {selectedRows.length > 0 && (
        <FooterToolbar>
          <Button
            danger
            loading={deleteLoading}
            onClick={() => handleDelete({ id: selectedRows[0].id })}
          >
            批量删除
          </Button>
        </FooterToolbar>
      )}
    </PageContainer>
  );
};

export default index;

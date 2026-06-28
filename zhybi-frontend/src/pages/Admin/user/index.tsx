import { Button, Drawer, message } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import {
  PageContainer,
  FooterToolbar,
  ProTable,
  ProDescriptions,
} from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import type { ActionType, ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import CreateForm from './components/CreateForm';
import UpdateForm from './components/UpdateForm';
import {
  deleteUserUsingPost,
  listUserByPageUsingPost,
} from '@/services/swagger/userController';

const Index: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [showDetail, setShowDetail] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<API.UserVO>();
  const [selectedRows, setSelectedRows] = useState<API.UserVO[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  // 删除用户
  const { run: handleDelete, loading: deleteLoading } = useRequest(deleteUserUsingPost, {
    manual: true,
    onSuccess: () => {
      messageApi.success('删除成功');
      actionRef.current?.reload();
      setSelectedRows([]);
    },
    onError: () => messageApi.error('删除失败'),
  });

  // 表格列
  // @ts-ignore
  const columns: ProColumns<API.UserVO>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
      search: false,
    },
    {
      title: '用户账号',
      dataIndex: 'userAccount',
    },
    {
      title: '用户昵称',
      dataIndex: 'userName',
      renderText: (t) => t || '未设置',
    },
    {
      title: '头像',
      dataIndex: 'userAvatar',
      search: false,
      render: (_, r) => (
        <img
          src={r.userAvatar || 'https://picsum.photos/id/1005/200/200'}
          style={{ width: 40, height: 40, borderRadius: '50%' }}
          alt="avatar"
        />
      ),
    },
    {
      title: '角色',
      dataIndex: 'userRole',
      valueEnum: {
        user: { text: '普通用户', status: 'Default' },
        admin: { text: '管理员', status: 'Success' },
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      search: false,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => [
        <UpdateForm
          key="edit"
          trigger={<a>编辑</a>}
          values={record}
          onReload={() => actionRef.current?.reload()}
        />,
      ],
    },
  ];

  // 批量删除
  const batchDelete = useCallback(async () => {
    if (selectedRows.length === 0) {
      messageApi.warning('请选择数据');
      return;
    }
    await handleDelete({ id: selectedRows[0].id });
  }, [selectedRows, handleDelete]);

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<API.UserVO>
        headerTitle="用户管理"
        actionRef={actionRef}
        rowKey="id"
        pagination={{ pageSize: 10 }}
        toolBarRender={() => [<CreateForm key="add" onReload={() => actionRef.current?.reload()} />]}
        request={async (params) => {
          const res = await listUserByPageUsingPost({
            current: params.current,
            pageSize: params.pageSize,
            userAccount: params.userAccount,
            userName: params.userName,
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
          <Button loading={deleteLoading} danger onClick={batchDelete}>
            批量删除
          </Button>
        </FooterToolbar>
      )}

      <Drawer
        width={500}
        open={showDetail}
        onClose={() => setShowDetail(false)}
      >
        {currentRow && (
          <ProDescriptions
            column={2}
            title="用户详情"
            dataSource={currentRow}
            columns={columns as ProDescriptionsItemProps<API.UserVO>[]}
          />
        )}
      </Drawer>
    </PageContainer>
  );
};

export default Index;

import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, ProFormText, ProFormSelect } from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import { Button, message } from 'antd';
import React from 'react';
import { addUserUsingPost } from '@/services/swagger/userController';

const CreateForm: React.FC<{ onReload: () => void }> = ({ onReload }) => {
  const [messageApi, contextHolder] = message.useMessage();

  const { run, loading } = useRequest(addUserUsingPost, {
    manual: true,
    onSuccess: () => {
      messageApi.success('创建成功');
      onReload();
    },
    onError: () => messageApi.error('创建失败'),
  });

  return (
    <>
      {contextHolder}
      <ModalForm
        title="新建用户"
        trigger={
          <Button type="primary" icon={<PlusOutlined />}>
            新建
          </Button>
        }
        width="450px"
        modalProps={{ okButtonProps: { loading } }}
        onFinish={async (values) => {
          await run(values);
          return true;
        }}
      >
        <ProFormText
          name="userAccount"
          label="用户账号"
          rules={[{ required: true }]}
        />
        <ProFormText
          name="userName"
          label="用户昵称"
        />
        <ProFormSelect
          name="userRole"
          label="角色"
          initialValue="user"
          valueEnum={{ user: '普通用户', admin: '管理员' }}
        />
      </ModalForm>
    </>
  );
};

export default CreateForm;

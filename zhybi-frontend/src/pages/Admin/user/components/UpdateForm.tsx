import { ProForm, ProFormText, ProFormSelect } from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import { Modal, message } from 'antd';
import React, { useState } from 'react';
import { updateUserUsingPost } from '@/services/swagger/userController';

const UpdateForm: React.FC<{
  trigger: React.ReactElement;
  values: API.UserVO;
  onReload: () => void;
}> = ({ trigger, values, onReload }) => {
  const [open, setOpen] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [formValues, setFormValues] = useState({ ...values });

  const { run, loading } = useRequest(updateUserUsingPost, {
    manual: true,
    onSuccess: () => {
      messageApi.success('修改成功');
      setOpen(false);
      onReload();
    },
    onError: () => messageApi.error('修改失败'),
  });

  return (
    <>
      {contextHolder}
      {React.cloneElement(trigger, { onClick: () => setOpen(true) })}

      <Modal
        open={open}
        title="编辑用户"
        confirmLoading={loading}
        onCancel={() => setOpen(false)}
        onOk={() => run(formValues)}
      >
        <ProForm
          initialValues={formValues}
          onValuesChange={(_, allValues) => setFormValues(allValues)}
        >
          {/* 隐藏域自动提交 id */}
          <ProFormText name="id" hidden />

          <ProFormText name="userName" label="用户昵称" />
          <ProFormSelect
            name="userRole"
            label="用户角色"
            valueEnum={{ user: '普通用户', admin: '管理员' }}
          />
        </ProForm>
      </Modal>
    </>
  );
};

export default UpdateForm;

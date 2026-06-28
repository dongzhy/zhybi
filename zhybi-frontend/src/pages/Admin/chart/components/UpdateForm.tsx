import { ProForm, ProFormText, ProFormSelect } from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import { Modal, message } from 'antd';
import React, { useState } from 'react';
import { adminUpdateChartUsingPost } from '@/services/swagger/chartController';

const ChartUpdateForm: React.FC<{
  trigger: React.ReactElement;
  values: API.Chart;
  onReload: () => void;
}> = ({ trigger, values, onReload }) => {
  const [open, setOpen] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const { run, loading } = useRequest(adminUpdateChartUsingPost, {
    manual: true,
    onSuccess: () => {
      messageApi.success('修改成功');
      setOpen(false);
      onReload();
    },
  });

  // @ts-ignore
  return (
    <>
      {contextHolder}
      {React.cloneElement(trigger, { onClick: () => setOpen(true) })}
      <Modal
        open={open}
        title="编辑图表"
        confirmLoading={loading}
        onCancel={() => setOpen(false)}
        onOk={() => run(values)}
      >
        <ProForm initialValues={values} onValuesChange={(_, v) => (values = v)}>
          <ProFormText name="id" hidden />
          <ProFormText name="name" label="图表名称" />
          <ProFormText name="goal" label="分析目标" />
          <ProFormSelect
            name="status"
            label="状态"
            valueEnum={{
              wait: '等待',
              running: '执行中',
              succeed: '成功',
              failed: '失败',
            }}
          />
        </ProForm>
      </Modal>
    </>
  );
};

export default ChartUpdateForm;

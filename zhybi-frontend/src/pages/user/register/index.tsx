import { Footer } from '@/components';
import {
  LockOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  ProForm,
  ProFormText,
} from '@ant-design/pro-components';
import { Helmet, Link, useModel } from '@umijs/max';
import { Alert, App, Tabs } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import Settings from '../../../../config/defaultSettings';
import { userRegisterUsingPost } from "@/services/swagger/userController";

const useStyles = createStyles(({ token }) => {
  return {
    container: {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
        "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    },
  };
});

const RegisterMessage: React.FC<{ content: string }> = ({ content }) => {
  return <Alert style={{ marginBottom: 24 }} message={content} type="error" showIcon />;
};

const Register: React.FC = () => {
  const [type, setType] = useState<string>('account');
  const { message } = App.useApp();
  const { styles } = useStyles();

  // 注册提交
  const handleSubmit = async (values: API.UserRegisterRequest) => {
    try {
      // 校验两次密码一致
      if (values.userPassword !== values.checkPassword) {
        message.error('两次输入的密码不一致！');
        return;
      }

      const res = await userRegisterUsingPost(values);
      if (res.code === 0) {
        message.success('注册成功！即将跳转到登录页');
        setTimeout(() => {
          window.location.href = '/user/login';
        }, 1000);
      } else {
        message.error(res.message || '注册失败，请重试');
      }
    } catch (error) {
      message.error('注册请求异常，请稍后再试');
      console.error(error);
    }
  };

  return (
    <div className={styles.container}>
      <Helmet>
        <title>注册 - {Settings.title}</title>
      </Helmet>

      <div style={{ flex: 1, padding: '32px 0', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <div style={{ width: 400, background: '#fff', padding: 32, borderRadius: 8 }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <h2>智能 BI - 注册</h2>
          </div>

          <ProForm
            onFinish={handleSubmit}
            submitter={{
              searchConfig: {
                submitText: '立即注册',
              },
              render: (_, dom) => (
                <div style={{ marginTop: 16 }}>{dom}</div>
              ),
            }}
          >
            <Tabs activeKey={type} onChange={setType} centered items={[{ key: 'account', label: '账户密码注册' }]} />

            {type === 'account' && (
              <>
                <ProFormText
                  name="userAccount"
                  fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
                  placeholder="请输入用户名"
                  rules={[{ required: true, message: '请输入用户名' }]}
                />

                <ProFormText.Password
                  name="userPassword"
                  fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
                  placeholder="请输入密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                />

                <ProFormText.Password
                  name="checkPassword"
                  fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
                  placeholder="请确认密码"
                  rules={[{ required: true, message: '请确认密码' }]}
                />
              </>
            )}
          </ProForm>

          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Link to="/user/login">已有账号？立即登录</Link>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
};

export default Register;

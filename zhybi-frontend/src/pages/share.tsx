import { useState, useEffect } from 'react';
import { useParams } from '@umijs/max';
import { request } from '@umijs/max';

export default function SharePage() {
  // 获取动态路由参数 shareCode
  const { shareCode } = useParams();

  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [needPassword, setNeedPassword] = useState(false);
  const [isPass, setIsPass] = useState(false);
  const [inputPwd, setInputPwd] = useState('');
  const [boardData, setBoardData] = useState<Record<string, any> | null>(null);

  // 页面加载校验分享链接
  useEffect(() => {
    if (!shareCode) return;

    const verifyShare = async () => {
      try {
        setLoading(true);
        const res = await request.get('/board/share/verify', {
          params: { shareCode },
        });
        if (res.code !== 0) {
          setErrorMsg(res.message || '分享链接失效');
          return;
        }
        setNeedPassword(res.data.needPassword);
        // 无密码直接校验
        if (!res.data.needPassword) {
          await checkPassword('');
        }
      } catch (err) {
        setErrorMsg('请求异常');
      } finally {
        setLoading(false);
      }
    };

    verifyShare();
  }, [shareCode]);

  // 密码校验函数
  const checkPassword = async (pwd: string) => {
    try {
      const res = await request.post('/board/share/verifyPwd', {
        shareCode,
        inputPwd: pwd,
      });
      if (res.code === 0) {
        setIsPass(true);
        setBoardData(res.data);
      } else {
        setErrorMsg(res.message || '密码错误');
      }
    } catch {
      setErrorMsg('验证失败');
    }
  };

  if (loading) return <div>加载中...</div>;
  if (errorMsg) return <div style={{ color: 'red' }}>{errorMsg}</div>;

  // 需要密码输入框
  if (needPassword && !isPass) {
    return (
      <div style={{ padding: '40px' }}>
        <h2>该分享需要访问密码</h2>
        <input
          type="password"
          value={inputPwd}
          onChange={(e) => setInputPwd(e.target.value)}
          placeholder="请输入访问密码"
        />
        <button onClick={() => checkPassword(inputPwd)}>确认</button>
      </div>
    );
  }

  // 验证成功，渲染看板
  return (
    <div style={{ padding: '20px' }}>
      <h2>共享看板</h2>
      <p>看板ID：{boardData?.id}</p>
      {/* 此处放置你的可视化 ECharts / 大屏组件 */}
    </div>
  );
}

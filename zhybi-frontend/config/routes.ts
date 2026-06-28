export default [
  {
    path: '/dashboard',
    name: '图表情况概览',
    icon: 'dashboard',
    component: './Dashboard/Index',
  },
  {
    path: '/user',
    layout: false,
    routes: [
      { name: '登录', path: '/user/login', component: './user/login' },
      { name: '注册', path: '/user/register', component: './user/register' },
    ],
  },
  // { path: '/template', name: '模板中心', icon: 'barChart', component: './TemplateList' },
  { path: '/smart/analysis', name: '智能数据分析', icon: 'barChart', component: './MultiFileAnalyse' },
  { path: '/add_chart', name: '智能分析', icon: 'barChart', component: './AddChart' },
  { path: '/add_chart_async', name: '智能分析（异步）', icon: 'barChart', component: './AddChartAsync' },
  { path: '/my_chart', name: '我的图表', icon: 'pieChart', component: './MyChart' },
  { path: '/data_board', name: '数据大屏', icon: 'pieChart', component: './DataScreenBoard' },

  {
    path: '/admin',
    name: '管理页',
    layout: true,
    icon: 'crown',
    routes: [
      { path: '/admin/user', name: '用户管理', component: './Admin/user/index' },
      { path: '/admin/chart', name: '图表管理', component: './Admin/chart/index' },
    ],
  },
  // ✅ 只保留一个重定向
  { path: '/', redirect: '/dashboard' },
  { path: '*', layout: false, component: './404' },
  {
    path: '/share/:shareCode',
    component: '@/pages/share', // 页面文件路径 src/pages/share.tsx
    layout: false, // 关闭全局layout，纯净页面
  },
];

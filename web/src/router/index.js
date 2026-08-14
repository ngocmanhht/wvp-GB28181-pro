import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
 roles: ['admin','editor']    control the page roles (you can set multiple roles)
 title: 'title'               the name show in sidebar and breadcrumb (recommend set)
 icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
 breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
 activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
 }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [{
      path: 'dashboard',
      name: 'console',
      component: () => import('@/views/dashboard/index'),
      meta: { title: 'console', icon: 'dashboard', affix: true }
    }]
  },

  {
    path: '/live',
    component: Layout,
    redirect: '/live',
    children: [{
      path: '',
      name: 'Live',
      component: () => import('@/views/live/index'),
      meta: { title: 'Split screen monitoring', icon: 'live' }
    }]
  },
  {
    path: '/channel',
    component: Layout,
    redirect: '/channel',
    onlyIndex: 0,
    children: [{
      path: '/channel',
      name: 'Channel',
      component: () => import('@/views/channel/index'),
      meta: { title: 'Channel list', icon: 'channelManger' }
    },
    {
      path: '/channel/record/:channelId',
      name: 'CommonRecord',
      component: () => import('@/views/channel/record'),
      meta: { title: 'Equipment video' }
    }
    ]
  },
  {
    path: '/map',
    component: Layout,
    redirect: '/map',
    children: [{
      path: '',
      name: 'Map',
      component: () => import('@/views/map/index'),
      meta: { title: 'electronic map', icon: 'map' }
    }]
  },
  {
    path: '/device',
    component: Layout,
    name: 'Device access',
    meta: { title: 'Device access', icon: 'devices' },
    children: [
      {
        path: '/device',
        name: 'Device',
        component: () => import('@/views/device/index'),
        meta: { title: 'National standard equipment', icon: 'device' }
      },
      {
        hidden: true,
        path: '/device/record/:deviceId/:channelDeviceId',
        name: 'DeviceRecord',
        component: () => import('@/views/device/channel/record'),
        meta: { title: 'National standard video' }
      },
      {
        path: '/jtDevice',
        name: 'JTDevice',
        component: () => import('@/views/jtDevice/index'),
        meta: { title: 'Ministry standard equipment', icon: 'jtDevice' }
      },
      {
        hidden: true,
        path: '/jtDevice/record/:phoneNumber/:channelId',
        name: 'JTDeviceRecord',
        component: () => import('@/views/jtDevice/channel/record'),
        meta: { title: 'Ministry logo video' }
      },
      {
        path: '/push',
        name: 'PushList',
        component: () => import('@/views/streamPush/index'),
        meta: { title: 'Push list', icon: 'streamPush' }
      },
      {
        path: '/proxy',
        name: 'Proxy',
        component: () => import('@/views/streamProxy/index'),
        meta: { title: 'Streaming agent', icon: 'streamProxy' }
      }
    ]
  },
  {
    path: '/commonChannel',
    component: Layout,
    redirect: '/commonChannel/region',
    name: 'organizational structure',
    meta: { title: 'organizational structure', icon: 'tree' },
    children: [
      {
        path: 'region',
        name: 'Region',
        component: () => import('@/views/channel/region/index'),
        meta: { title: 'Administrative division', icon: 'region' }
      },
      {
        path: 'group',
        name: 'Group',
        component: () => import('@/views/channel/group/index'),
        meta: { title: 'business grouping', icon: 'tree' }
      }
    ]
  },
  {
    path: '/alarm',
    component: Layout,
    redirect: '/alarm',
    children: [
      {
        path: '',
        name: 'AlarmManage',
        component: () => import('@/views/alarm/index'),
        meta: { title: 'Alarm management', icon: 'el-icon-bell' }
      }
    ]
  },
  {
    path: '/recordPlan',
    component: Layout,
    redirect: '/recordPlan',
    children: [
      {
        path: '',
        name: 'RecordPlan',
        component: () => import('@/views/recordPlan/index'),
        meta: { title: 'Recording plan', icon: 'recordPlan' }
      }
    ]
  },
  {
    path: '/cloudRecord',
    component: Layout,
    redirect: '/cloudRecord',
    onlyIndex: 0,
    children: [
      {
        path: '/cloudRecord',
        name: 'CloudRecord',
        component: () => import('@/views/cloudRecord/index'),
        meta: { title: 'Cloud recording', icon: 'cloudRecord' }
      },
      {
        path: '/cloudRecord/detail/:app/:stream',
        name: 'CloudRecordDetail',
        component: () => import('@/views/cloudRecord/detail'),
        meta: { title: 'Cloud recording details' }
      }
    ]
  },
  {
    path: '/mediaServer',
    component: Layout,
    redirect: '/mediaServer',
    children: [
      {
        path: '',
        name: 'MediaServer',
        component: () => import('@/views/mediaServer/index'),
        meta: { title: 'media node', icon: 'mediaServerList' }
      }
    ]
  },
  {
    path: '/platform',
    component: Layout,
    redirect: '/platform',
    children: [
      {
        path: '',
        name: 'Platform',
        component: () => import('@/views/platform/index'),
        meta: { title: 'National standard cascade', icon: 'platform' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    redirect: '/user',
    children: [
      {
        path: '',
        name: 'User',
        component: () => import('@/views/user/index'),
        meta: { title: 'User management', icon: 'user' }
      }
    ]
  },
  // {
  //   path: '/setting',
  //   component: Layout,
  //   redirect: '/setting',
  //   children: [
  //     {
  //       path: '',
  //       name: 'System settings',
  //       component: () => import('@/views/platform/index'),
  //       meta: { title: 'System settings', icon: 'setting' }
  //     }
  //   ]
  // },
  {
    path: '/operations',
    component: Layout,
    meta: { title: 'Operation and maintenance center', icon: 'operations' },
    redirect: '/operations/systemInfo',
    children: [
      {
        path: '/operations/systemInfo',
        name: 'OperationsSystemInfo',
        component: () => import('@/views/operations/systemInfo'),
        meta: { title: 'Platform information', icon: 'systemInfo' }
      },
      {
        path: '/operations/historyLog',
        name: 'OperationsHistoryLog',
        component: () => import('@/views/operations/historyLog'),
        meta: { title: 'History log', icon: 'historyLog' }
      },
      {
        path: '/operations/realLog',
        name: 'OperationsRealLog',
        component: () => import('@/views/operations/realLog'),
        meta: { title: 'real time log', icon: 'realLog' }
      }
    ]
  },
  {
    path: '/play/share',
    name: 'sharePlayer',
    hidden: true,
    component: () => import('@/views/common/share.vue')
  },
  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router

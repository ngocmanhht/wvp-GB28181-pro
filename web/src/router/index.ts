import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import AppLayout from '@/components/layout/AppLayout.vue'

NProgress.configure({ showSpinner: false })

export const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { hidden: true }
  },
  {
    path: '/play/share',
    name: 'SharePlayer',
    component: () => import('@/views/share/SharePlayer.vue'),
    meta: { hidden: true, public: true }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: 'Dashboard', icon: 'Odometer' }
      },
      {
        path: 'live',
        name: 'LiveView',
        component: () => import('@/views/live/LiveView.vue'),
        meta: { title: 'Split Live Monitor', icon: 'VideoCamera' }
      },
      {
        path: 'channel',
        name: 'ChannelList',
        component: () => import('@/views/channel/ChannelList.vue'),
        meta: { title: 'Channels', icon: 'List' }
      },
      {
        path: 'channel/record/:channelId',
        name: 'ChannelRecord',
        component: () => import('@/views/channel/ChannelRecord.vue'),
        meta: { title: 'Channel Record', hidden: true }
      },
      {
        path: 'device',
        name: 'DeviceList',
        component: () => import('@/views/device/DeviceList.vue'),
        meta: { title: 'GB28181 Devices', icon: 'Monitor' }
      },
      {
        path: 'device/record/:deviceId/:channelDeviceId',
        name: 'DeviceRecord',
        component: () => import('@/views/device/DeviceRecord.vue'),
        meta: { title: 'Device Record', hidden: true }
      },
      {
        path: 'jtDevice',
        name: 'JTDevice',
        component: () => import('@/views/jtDevice/JtDeviceView.vue'),
        meta: { title: 'JT1078 Vehicles', icon: 'Van' }
      },
      {
        path: 'stream/proxy',
        name: 'StreamProxy',
        component: () => import('@/views/stream/StreamProxy.vue'),
        meta: { title: 'Stream Proxy', icon: 'Connection' }
      },
      {
        path: 'stream/push',
        name: 'StreamPush',
        component: () => import('@/views/stream/StreamPush.vue'),
        meta: { title: 'Stream Push', icon: 'Upload' }
      },
      {
        path: 'org/region',
        name: 'OrgRegion',
        component: () => import('@/views/org/RegionView.vue'),
        meta: { title: 'Administrative Division', icon: 'OfficeBuilding' }
      },
      {
        path: 'org/group',
        name: 'OrgGroup',
        component: () => import('@/views/org/GroupView.vue'),
        meta: { title: 'Business Grouping', icon: 'Folder' }
      },
      {
        path: 'cloudRecord',
        name: 'CloudRecord',
        component: () => import('@/views/cloudRecord/CloudRecordList.vue'),
        meta: { title: 'Cloud Record', icon: 'Film' }
      },
      {
        path: 'recordPlan',
        name: 'RecordPlan',
        component: () => import('@/views/recordPlan/RecordPlanView.vue'),
        meta: { title: 'Recording Plan', icon: 'Calendar' }
      },
      {
        path: 'map',
        name: 'MapView',
        component: () => import('@/views/map/MapView.vue'),
        meta: { title: 'GIS Map', icon: 'Location' }
      },
      {
        path: 'mediaServer',
        name: 'MediaServer',
        component: () => import('@/views/mediaServer/MediaServerList.vue'),
        meta: { title: 'Media Nodes', icon: 'Coin' }
      },
      {
        path: 'platform',
        name: 'Platform',
        component: () => import('@/views/platform/PlatformList.vue'),
        meta: { title: 'GB Cascade', icon: 'Share' }
      },
      {
        path: 'alarm',
        name: 'AlarmList',
        component: () => import('@/views/alarm/AlarmList.vue'),
        meta: { title: 'Alarms', icon: 'Bell' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: 'Users & API Keys', icon: 'User' }
      },
      {
        path: 'systemInfo',
        name: 'SystemInfo',
        component: () => import('@/views/operations/SystemInfo.vue'),
        meta: { title: 'System Status', icon: 'Cpu' }
      },
      {
        path: 'logs',
        name: 'RealLog',
        component: () => import('@/views/operations/RealLog.vue'),
        meta: { title: 'Real-time Logs', icon: 'Document' }
      },
      {
        path: 'operations/history',
        name: 'HistoryLog',
        component: () => import('@/views/operations/HistoryLog.vue'),
        meta: { title: 'Audit History Logs', icon: 'DocumentCopy' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// Route Guard
router.beforeEach((to, _from, next) => {
  NProgress.start()
  const token = getToken()

  if (to.meta?.public || to.path === '/play/share') {
    next()
    return
  }

  if (to.path === '/login') {
    if (token) {
      next({ path: '/' })
    } else {
      next()
    }
  } else {
    if (token) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router

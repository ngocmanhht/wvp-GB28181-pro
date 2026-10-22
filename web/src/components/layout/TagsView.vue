<template>
  <div class="h-9 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 flex items-center px-4 overflow-x-auto gap-1 select-none scrollbar-none transition-colors duration-200">
    <router-link
      v-for="tag in appStore.visitedViews"
      :key="tag.path"
      :to="tag.path"
      class="flex items-center gap-1.5 px-3 py-1 text-xs rounded-md border transition-all"
      :class="
        isActive(tag.path)
          ? 'bg-blue-50 dark:bg-blue-600/20 border-blue-200 dark:border-blue-500/40 text-blue-600 dark:text-blue-400 font-medium'
          : 'bg-slate-50 dark:bg-slate-800/40 border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
      "
    >
      <span class="w-1.5 h-1.5 rounded-full" :class="isActive(tag.path) ? 'bg-blue-500 dark:bg-blue-400' : 'bg-slate-300 dark:bg-slate-600'"></span>
      <span>{{ tag.title }}</span>
      <el-icon
        v-if="tag.path !== '/dashboard'"
        class="text-[10px] ml-1 hover:text-rose-500 dark:hover:text-rose-400 cursor-pointer"
        @click.prevent.stop="closeTag(tag.path)"
      >
        <Close />
      </el-icon>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Close } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

function isActive(path: string) {
  return route.path === path
}

function addCurrentView() {
  if (route.meta?.title && !route.meta?.hidden) {
    appStore.addVisitedView({
      name: route.name as string || '',
      path: route.path,
      title: route.meta.title as string
    })
  }
}

function closeTag(path: string) {
  appStore.removeVisitedView(path)
  if (route.path === path) {
    const latest = appStore.visitedViews[appStore.visitedViews.length - 1]
    if (latest) {
      router.push(latest.path)
    } else {
      router.push('/')
    }
  }
}

watch(
  () => route.path,
  () => {
    addCurrentView()
  },
  { immediate: true }
)
</script>

<style scoped>
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>

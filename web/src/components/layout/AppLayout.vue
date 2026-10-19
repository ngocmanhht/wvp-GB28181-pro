<template>
  <div class="flex h-screen w-screen overflow-hidden bg-slate-950 text-slate-100 font-sans">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
      <!-- Top Header -->
      <Header />

      <!-- Tabs Navigation -->
      <TagsView />

      <!-- Router Main View Area -->
      <main class="flex-1 overflow-y-auto overflow-x-hidden p-4 bg-slate-950/80">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import TagsView from './TagsView.vue'
</script>

<style>
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>

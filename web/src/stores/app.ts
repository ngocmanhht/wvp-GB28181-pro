import { defineStore } from "pinia";
import { ref } from "vue";

export interface TagView {
  name: string;
  path: string;
  title: string;
}

export const useAppStore = defineStore("app", () => {
  const sidebarCollapsed = ref(false);
  const isDark = ref(true); // Default to sleek dark mode for surveillance UI
  const visitedViews = ref<TagView[]>([
    { name: "Dashboard", path: "/dashboard", title: "Dashboard" },
  ]);

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function toggleTheme() {
    isDark.value = !isDark.value;
    if (isDark.value) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }

  function addVisitedView(view: TagView) {
    if (!view.path || visitedViews.value.some((v) => v.path === view.path))
      return;
    visitedViews.value.push(view);
  }

  function removeVisitedView(path: string) {
    visitedViews.value = visitedViews.value.filter((v) => v.path !== path);
  }

  return {
    sidebarCollapsed,
    isDark,
    visitedViews,
    toggleSidebar,
    toggleTheme,
    addVisitedView,
    removeVisitedView,
  };
});

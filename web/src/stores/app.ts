import { defineStore } from "pinia";
import { ref } from "vue";

export interface TagView {
  name: string;
  path: string;
  title: string;
}

export const useAppStore = defineStore("app", () => {
  const sidebarCollapsed = ref(false);
  const savedTheme = localStorage.getItem("wvp_theme");
  const isDark = ref(savedTheme !== null ? savedTheme === "dark" : true);
  const visitedViews = ref<TagView[]>([
    { name: "Dashboard", path: "/dashboard", title: "Dashboard" },
  ]);

  function applyTheme(dark: boolean) {
    isDark.value = dark;
    if (dark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("wvp_theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("wvp_theme", "light");
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function toggleTheme() {
    applyTheme(!isDark.value);
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
    applyTheme,
    toggleSidebar,
    toggleTheme,
    addVisitedView,
    removeVisitedView,
  };
});

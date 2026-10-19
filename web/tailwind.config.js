/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        wvp: {
          dark: '#0f172a',
          darker: '#020617',
          card: '#1e293b',
          border: '#334155',
          primary: '#2563eb',
          accent: '#38bdf8'
        }
      }
    },
  },
  plugins: [],
}

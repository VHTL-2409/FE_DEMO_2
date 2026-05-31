import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useToastStore } from './stores/toastStore'
import LucideIcon from './components/common/LucideIcon.vue'
import PrimeVue from 'primevue/config'
import Dialog from 'primevue/dialog'
import './styles/tailwind.css'
import './fonts.css'
import './animation.css'
import './styles/design-tokens.css'
import './styles/tokens.css'
import './styles/staff-ui.css'
import './styles/admin-ui.css'
import './styles/admin-dashboard.css'
import './styles/teacher-ui.css'
import './styles/teacher-ui-refresh.css'
import './styles/portal-ui.css'
import './styles/monitoring.css'
import './styles/system-ui.css'
import './styles/teacher-command.css'
import './styles/anti-blur-overrides.css'

let _toast = null
const toastServiceProxy = () => {
  if (!_toast) _toast = useToastStore()
  return {
    show: (...args) => _toast.show(...args),
    success: (...args) => _toast.success(...args),
    error: (...args) => _toast.error(...args),
    info: (...args) => _toast.info(...args),
    warning: (...args) => _toast.warning(...args),
    dismiss: (...args) => _toast.dismiss(...args),
    clearAll: () => _toast.clearAll()
  }
}

createApp(App).use(pinia).provide('toast', toastServiceProxy()).use(router).use(PrimeVue).component('Dialog', Dialog).component('LucideIcon', LucideIcon).mount('#app')

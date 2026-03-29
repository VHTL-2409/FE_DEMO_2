import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { toastService } from './services/toastService'
import './styles/tailwind.css'
import './styles/design-tokens.css'
import './styles/staff-ui.css'
import './styles/admin-ui.css'
import './styles/teacher-ui.css'
import './styles/portal-ui.css'

createApp(App).use(pinia).provide('toast', toastService).use(router).mount('#app')
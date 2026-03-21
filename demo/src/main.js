import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { toastService } from './services/toastService'
import './styles/teacher-ui.css'
import './styles/portal-ui.css'

createApp(App).provide('toast', toastService).use(router).mount('#app')
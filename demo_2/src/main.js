import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { toastService } from './services/toastService'
import './styles/tailwind.css'
import './styles/tokens.css'
import './styles/glass-system.css'
import './styles/liquid-neural-tokens.css'
import './styles/liquid-neural.css'
import './styles/demo2-liquid.css'
import './styles/cosmos-prismatic.css'
import './styles/studio-minimal.css'

document.documentElement.dataset.demo2 = '1'

createApp(App).use(pinia).provide('toast', toastService).use(router).mount('#app')
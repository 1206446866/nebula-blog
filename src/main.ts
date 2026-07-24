import { createApp } from 'vue'

import App from './App.vue'

import router from './router'

import { createPinia } from 'pinia'

import ElementPlus from 'element-plus'

import 'element-plus/dist/index.css'

import './assets/main.css'
import '@/router/permission'
import '@/styles/global.css'
import '@/styles/theme.css'
import '@/styles/element.css'

const app = createApp(App)

app.use(createPinia())

app.use(router)

app.use(ElementPlus)

app.mount('#app')

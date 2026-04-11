import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'

export default [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    ignores: ['dist', 'node_modules', '*.config.js']
  },
  {
    rules: {
      'vue/multi-word-component-names': 'warn',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]
    }
  }
]

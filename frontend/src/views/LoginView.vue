<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Logo -->
      <div class="login-logo">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" x2="12" y1="19" y2="22"/>
        </svg>
      </div>

      <h1 class="login-title">{{ isRegister ? '创建账户' : '欢迎回来' }}</h1>
      <p class="login-subtitle">{{ isRegister ? '注册后开始你的英语口语练习之旅' : '登录后继续你的学习' }}</p>

      <!-- 错误提示 -->
      <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>

      <!-- 表单 -->
      <form class="login-form" @submit.prevent="submit">
        <!-- 用户名 -->
        <div class="input-group">
          <input
            v-model="form.username"
            type="text"
            class="input-field"
            placeholder="用户名"
            required
            maxlength="64"
          />
        </div>

        <!-- 邮箱（仅注册） -->
        <div v-if="isRegister" class="input-group">
          <input
            v-model="form.email"
            type="email"
            class="input-field"
            placeholder="邮箱（选填）"
            maxlength="128"
          />
        </div>

        <!-- 密码 -->
        <div class="input-group">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="input-field"
            placeholder="密码"
            required
            minlength="6"
            maxlength="128"
          />
          <button type="button" class="toggle-pw" @click="showPassword = !showPassword">
            <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
              <path d="m14.12 14.12a3 3 0 1 1-4.24-4.24"/>
              <line x1="1" y1="1" x2="23" y2="23"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </button>
        </div>

        <!-- 提交按钮 -->
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
        </button>
      </form>

      <!-- 切换 -->
      <p class="login-switch">
        {{ isRegister ? '已有账户？' : '还没有账户？' }}
        <a href="#" @click.prevent="toggleMode">{{ isRegister ? '登录' : '立即注册' }}</a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/auth'

const router = useRouter()
const isRegister = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

function toggleMode() {
  isRegister.value = !isRegister.value
  errorMsg.value = ''
}

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    if (isRegister.value) {
      const res = await register({
        username: form.username,
        password: form.password,
        email: form.email || undefined
      })
      if (res.code === 200 && res.data) {
        saveToken(res.data)
        router.push('/')
      } else {
        errorMsg.value = (res as any).message || '注册失败'
      }
    } else {
      const res = await login({
        username: form.username,
        password: form.password
      })
      if (res.code === 200 && res.data) {
        saveToken(res.data)
        router.push('/')
      } else {
        errorMsg.value = (res as any).message || '登录失败'
      }
    }
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || e?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function saveToken(data: { userId: number; username: string; token: string }) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('userId', String(data.userId))
  localStorage.setItem('username', data.username)
}
</script>

<style lang="scss" scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  padding: 20px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: var(--shadow-lg);
}

.login-logo {
  width: 52px;
  height: 52px;
  background: var(--color-accent-light);
  color: var(--color-accent);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.login-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
  margin: 0 0 6px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
  text-align: center;
  margin: 0 0 24px;
}

.login-error {
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
  text-align: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-group {
  position: relative;
}

.input-field {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border-hover);
  border-radius: 10px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;

  &::placeholder {
    color: var(--color-text-tertiary);
  }

  &:focus {
    border-color: var(--color-accent);
    box-shadow: 0 0 0 3px var(--color-accent-subtle);
  }
}

.toggle-pw {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;

  &:hover {
    color: var(--color-text-secondary);
  }
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 4px;

  &:hover:not(:disabled) {
    background: var(--color-accent-hover);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.login-switch {
  text-align: center;
  font-size: 13.5px;
  color: var(--color-text-tertiary);
  margin: 22px 0 0;

  a {
    color: var(--color-accent);
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>

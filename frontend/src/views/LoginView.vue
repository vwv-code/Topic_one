<template>
  <div class="login-page">
    <div class="container">
      <!-- 左侧品牌区 -->
      <div class="left-section">
        <h1 class="brand-logo">AI 口语陪练</h1>
        <p class="brand-subtitle">口语提升易如反掌</p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="right-section">
        <div class="login-card">
          <!-- ========== 忘记密码模式 ========== -->
          <template v-if="showForgotPassword">
            <!-- 成功提示 -->
            <div v-if="resetSuccess" class="login-success">{{ resetSuccess }}</div>
            <!-- 错误提示 -->
            <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>

            <form @submit.prevent="submitReset" v-if="!resetSuccess">
              <div class="form-group">
                <input
                  v-model="resetForm.email"
                  type="text"
                  placeholder="请输入注册邮箱"
                  required
                />
              </div>
              <div class="form-group">
                <input
                  v-model="resetForm.newPassword"
                  type="password"
                  placeholder="新密码"
                  required
                  minlength="6"
                  maxlength="128"
                />
              </div>
              <div class="form-group">
                <input
                  v-model="resetForm.confirmPassword"
                  type="password"
                  placeholder="确认新密码"
                  required
                  minlength="6"
                  maxlength="128"
                />
              </div>
              <button type="submit" class="login-btn" :disabled="loading">
                {{ loading ? '处理中...' : '重置密码' }}
              </button>
            </form>

            <div class="divider" v-if="!resetSuccess"></div>
            <a href="#" class="forgot-link" @click.prevent="showForgotPassword = false; errorMsg = ''; resetSuccess = ''">
              {{ resetSuccess ? '返回登录' : '返回登录' }}
            </a>
          </template>

          <!-- ========== 登录/注册模式 ========== -->
          <template v-else>
            <!-- 错误提示 -->
            <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>

            <form @submit.prevent="submit">
              <!-- 用户名 -->
              <div class="form-group">
                <input
                  v-model="form.username"
                  type="text"
                  :placeholder="isRegister ? '用户名' : '用户名或邮箱'"
                  required
                  maxlength="64"
                />
              </div>

              <!-- 邮箱（仅注册） -->
              <div v-if="isRegister" class="form-group">
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="邮箱（选填）"
                  maxlength="128"
                />
              </div>

              <!-- 密码 -->
              <div class="form-group password-group">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
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

              <button type="submit" class="login-btn" :disabled="loading">
                {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
              </button>
            </form>

            <a href="#" class="forgot-link" @click.prevent="showForgotPassword = true; errorMsg = ''">忘记密码？</a>
            <div class="divider"></div>

            <button class="create-btn" @click="toggleMode">
              {{ isRegister ? '使用已有账户登录' : '创建新账户' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, resetPassword } from '@/api/auth'

const router = useRouter()
const isRegister = ref(false)
const showPassword = ref(false)
const showForgotPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const resetSuccess = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const resetForm = reactive({
  email: '',
  newPassword: '',
  confirmPassword: ''
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

async function submitReset() {
  errorMsg.value = ''
  resetSuccess.value = ''

  if (resetForm.newPassword !== resetForm.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    const res = await resetPassword({
      email: resetForm.email,
      newPassword: resetForm.newPassword
    })
    if (res.code === 200) {
      resetSuccess.value = '密码重置成功，请返回登录'
    } else {
      errorMsg.value = (res as any).message || '重置失败'
    }
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || e?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
  background: #f0f2f5;
}

.container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 980px;
  width: 100%;
  gap: 80px;
}

// ========== 左侧品牌区 ==========
.left-section {
  flex: 1;
  max-width: 500px;
}

.brand-logo {
  color: #0ea5e9;
  font-size: 56px;
  font-weight: bold;
  margin-bottom: 16px;
  line-height: 1.1;
}

.brand-subtitle {
  font-size: 28px;
  line-height: 32px;
  color: #1c1e21;
}

// ========== 右侧登录表单 ==========
.right-section {
  flex: 1;
  max-width: 400px;
}

.login-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1), 0 8px 16px rgba(0, 0, 0, 0.1);
}

.login-error {
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 6px;
  margin-bottom: 12px;
  text-align: center;
  border: 1px solid #fecaca;
}

.login-success {
  background: #f0fdf4;
  color: #16a34a;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 6px;
  margin-bottom: 12px;
  text-align: center;
  border: 1px solid #bbf7d0;
}

.form-group {
  margin-bottom: 12px;
  position: relative;
}

.form-group input {
  width: 100%;
  padding: 14px 16px;
  font-size: 17px;
  border: 1px solid #dddfe2;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s ease;
  color: #1c1e21;
  background: #fff;

  &::placeholder {
    color: #8a8d91;
  }

  &:focus {
    border-color: #0ea5e9;
    box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.15);
  }
}

.password-group {
  position: relative;
}

.toggle-pw {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #8a8d91;
  cursor: pointer;
  display: flex;
  align-items: center;

  &:hover {
    color: #1c1e21;
  }
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #0ea5e9;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 6px;
  transition: background-color 0.2s ease;

  &:hover:not(:disabled) {
    background: #0284c7;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.forgot-link {
  display: block;
  text-align: center;
  margin: 16px 0;
  color: #0ea5e9;
  text-decoration: none;
  font-size: 14px;

  &:hover {
    text-decoration: underline;
  }
}

.divider {
  border-top: 1px solid #dadde1;
  margin: 20px 0;
}

.create-btn {
  display: block;
  width: fit-content;
  margin: 0 auto;
  padding: 12px 20px;
  background: #42b72a;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: #36a420;
  }
}

// ========== 响应式 - 移动端 ==========
@media (max-width: 900px) {
  .container {
    flex-direction: column;
    gap: 32px;
    text-align: center;
  }

  .left-section {
    max-width: 100%;
  }

  .brand-logo {
    font-size: 40px;
  }

  .brand-subtitle {
    font-size: 20px;
  }

  .right-section {
    max-width: 100%;
    width: 100%;
  }
}
</style>

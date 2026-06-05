<template>
  <div class="settings-page">
    <div class="settings-container">
      <!-- 返回按钮 -->
      <div class="page-header">
        <button class="back-btn" @click="$router.push('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          返回
        </button>
        <h1 class="page-title">设置</h1>
      </div>

      <div class="settings-sections">
        <!-- 场景选择 -->
        <section class="setting-section">
          <h3 class="section-label">对话场景</h3>
          <div class="scene-grid">
            <button
              v-for="scene in allScenes"
              :key="scene.value"
              :class="['scene-card', { active: form.scene === scene.value }]"
              @click="form.scene = scene.value"
            >
              <!-- 删除按钮（hover 显示） -->
              <span
                class="delete-btn"
                @click.stop="confirmDelete(scene)"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <path d="M18 6L6 18M6 6l12 12"/>
                </svg>
              </span>
              <!-- 自定义场景：彩色首字母头像 -->
              <span
                v-if="isCustomIcon(scene.icon)"
                class="scene-icon letter-avatar"
                :style="{ background: getIconColor(scene.icon) }"
              >{{ getIconLetter(scene.icon) }}</span>
              <!-- 内置场景：emoji -->
              <span v-else class="scene-icon">{{ scene.icon }}</span>
              <span class="scene-name">{{ scene.label }}</span>
            </button>
            <!-- 自定义按钮 -->
            <button
              class="scene-card custom-card"
              @click="showModal = true"
            >
              <span class="scene-icon custom-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </span>
              <span class="scene-name">自定义</span>
            </button>
          </div>
        </section>

        <!-- 选中场景详情 -->
        <Transition name="detail-fade">
          <section v-if="selectedScene" class="setting-section detail-section">
            <div class="detail-header">
              <span v-if="isCustomIcon(selectedScene.icon)" class="detail-icon letter-avatar" :style="{ background: getIconColor(selectedScene.icon) }">{{ getIconLetter(selectedScene.icon) }}</span>
              <span v-else class="detail-icon">{{ selectedScene.icon }}</span>
              <span class="detail-title">{{ selectedScene.label }}</span>
            </div>

            <div class="detail-body">
              <div class="detail-field" @click="startEdit('description')">
                <label class="field-label">场景描述</label>
                <div v-if="editingField !== 'description'" class="field-content editable-text" :class="{ empty: !editDescription }">
                  {{ editDescription || '点击编辑场景描述...' }}
                </div>
                <textarea
                  v-else
                  ref="descTextareaRef"
                  v-model="editDescription"
                  class="field-content editable-input"
                  placeholder="描述这个场景的背景、目标和适用情况..."
                  rows="3"
                  maxlength="200"
                  @click.stop
                  @blur="editingField = null"
                ></textarea>
              </div>
              <div class="detail-field" @click="startEdit('roleSetting')">
                <label class="field-label">角色设定</label>
                <div v-if="editingField !== 'roleSetting'" class="field-content editable-text" :class="{ empty: !editRoleSetting }">
                  {{ editRoleSetting || '点击编辑角色设定...' }}
                </div>
                <textarea
                  v-else
                  ref="roleTextareaRef"
                  v-model="editRoleSetting"
                  class="field-content editable-input"
                  placeholder="设定AI在这个场景中的角色..."
                  rows="2"
                  maxlength="200"
                  @click.stop
                  @blur="editingField = null"
                ></textarea>
              </div>
            </div>
          </section>
        </Transition>

        <!-- 难度等级 -->
        <section class="setting-section">
          <h3 class="section-label">难度等级</h3>
          <div class="difficulty-options">
            <button
              v-for="level in difficulties"
              :key="level.value"
              :class="['diff-btn', { active: form.difficulty === level.value }]"
              @click="form.difficulty = level.value"
            >
              {{ level.label }}
            </button>
          </div>
        </section>

        <!-- 语速调节 -->
        <section class="setting-section">
          <div class="slider-header">
            <h3 class="section-label">AI 语音速度</h3>
            <span class="speed-value">{{ form.speed }}x</span>
          </div>
          <input
            type="range"
            v-model.number="form.speed"
            min="0.5"
            max="2"
            step="0.1"
            class="speed-slider"
          />
          <div class="slider-marks">
            <span>慢</span>
            <span>正常</span>
            <span>快</span>
          </div>
        </section>

        <!-- 保存按钮 -->
        <div class="action-area">
          <button class="save-btn" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存设置' }}</button>
        </div>
      </div>
    </div>

    <!-- 自定义场景弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">新建自定义场景</h2>
              <button class="modal-close" @click="showModal = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M18 6L6 18M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <label class="input-label">场景名称</label>
              <div class="input-row">
                <input
                  v-model="customSceneName"
                  ref="inputRef"
                  type="text"
                  class="scene-input"
                  placeholder="例如：酒店入住、医院挂号..."
                  maxlength="20"
                  @keyup.enter="handleConfirm"
                />
                <span
                  v-if="customSceneName.trim()"
                  class="avatar-preview"
                  :style="{ background: previewColor }"
                >{{ previewLetter }}</span>
              </div>

              <label class="input-label" style="margin-top: 14px;">场景描述</label>
              <textarea
                v-model="customDescription"
                class="scene-textarea"
                placeholder="描述这个场景的背景、目标和适用情况..."
                rows="3"
                maxlength="200"
              ></textarea>

              <label class="input-label" style="margin-top: 14px;">角色设定</label>
              <textarea
                v-model="customRoleSetting"
                class="scene-textarea"
                placeholder="设定AI在这个场景中的角色，例如：你是一位友好的酒店前台接待员..."
                rows="2"
                maxlength="200"
              ></textarea>
              <p class="input-hint">填写描述与角色设定后点击确认</p>
            </div>
            <div class="modal-footer">
              <button class="btn-cancel" @click="showModal = false">取消</button>
              <button
                class="btn-confirm"
                :disabled="!customSceneName.trim()"
                @click="handleConfirm"
              >确认添加</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
          <div class="modal-container modal-sm">
            <div class="modal-body" style="text-align: center; padding: 28px 22px;">
              <p style="font-size: 15px; color: var(--color-text-primary); margin-bottom: 6px;">
                确定要删除场景「<strong>{{ deleteTarget?.label }}</strong>」吗？
              </p>
              <p style="font-size: 12.5px; color: var(--color-text-tertiary);">删除后无法恢复</p>
            </div>
            <div class="modal-footer" style="justify-content: center; gap: 12px;">
              <button class="btn-cancel" @click="showDeleteConfirm = false">取消</button>
              <button class="btn-danger" @click="handleDelete">确认删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, nextTick, watch, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { createScene, getSettings, saveSettings, deleteScene } from '@/api/scenes'
import type { SceneItem } from '@/api/scenes'

const store = useAppStore()

// 彩色头像色池（低饱和，与整体设计协调）
const colorPool = [
  '#6366f1', // 靛蓝
  '#8b5cf6', // 紫罗兰
  '#06b6d4', // 青
  '#10b981', // 翠绿
  '#f59e0b', // 琥珀
  '#ef4444', // 红
  '#ec4899', // 粉
  '#3b82f6', // 蓝
]

// 弹窗预览：取首字 + 按已创建数量分配颜色
const previewLetter = computed(() => {
  const name = customSceneName.value.trim()
  return name ? name.charAt(0).toUpperCase() : ''
})

const previewColor = computed(() => {
  const builtinCount = builtinScenes.value.length
  const customCount = allScenes.value.length - builtinCount
  return colorPool[customCount % colorPool.length]
})

const form = reactive({
  scene: '',
  difficulty: 'intermediate',
  speed: 1.0
})

// 设置页内使用的场景项类型（统一内置和自定义）
interface SettingsSceneItem {
  value: string
  label: string
  icon: string
  description?: string
  roleSetting?: string
  difficulty?: number
}

// 内置场景（从 store.scenes 中筛选 isBuiltin=true）
const builtinScenes = computed<SettingsSceneItem[]>(
  () => store.scenes.filter(s => s.isBuiltin).map(s => ({
    value: String(s.sceneId),
    label: s.sceneName,
    icon: s.icon,
    description: s.description,
    roleSetting: s.roleSetting,
    difficulty: s.difficulty
  }))
)

// 用户自建的场景列表
const customScenes = computed<SettingsSceneItem[]>(
  () => store.scenes.filter(s => !s.isBuiltin).map(s => ({
    value: String(s.sceneId),
    label: s.sceneName,
    icon: s.icon,
    description: s.description,
    roleSetting: s.roleSetting,
    difficulty: s.difficulty
  }))
)

// 全部场景：内置 + 自定义
const allScenes = computed(() => [...builtinScenes.value, ...customScenes.value])

// 当前选中的场景（含详情数据）
const selectedScene = computed(() =>
  allScenes.value.find((s) => s.value === form.scene) || null
)

// 详情面板内联编辑状态
const editingField = ref<'description' | 'roleSetting' | null>(null)
const editDescription = ref('')
const editRoleSetting = ref('')
const descTextareaRef = ref<HTMLTextAreaElement | null>(null)
const roleTextareaRef = ref<HTMLTextAreaElement | null>(null)

// 选中场景变化时，同步编辑值
watch(selectedScene, (scene) => {
  if (scene) {
    editDescription.value = scene.description || ''
    editRoleSetting.value = scene.roleSetting || ''
    editingField.value = null
  }
}, { immediate: true })

function startEdit(field: 'description' | 'roleSetting') {
  editingField.value = field
  nextTick(() => {
    const el = field === 'description' ? descTextareaRef.value : roleTextareaRef.value
    el?.focus()
  })
}

const difficulties = [
  { value: 'beginner', label: '初级' },
  { value: 'intermediate', label: '中级' },
  { value: 'advanced', label: '高级' }
]

// 弹窗状态
const showModal = ref(false)
const customSceneName = ref('')
const customDescription = ref('')
const customRoleSetting = ref('')
const inputRef = ref<HTMLInputElement | null>(null)
const creating = ref(false) // 创建中防重复提交
const saving = ref(false) // 保存设置中

// 删除确认弹窗
const showDeleteConfirm = ref(false)
const deleteTarget = ref<SettingsSceneItem | null>(null)
const deleting = ref(false)

watch(showModal, (val) => {
  if (val) {
    customSceneName.value = ''
    customDescription.value = ''
    customRoleSetting.value = ''
    nextTick(() => inputRef.value?.focus())
  }
})

// 页面加载时从后端拉取场景列表 + 已存设置
onMounted(async () => {
  // 并行加载场景列表和用户设置
  const [scenesRes, settingsRes] = await Promise.allSettled([
    store.scenes.length === 0 ? store.fetchScenes() : Promise.resolve(),
    getSettings(store.userId)
  ]) as [
    PromiseSettledResult<void>,
    PromiseSettledResult<{ code: number; data: import('@/api/scenes').UserSettings }>
  ]

  // 场景列表
  if (allScenes.value.length > 0) {
    // 优先用已保存的场景ID，否则默认第一个
    if (settingsRes.status === 'fulfilled' && settingsRes.value.data?.currentSceneId) {
      const savedSceneId = String(settingsRes.value.data.currentSceneId)
      if (allScenes.value.some(s => s.value === savedSceneId)) {
        form.scene = savedSceneId
      } else {
        form.scene = allScenes.value[0].value
      }
    } else {
      form.scene = allScenes.value[0].value
    }
  }

  // 用户设置（难度、语速）
  if (settingsRes.status === 'fulfilled' && settingsRes.value.data) {
    const s = settingsRes.value.data
    form.difficulty = s.difficulty || 'intermediate'
    form.speed = s.speechSpeed || 1.0
  }
})

async function handleConfirm() {
  const name = customSceneName.value.trim()
  if (!name || creating.value) return

  creating.value = true
  try {
    const res = await createScene({
      userId: store.userId,
      sceneName: name,
      description: customDescription.value.trim() || `自定义场景：${name}`,
      roleSetting: customRoleSetting.value.trim(),
      difficulty: 1
    })

    if (res.code === 200 && res.data) {
      // 将新场景追加到 store
      store.scenes.push(res.data)
      form.scene = String(res.data.sceneId)
      showModal.value = false
    }
  } catch (e) {
    console.error('创建自定义场景失败:', e)
  } finally {
    creating.value = false
  }
}

// 判断是否为内置场景（内置场景不可删除）
function isBuiltinScene(scene: SettingsSceneItem): boolean {
  return builtinScenes.value.some(s => s.value === scene.value)
}

// 弹出删除确认框
function confirmDelete(scene: SettingsSceneItem) {
  deleteTarget.value = scene
  showDeleteConfirm.value = true
}

// 执行删除
async function handleDelete() {
  if (!deleteTarget.value || deleting.value) return
  deleting.value = true

  try {
    await deleteScene(Number(deleteTarget.value.value))
    // 从 store 中移除
    const idx = store.scenes.findIndex(s => s.sceneId === Number(deleteTarget.value!.value))
    if (idx !== -1) store.scenes.splice(idx, 1)
    // 如果删除的是当前选中场景，切换到第一个
    if (form.scene === deleteTarget.value?.value && allScenes.value.length > 0) {
      form.scene = allScenes.value[0].value
    }
    showDeleteConfirm.value = false
    deleteTarget.value = null
  } catch (e) {
    console.error('删除场景失败:', e)
    alert('删除失败，请重试')
  } finally {
    deleting.value = false
  }
}

async function handleSave() {
  if (saving.value) return
  saving.value = true

  const selected = allScenes.value.find((s) => s.value === form.scene)
  store.currentScene = selected?.label || ''

  try {
    // 一次性保存：用户设置 + 场景描述/角色设定
    await saveSettings({
      userId: store.userId,
      currentSceneId: selected ? Number(selected.value) : null,
      difficulty: form.difficulty,
      speechSpeed: form.speed,
      sceneId: selected ? Number(selected.value) : null,
      description: editDescription.value,
      roleSetting: editRoleSetting.value
    })

    // 同步更新本地 store 中的场景数据
    if (selected) {
      const storeScene = store.scenes.find(s => s.sceneId === Number(selected.value))
      if (storeScene) {
        storeScene.description = editDescription.value
        storeScene.roleSetting = editRoleSetting.value
      }
    }

    alert('设置已保存')
  } catch (e) {
    console.error('保存设置失败:', e)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 判断是否为自定义图标格式（首字|颜色）
function isCustomIcon(icon: string): boolean {
  return icon.includes('|')
}

function getIconLetter(icon: string): string {
  return icon.split('|')[0] || '?'
}

function getIconColor(icon: string): string {
  return icon.split('|')[1] || '#6366f1'
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: var(--color-bg-secondary);
  padding: 40px;
}

.settings-container {
  max-width: 560px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 36px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
    border-color: var(--color-border-hover);
  }
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.setting-section {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

// 场景网格
.scene-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.scene-card {
  padding: 14px 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  position: relative;

  // 删除按钮（hover 显示，右上角）
  .delete-btn {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--color-bg-tertiary);
    color: var(--color-text-tertiary);
    opacity: 0;
    transition: all 0.15s;
    pointer-events: none;

    &:hover {
      background: #fee2e2;
      color: #ef4444;
    }
  }

  &:hover .delete-btn {
    opacity: 1;
    pointer-events: auto;
  }

  .scene-icon {
    font-size: 22px;
    display: block;
    margin-bottom: 6px;
  }

  .scene-name {
    font-size: 12.5px;
    color: var(--color-text-secondary);
    font-weight: 500;
  }

  &:hover {
    border-color: var(--color-border-hover);
    background: var(--color-bg-hover);
  }

  &.active {
    border-color: var(--color-accent-muted);
    background: var(--color-accent-light);

    .scene-name {
      color: var(--color-accent);
    }
  }

  &.custom-card {
    border-style: dashed;
    border-color: var(--color-border-hover);

    .custom-icon {
      width: 36px;
      height: 36px;
      margin: 0 auto 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-bg-tertiary);
      border-radius: 8px;
      color: var(--color-text-tertiary);
      transition: all 0.2s;
    }

    &:hover {
      border-color: var(--color-accent-muted);

      .custom-icon {
        background: var(--color-accent-subtle);
        color: var(--color-accent);
      }
    }
  }
}

// 难度选项
.difficulty-options {
  display: flex;
  gap: 8px;
}

.diff-btn {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-bg-hover);
  }

  &.active {
    border-color: var(--color-accent);
    background: var(--color-accent-light);
    color: var(--color-accent);
  }
}

// 滑块样式
.slider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;

  .section-label {
    margin-bottom: 0;
  }

  .speed-value {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-accent);
    font-family: 'SF Mono', monospace;
  }
}

.speed-slider {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--color-border);
  border-radius: 3px;
  outline: none;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--color-accent);
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(79, 70, 229, 0.25);
    transition: transform 0.15s;

    &:hover {
      transform: scale(1.15);
    }
  }
}

.slider-marks {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--color-text-tertiary);
}

.action-area {
  margin-top: 28px;
  text-align: right;
}

.save-btn {
  padding: 11px 28px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-accent-hover);
  }
}

/* ====== 弹窗样式 ====== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.35);
  backdrop-filter: blur(4px);
}

.modal-container {
  width: 420px;
  max-width: 90vw;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 0;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.modal-close {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--color-bg-tertiary);
    color: var(--color-text-secondary);
  }
}

.modal-body {
  padding: 18px 22px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.scene-input {
  width: 100%;
  padding: 10px 13px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
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

// 输入行（输入框 + 头像预览）
.input-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .scene-input {
    flex: 1;
  }
}

// 弹窗内头像预览
.avatar-preview {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  animation: avatarPop 0.25s ease-out;
}

@keyframes avatarPop {
  from { transform: scale(0.6); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}

// 场景卡片内的彩色首字母头像
.letter-avatar {
  font-size: 20px !important;
  width: 28px;
  height: 28px;
  margin: 0 auto 6px !important;
  border-radius: 7px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 22px 18px;
}

.btn-cancel,
.btn-confirm {
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-secondary);

  &:hover {
    background: var(--color-bg-tertiary);
  }
}

.btn-confirm {
  border: none;
  background: var(--color-accent);
  color: #fff;

  &:hover:not(:disabled) {
    background: var(--color-accent-hover);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.btn-danger {
  border: none;
  background: #ef4444;
  color: #fff;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #dc2626;
  }
}

.modal-sm {
  width: 340px;
}

/* 过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;

  .modal-container {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;

  .modal-container {
    transform: translateY(12px) scale(0.97);
    opacity: 0;
  }
}

/* ====== 场景详情面板 ====== */
.detail-section {
  border-color: var(--color-border);
  background: #fff;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  margin-bottom: 16px;
  width: fit-content;

  .detail-icon {
    font-size: 22px;
    flex-shrink: 0;
    line-height: 1;

    &.letter-avatar {
      width: 28px;
      height: 28px;
      margin: 0;
      border-radius: 6px;
      display: inline-flex !important;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: 600;
      font-size: 14px !important;
    }
  }

  .detail-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    line-height: 1.2;
  }
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-field {
  .field-label {
    display: block;
    font-size: 11.5px;
    font-weight: 600;
    color: var(--color-text-tertiary);
    text-transform: uppercase;
    letter-spacing: 0.04em;
    margin-bottom: 5px;
  }

  .field-content {
    font-size: 13.5px;
    line-height: 1.6;
    color: var(--color-text-primary);
    margin: 0;
    position: relative;
  }
}

/* 详情面板过渡动画 */
.detail-fade-enter-active,
.detail-fade-leave-active {
  transition: all 0.25s ease;
}

.detail-fade-enter-from,
.detail-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 弹窗内文本框 */
.scene-textarea {
  width: 100%;
  padding: 10px 13px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  outline: none;
  resize: vertical;
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;

  &::placeholder {
    color: var(--color-text-tertiary);
  }

  &:focus {
    border-color: var(--color-accent);
    box-shadow: 0 0 0 3px var(--color-accent-subtle);
  }
}

/* ====== 内联编辑样式 ====== */
.editable-text {
  cursor: text;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background-color 0.15s, color 0.15s;

  &:hover {
    background: rgba(79, 70, 229, 0.04);
    color: var(--color-text-primary);
  }

  &.empty {
    color: var(--color-text-tertiary);

    &:hover {
      color: var(--color-text-secondary);
    }
  }

  &::after {
    content: '';
    position: absolute;
    right: 6px;
    top: 50%;
    transform: translateY(-50%);
    width: 14px;
    height: 14px;
    opacity: 0;
    transition: opacity 0.15s;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7'%3E%3C/path%3E%3Cpath d='M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z'%3E%3C/path%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: center;
  }

  &:hover::after {
    opacity: 1;
  }
}

.editable-input {
  width: 100%;
  padding: 8px 10px;
  border: 1.5px solid var(--color-accent) !important;
  border-radius: 6px !important;
  font-size: 13.5px !important;
  line-height: 1.55 !important;
  color: var(--color-text-primary) !important;
  background: #fff !important;
  outline: none;
  resize: vertical;
  box-shadow: 0 0 0 3px var(--color-accent-subtle) !important;
  box-sizing: border-box;
  font-family: inherit;
}
</style>

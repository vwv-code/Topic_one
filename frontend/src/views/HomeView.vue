<template>
  <div class="app-layout">
    <!-- 左侧侧边栏 -->
    <Sidebar />

    <!-- 右侧主内容区 -->
    <main class="main-area">
      <!-- 顶部导航栏 -->
      <Header />

      <!-- 中间内容区 - AI虚拟人 -->
      <ContentArea />
    </main>

    <!-- 底部语音输入栏 -->
    <VoiceInput />

    <!-- 发音评测面板（右侧滑出） -->
    <PronunciationPanel />
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import ContentArea from '@/components/layout/ContentArea.vue'
import VoiceInput from '@/components/layout/VoiceInput.vue'
import PronunciationPanel from '@/components/layout/PronunciationPanel.vue'
</script>

<style lang="scss" scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.main-area {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
</style>

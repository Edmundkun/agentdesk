<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps<{ content: string }>()

marked.setOptions({ gfm: true, breaks: true })

// 外链统一新窗口打开，同时阻断 window.opener
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.nodeName === 'A') {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

const html = computed<string>(() => {
  const parsed = marked.parse(props.content, { async: false, gfm: true, breaks: true })
  return DOMPurify.sanitize(typeof parsed === 'string' ? parsed : '')
})
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div class="md-body" v-html="html"></div>
</template>

<style scoped>
.md-body :deep(pre) {
  position: relative;
}
</style>

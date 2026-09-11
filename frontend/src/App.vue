<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { Camera, Check, ChevronRight, ImagePlus, RotateCcw, ScanFace, ShieldCheck, Sparkles, TriangleAlert } from 'lucide-vue-next'
import { saveAnalysis } from './api'
import { cameraErrorMessage } from './camera'
import { createFaceAnalyzer, metricLabels, stabilityOf } from './face-analysis'

const video = ref()
const canvas = ref()
const photo = ref()
const fileInput = ref()
const mode = ref('empty')
const busy = ref(false)
const error = ref('')
const analysis = ref()
const recommendations = ref([])
const samples = ref([])
const stream = ref()
const stability = computed(() => stabilityOf(samples.value))

async function startCamera() {
  reset(false)
  if (!navigator.mediaDevices?.getUserMedia) {
    error.value = cameraErrorMessage(null)
    return
  }
  let mediaStream
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 1280 } }, audio: false })
  } catch (cause) {
    error.value = cameraErrorMessage(cause)
    return
  }
  try {
    stream.value = mediaStream
    mode.value = 'camera'
    await nextTick()
    video.value.srcObject = stream.value
    await video.value.play()
  } catch (cause) {
    stopCamera()
    mode.value = 'empty'
    error.value = `摄像头已连接，但预览播放失败${cause?.name ? `（${cause.name}）` : ''}，请刷新页面重试`
  }
}

function stopCamera() {
  stream.value?.getTracks().forEach(track => track.stop())
  stream.value = undefined
}

async function capture() {
  canvas.value.width = video.value.videoWidth
  canvas.value.height = video.value.videoHeight
  const context = canvas.value.getContext('2d')
  context.translate(canvas.value.width, 0)
  context.scale(-1, 1)
  context.drawImage(video.value, 0, 0)
  stopCamera()
  mode.value = 'photo'
  await analyze(canvas.value)
}

function chooseFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
    error.value = '请选择 10MB 以内的 JPG、PNG 或 WebP 图片'
    return
  }
  stopCamera()
  photo.value.src = URL.createObjectURL(file)
  photo.value.onload = async () => {
    mode.value = 'photo'
    await analyze(photo.value)
    URL.revokeObjectURL(photo.value.src)
  }
}

async function useSample() {
  stopCamera()
  photo.value.src = '/sample-face.png'
  photo.value.onload = async () => {
    mode.value = 'photo'
    await analyze(photo.value)
  }
}

async function analyze(source) {
  busy.value = true
  error.value = ''
  recommendations.value = []
  try {
    const analyzer = await createFaceAnalyzer()
    analysis.value = analyzer(source)
    if (analysis.value.quality.passed) samples.value.push(analysis.value.metrics)
    if (!analysis.value.quality.passed) return
    const saved = await saveAnalysis(analysis.value.metrics, analysis.value.quality)
    recommendations.value = saved.recommendations
  } catch (cause) {
    error.value = cause.message || '分析失败，请换一张清晰正脸照片'
  } finally {
    busy.value = false
  }
}

function reset(clearSamples = true) {
  stopCamera()
  mode.value = 'empty'
  error.value = ''
  analysis.value = undefined
  recommendations.value = []
  if (clearSamples) samples.value = []
  if (fileInput.value) fileInput.value.value = ''
}

onBeforeUnmount(stopCamera)
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <a class="brand" href="#" aria-label="妆鉴首页"><span>妆</span>妆鉴</a>
      <div class="privacy"><ShieldCheck :size="17" /> 图片仅在本机分析</div>
    </header>

    <main>
      <section class="intro">
        <p class="eyebrow"><Sparkles :size="15" /> MAKEUP REFERENCE</p>
        <h1>先读懂你的比例，<br><em>再找到值得参考的妆。</em></h1>
        <p>正脸、自然光、无遮挡。我们只比较五官比例，不评价美丑。</p>
      </section>

      <section class="workspace">
        <div class="capture-card">
          <div class="card-heading">
            <div><span>01</span><h2>添加一张正脸照片</h2></div>
            <button v-if="mode !== 'empty'" class="icon-button" aria-label="重新选择" @click="reset(false)"><RotateCcw :size="18" /></button>
          </div>

          <div class="media-stage" :class="{ active: mode !== 'empty' }">
            <div v-if="mode === 'empty'" class="empty-state">
              <div class="face-guide"><ScanFace :size="58" stroke-width="1.2" /></div>
              <strong>保持正脸，露出完整下颌线</strong>
              <span>建议使用无滤镜、光线均匀的照片</span>
              <div class="primary-actions">
                <button class="primary" @click="startCamera"><Camera :size="18" /> 打开摄像头</button>
                <button class="secondary" @click="fileInput.click()"><ImagePlus :size="18" /> 选择照片</button>
              </div>
              <button class="sample-link" @click="useSample">没有合适照片？体验示例</button>
            </div>
            <video v-show="mode === 'camera'" ref="video" autoplay playsinline muted></video>
            <button v-if="mode === 'camera'" class="shutter" aria-label="拍照" @click="capture"><span /></button>
            <img v-show="mode === 'photo'" ref="photo" alt="待分析的正脸照片" />
            <canvas v-show="mode === 'photo'" ref="canvas" aria-label="拍摄的正脸照片" />
            <div v-if="busy" class="scanning"><span /><p>正在定位面部关键点…</p></div>
          </div>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="chooseFile">

          <div v-if="error" class="notice error"><TriangleAlert :size="18" /><span>{{ error }}</span></div>
          <div v-if="analysis" class="quality-row">
            <div class="quality-score" :class="{ passed: analysis.quality.passed }">{{ analysis.quality.score }}</div>
            <div>
              <strong>{{ analysis.quality.passed ? '照片质量合格' : '建议重新拍摄' }}</strong>
              <p>{{ analysis.quality.issues.join(' · ') || '清晰度、光线与角度均符合分析要求' }}</p>
            </div>
          </div>
        </div>

        <aside class="guide-card">
          <p class="step-label">拍摄检查</p>
          <div class="guide-list">
            <div><span>光</span><p><strong>柔和自然光</strong><small>避免背光、过曝与彩色灯光</small></p></div>
            <div><span>正</span><p><strong>镜头与眼睛平齐</strong><small>不仰头，不低头，嘴唇自然闭合</small></p></div>
            <div><span>净</span><p><strong>无遮挡、无滤镜</strong><small>摘下眼镜，头发不要遮住脸侧</small></p></div>
          </div>
          <div class="stability-panel">
            <div><span>稳定性对比</span><strong>{{ samples.length }} / 3</strong></div>
            <div class="progress"><i :style="{ width: `${Math.min(100, samples.length / 3 * 100)}%` }" /></div>
            <p>{{ stability.message }}</p>
          </div>
        </aside>
      </section>

      <section v-if="analysis" class="results">
        <div class="section-title"><div><span>02</span><h2>你的面部比例</h2></div><p>比例越稳定，推荐越有参考价值</p></div>
        <div class="metric-grid">
          <div v-for="(value, key) in analysis.metrics" :key="key" class="metric">
            <span>{{ metricLabels[key] }}</span><strong>{{ value.toFixed(3) }}</strong>
            <i><b :style="{ width: `${Math.min(92, Math.max(18, value * (key === 'faceLengthWidth' ? 42 : 120)))}%` }" /></i>
          </div>
        </div>
      </section>

      <section v-if="recommendations.length" class="recommendations">
        <div class="section-title"><div><span>03</span><h2>更值得参考的博主</h2></div><p>按部位匹配，不必照搬整张脸</p></div>
        <div class="blogger-grid">
          <article v-for="item in recommendations" :key="item.id" class="blogger-card">
            <div class="portrait-wrap"><img v-if="item.avatarUrl" :src="item.avatarUrl" :alt="`${item.displayName} 的内容封面`"><div v-else class="portrait-placeholder">{{ item.platform }}</div><span>第 {{ item.rank }} 名</span><b>{{ item.score }}% 相关</b></div>
            <div class="blogger-body">
              <div class="blogger-name"><div><h3>{{ item.displayName }}</h3><small>{{ item.platform }}</small></div><strong>{{ item.bestPart }}</strong></div>
              <div class="tags"><span v-for="tag in item.styleTags" :key="tag">{{ tag }}</span></div>
              <ul><li v-for="reason in item.reasons" :key="reason"><Check :size="15" />{{ reason }}</li></ul>
              <p class="focus">建议重点看：{{ item.focus }}</p>
              <a :href="item.platformUrl" target="_blank" rel="noopener noreferrer">去看她的参考视频 <ChevronRight :size="17" /></a>
            </div>
          </article>
        </div>
        <p class="disclaimer">推荐来自公开网页搜索，匹配分表示检索相关度，不代表对博主本人面部比例的测量。</p>
      </section>
    </main>
    <footer>妆鉴 MAKEUP REFERENCE <span>·</span> 不保存原始照片</footer>
  </div>
</template>

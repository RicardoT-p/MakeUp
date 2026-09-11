import { FaceLandmarker, FilesetResolver } from '@mediapipe/tasks-vision'

let landmarker

const point = (landmarks, index) => landmarks[index]
const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y)
const ratio = (value, base) => Number((value / base).toFixed(3))
const average = (values) => values.reduce((sum, value) => sum + value, 0) / values.length

export function sourceDimensions(source) {
  const width = source.videoWidth || source.naturalWidth || source.width
  const height = source.videoHeight || source.naturalHeight || source.height
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    throw new Error('无法读取照片尺寸，请重新拍摄')
  }
  return { width, height }
}

export async function createFaceAnalyzer() {
  if (!landmarker) {
    const vision = await FilesetResolver.forVisionTasks('/wasm')
    landmarker = await FaceLandmarker.createFromOptions(vision, {
      baseOptions: { modelAssetPath: '/models/face_landmarker.task', delegate: 'GPU' },
      runningMode: 'IMAGE',
      numFaces: 1,
      minFaceDetectionConfidence: 0.65,
      minFacePresenceConfidence: 0.65,
    })
  }
  return analyze
}

function imageQuality(source, landmarks, sourceWidth, sourceHeight) {
  const canvas = document.createElement('canvas')
  const width = 240
  const height = Math.max(1, Math.round(width * sourceHeight / sourceWidth))
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d', { willReadFrequently: true })
  context.drawImage(source, 0, 0, width, height)
  const pixels = context.getImageData(0, 0, width, height).data
  const gray = new Float32Array(width * height)
  let light = 0
  for (let i = 0, j = 0; i < pixels.length; i += 4, j++) {
    gray[j] = pixels[i] * .299 + pixels[i + 1] * .587 + pixels[i + 2] * .114
    light += gray[j]
  }
  light /= gray.length
  let edgeSum = 0
  let edgeSquared = 0
  let samples = 0
  for (let y = 1; y < height - 1; y += 2) {
    for (let x = 1; x < width - 1; x += 2) {
      const i = y * width + x
      const laplace = 4 * gray[i] - gray[i - 1] - gray[i + 1] - gray[i - width] - gray[i + width]
      edgeSum += laplace
      edgeSquared += laplace * laplace
      samples++
    }
  }
  const sharpness = edgeSquared / samples - (edgeSum / samples) ** 2
  const left = point(landmarks, 234)
  const right = point(landmarks, 454)
  const nose = point(landmarks, 1)
  const eyeLeft = midpoint(point(landmarks, 33), point(landmarks, 133))
  const eyeRight = midpoint(point(landmarks, 362), point(landmarks, 263))
  const yaw = Math.abs(distance(nose, left) - distance(nose, right)) / distance(left, right)
  const roll = Math.abs(Math.atan2(eyeRight.y - eyeLeft.y, eyeRight.x - eyeLeft.x) * 180 / Math.PI)
  const faceCoverage = distance(left, right) / sourceWidth
  const issues = []
  if (light < 55) issues.push('光线偏暗')
  if (light > 215) issues.push('面部过曝')
  if (sharpness < 45) issues.push('照片有些模糊')
  if (faceCoverage < .28) issues.push('请靠近镜头')
  if (yaw > .1) issues.push('请正对镜头')
  if (roll > 8) issues.push('请保持头部水平')
  return {
    passed: issues.length === 0,
    score: Math.max(0, Math.round(100 - issues.length * 17 - yaw * 80 - Math.max(0, roll - 2))),
    sharpness: Math.round(sharpness),
    brightness: Math.round(light),
    yaw: Number(yaw.toFixed(3)),
    roll: Number(roll.toFixed(1)),
    issues,
  }
}

function midpoint(a, b) {
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
}

function metricsOf(l) {
  const faceHeight = distance(point(l, 10), point(l, 152))
  const faceWidth = distance(point(l, 234), point(l, 454))
  const cheekWidth = faceWidth
  const jawWidth = distance(point(l, 172), point(l, 397))
  const eyeLeftWidth = distance(point(l, 33), point(l, 133))
  const eyeRightWidth = distance(point(l, 362), point(l, 263))
  const eyeLeftHeight = distance(point(l, 159), point(l, 145))
  const eyeRightHeight = distance(point(l, 386), point(l, 374))
  const eyebrowWidth = average([
    distance(point(l, 70), point(l, 107)),
    distance(point(l, 336), point(l, 300)),
  ])
  const eyebrowThickness = average([
    distance(point(l, 65), point(l, 55)),
    distance(point(l, 295), point(l, 285)),
  ])
  return {
    faceLengthWidth: ratio(faceHeight, faceWidth),
    jawCheekWidth: ratio(jawWidth, cheekWidth),
    upperThirdCheekWidth: ratio(distance(point(l, 10), point(l, 168)), cheekWidth),
    lowerThirdRatio: ratio(distance(point(l, 2), point(l, 152)), faceHeight),
    eyeSpacingFaceWidth: ratio(distance(point(l, 133), point(l, 362)), faceWidth),
    eyeAspectRatio: ratio(average([eyeLeftWidth, eyeRightWidth]), average([eyeLeftHeight, eyeRightHeight])),
    noseWidthRatio: ratio(distance(point(l, 129), point(l, 358)), faceWidth),
    eyebrowWidthRatio: ratio(eyebrowWidth, faceWidth),
    eyebrowThicknessRatio: ratio(eyebrowThickness, eyebrowWidth),
    mouthWidthRatio: ratio(distance(point(l, 61), point(l, 291)), faceWidth),
  }
}

function analyze(source) {
  const result = landmarker.detect(source)
  if (result.faceLandmarks.length !== 1) throw new Error('请确保画面中只有一张清晰、完整的脸')
  const landmarks = result.faceLandmarks[0]
  const { width, height } = sourceDimensions(source)
  const pixelLandmarks = landmarks.map(value => ({ x: value.x * width, y: value.y * height }))
  return {
    landmarks,
    metrics: metricsOf(pixelLandmarks),
    quality: imageQuality(source, pixelLandmarks, width, height),
  }
}

export function stabilityOf(samples) {
  if (samples.length < 3) return { stable: false, score: 0, message: `还需 ${3 - samples.length} 张合格照片` }
  const keys = Object.keys(samples[0])
  const variation = average(keys.map(key => {
    const values = samples.map(sample => sample[key])
    const mean = average(values)
    return Math.sqrt(average(values.map(value => (value - mean) ** 2))) / mean
  }))
  return {
    stable: variation < .045,
    score: Math.max(0, Math.round(100 - variation * 1000)),
    message: variation < .045 ? '多张照片数据稳定，可用于匹配' : '波动较大，请保持正脸和相同距离重拍',
  }
}

export const metricLabels = {
  faceLengthWidth: '脸部长宽比',
  jawCheekWidth: '下颌 / 颧骨宽',
  upperThirdCheekWidth: '上庭 / 颧骨宽',
  lowerThirdRatio: '下庭长度占比',
  eyeSpacingFaceWidth: '眼间距占脸宽',
  eyeAspectRatio: '双眼长宽比',
  noseWidthRatio: '鼻翼宽度占比',
  eyebrowWidthRatio: '眉宽占比',
  eyebrowThicknessRatio: '眉厚 / 眉宽',
  mouthWidthRatio: '嘴宽占比',
}

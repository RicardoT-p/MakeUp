export function cameraErrorMessage(error, secureContext = globalThis.isSecureContext) {
  if (!secureContext) return '摄像头只能在 HTTPS 或 localhost 页面使用'
  return {
    NotAllowedError: '浏览器已阻止摄像头，请在地址栏的网站权限中允许后重试',
    SecurityError: '浏览器安全设置阻止了摄像头访问',
    NotFoundError: '没有检测到可用摄像头',
    NotReadableError: '摄像头正被其他页面或应用占用，请关闭后重试',
    AbortError: '摄像头启动被中断，请关闭其他相机应用后重试',
    OverconstrainedError: '摄像头不支持所需的视频参数',
  }[error?.name] || `摄像头启动失败${error?.name ? `（${error.name}）` : ''}`
}


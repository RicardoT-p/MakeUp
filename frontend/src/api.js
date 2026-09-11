export async function saveAnalysis(metrics, quality) {
  const response = await fetch('/api/v1/analyses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ metrics, qualityScore: quality.score }),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new Error(problem?.detail || '推荐服务暂时不可用，请确认后端已启动')
  }
  return response.json()
}

const DEFAULT_DEV_API_BASE_URL = 'http://localhost:8081'

export function apiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim()
  if (configured) return configured.replace(/\/+$/, '')
  return import.meta.env.DEV ? DEFAULT_DEV_API_BASE_URL : ''
}

export function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  if (path.startsWith('//')) return `${window.location.protocol}${path}`
  return `${apiBaseUrl()}${path.startsWith('/') ? '' : '/'}${path}`
}

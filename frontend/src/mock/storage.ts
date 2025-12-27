// frontend/src/mock/storage.ts
export function safeParse<T>(raw: string | null, fallback: T): T {
  try {
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

export function getJSON<T>(key: string, fallback: T): T {
  return safeParse<T>(localStorage.getItem(key), fallback)
}

export function setJSON(key: string, value: unknown) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function nowIso() {
  return new Date().toISOString()
}

export function uid(prefix = 'id') {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

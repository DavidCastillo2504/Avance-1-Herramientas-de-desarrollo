// La URL del backend se fija al compilar (VITE_API_URL). Ver .env.development y AWS_DEPLOYMENT_GUIDE.md.
const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const SESSION_KEY = 'sabor_admin_session'

export const session = {
  get() {
    try {
      const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY))
      if (!stored?.token || new Date(stored.expiresAt) <= new Date()) return null
      return stored
    } catch {
      return null
    }
  },
  save(value) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(value))
  },
  clear() {
    sessionStorage.removeItem(SESSION_KEY)
  },
}

let unauthorizedHandler = () => {}
export function onUnauthorized(handler) {
  unauthorizedHandler = handler
  return () => { if (unauthorizedHandler === handler) unauthorizedHandler = () => {} }
}

async function request(path, { auth = false, body, headers = {}, ...options } = {}) {
  const finalHeaders = { ...headers }
  let finalBody = body
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = 'application/json'
    finalBody = JSON.stringify(body)
  }
  if (auth) {
    const current = session.get()
    if (current) finalHeaders.Authorization = `Bearer ${current.token}`
  }

  let response
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers: finalHeaders, body: finalBody })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Inténtalo nuevamente en unos momentos.')
  }

  if (auth && (response.status === 401 || response.status === 403)) {
    session.clear()
    unauthorizedHandler()
    throw new Error('Tu sesión expiró o no es válida. Inicia sesión nuevamente.')
  }
  if (response.status === 204) return null

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const fieldMessage = data?.fieldErrors && Object.values(data.fieldErrors)[0]
    throw new Error(fieldMessage || data?.message || 'Ocurrió un error inesperado')
  }
  return data
}

const admin = (path, options = {}) => request(path, { ...options, auth: true, cache: 'no-store' })

export const api = {
  // Público
  getMenu: () => request('/menu-items'),
  createOrder: (order) => request('/orders', { method: 'POST', body: order }),
  createReservation: (reservation) => request('/reservations', { method: 'POST', body: reservation }),
  login: (username, password) => request('/auth/login', { method: 'POST', body: { username, password } }),

  // Administración
  getAdminMenu: () => admin('/menu-items'),
  createMenuItem: (item) => admin('/menu-items', { method: 'POST', body: item }),
  updateMenuItem: (id, item) => admin(`/menu-items/${id}`, { method: 'PUT', body: item }),
  setMenuItemAvailability: (id, available) => admin(`/menu-items/${id}/availability`, {
    method: 'PATCH', body: { available },
  }),
  deleteMenuItem: (id) => admin(`/menu-items/${id}`, { method: 'DELETE' }),
  getImageStatus: () => admin('/images/status'),
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return admin('/images', { method: 'POST', body: form })
  },
  getOrders: () => admin('/orders?page=0&size=50'),
  getDashboard: () => admin('/orders/dashboard'),
  updateOrderStatus: (id, status) => admin(`/orders/${id}/status`, { method: 'PATCH', body: { status } }),
  getReservations: (upcoming = true) => admin(`/reservations?page=0&size=50&upcoming=${upcoming}`),
  updateReservationStatus: (id, status) => admin(`/reservations/${id}/status`, {
    method: 'PATCH', body: { status },
  }),
}

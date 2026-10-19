const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Verifica que el backend esté iniciado.')
  }

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const fieldMessage = data?.fieldErrors && Object.values(data.fieldErrors)[0]
    throw new Error(fieldMessage || data?.message || 'Ocurrió un error inesperado')
  }
  return data
}

export const api = {
  getMenu: () => request('/menu-items'),
  createOrder: (order) => request('/orders', { method: 'POST', body: JSON.stringify(order) }),
  getOrders: () => request('/orders?page=0&size=50'),
  getDashboard: () => request('/orders/dashboard'),
  updateOrderStatus: (id, status) => request(`/orders/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }),
}

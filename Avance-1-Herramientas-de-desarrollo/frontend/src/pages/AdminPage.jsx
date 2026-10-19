import { useCallback, useEffect, useState } from 'react'
import StatusMessage from '../components/StatusMessage'
import { api } from '../services/api'

const money = (value) => `S/ ${Number(value).toFixed(2)}`
const statusLabel = { PENDING: 'Pendiente', READY: 'Listo' }
const typeLabel = { TAKEAWAY: 'Para llevar', DINE_IN: 'Consumo en local' }

export default function AdminPage() {
  const [orders, setOrders] = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [orderPage, dashboard] = await Promise.all([api.getOrders(), api.getDashboard()])
      setOrders(orderPage.content)
      setStats(dashboard)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  async function toggleStatus(order) {
    setUpdatingId(order.id)
    setError('')
    try {
      const updated = await api.updateOrderStatus(order.id, order.status === 'PENDING' ? 'READY' : 'PENDING')
      setOrders((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <main className="section admin-section">
      <div className="container">
        <div className="section-heading">
          <div><span className="eyebrow">Panel administrativo</span><h1 className="admin-title">Gestión de pedidos</h1></div>
          <button className="btn btn-secondary" onClick={loadData} disabled={loading}>Actualizar</button>
        </div>
        <StatusMessage type="error">{error}</StatusMessage>
        {loading && <div className="loading-card">Consultando la operación…</div>}
        {!loading && stats && (
          <div className="stats-grid">
            <article><span>Pedidos registrados</span><strong>{stats.orderCount}</strong></article>
            <article><span>Platos disponibles</span><strong>{stats.availableMenuItemCount}</strong></article>
            <article><span>Ventas registradas</span><strong>{money(stats.registeredSales)}</strong></article>
          </div>
        )}
        {!loading && !error && orders.length === 0 && <div className="empty">No hay pedidos registrados.</div>}
        {!loading && orders.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead><tr><th>Pedido</th><th>Cliente</th><th>Detalle</th><th>Total</th><th>Estado</th><th>Acción</th></tr></thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td><strong>{order.id.slice(0, 8).toUpperCase()}</strong><br /><small>{new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(order.createdAt))}</small></td>
                    <td>{order.customerName}<br /><small>{order.phone}</small></td>
                    <td>{order.items.map((item) => <div key={item.menuItemId}>{item.quantity}× {item.name}</div>)}<small>{typeLabel[order.orderType]}</small></td>
                    <td><strong>{money(order.total)}</strong></td>
                    <td><span className={`status ${order.status === 'PENDING' ? 'pending' : ''}`}>{statusLabel[order.status]}</span></td>
                    <td><button className="small-btn" disabled={updatingId === order.id} onClick={() => toggleStatus(order)}>
                      {updatingId === order.id ? 'Guardando…' : order.status === 'PENDING' ? 'Marcar listo' : 'Reabrir'}
                    </button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </main>
  )
}

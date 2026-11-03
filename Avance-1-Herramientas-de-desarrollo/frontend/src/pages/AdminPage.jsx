import { useCallback, useEffect, useState } from 'react'
import StatusMessage from '../components/StatusMessage'
import { api } from '../services/api'

const money = (value) => `S/ ${Number(value).toFixed(2)}`
const orderStatusLabel = { PENDING: 'Pendiente', READY: 'Listo' }
const reservationStatusLabel = { PENDING: 'Pendiente', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada' }
const typeLabel = { TAKEAWAY: 'Para llevar', DINE_IN: 'Consumo en local' }

export default function AdminPage() {
  const [orders, setOrders] = useState([])
  const [reservations, setReservations] = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [orderPage, dashboard, reservationPage] = await Promise.all([
        api.getOrders(), api.getDashboard(), api.getReservations(),
      ])
      setOrders(orderPage.content)
      setStats(dashboard)
      setReservations(reservationPage.content)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  async function toggleOrderStatus(order) {
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

  async function changeReservationStatus(reservation, status) {
    setUpdatingId(reservation.id)
    setError('')
    try {
      const updated = await api.updateReservationStatus(reservation.id, status)
      setReservations((current) => current.map((item) => item.id === updated.id ? updated : item))
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
          <div><span className="eyebrow">Panel administrativo</span><h1 className="admin-title">Gestion del restaurante</h1></div>
          <button className="btn btn-secondary" onClick={loadData} disabled={loading}>Actualizar</button>
        </div>
        <StatusMessage type="error">{error}</StatusMessage>
        {loading && <div className="loading-card">Consultando la operacion...</div>}
        {!loading && stats && (
          <div className="stats-grid">
            <article><span>Pedidos registrados</span><strong>{stats.orderCount}</strong></article>
            <article><span>Platos disponibles</span><strong>{stats.availableMenuItemCount}</strong></article>
            <article><span>Ventas registradas</span><strong>{money(stats.registeredSales)}</strong></article>
          </div>
        )}

        {!loading && <section className="admin-block">
          <div className="section-heading"><div><span className="eyebrow">Cocina</span><h2>Pedidos</h2></div></div>
          {orders.length === 0 && <div className="empty">No hay pedidos registrados.</div>}
          {orders.length > 0 && <div className="table-wrap"><table className="data-table">
            <thead><tr><th>Pedido</th><th>Cliente</th><th>Detalle</th><th>Total</th><th>Estado</th><th>Accion</th></tr></thead>
            <tbody>{orders.map((order) => <tr key={order.id}>
              <td><strong>{order.id.slice(0, 8).toUpperCase()}</strong><br /><small>{new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(order.createdAt))}</small></td>
              <td>{order.customerName}<br /><small>{order.phone}</small></td>
              <td>{order.items.map((item) => <div key={item.menuItemId}>{item.quantity}x {item.name}</div>)}<small>{typeLabel[order.orderType]}</small></td>
              <td><strong>{money(order.total)}</strong></td>
              <td><span className={`status ${order.status === 'PENDING' ? 'pending' : ''}`}>{orderStatusLabel[order.status]}</span></td>
              <td><button className="small-btn" disabled={updatingId === order.id} onClick={() => toggleOrderStatus(order)}>{order.status === 'PENDING' ? 'Marcar listo' : 'Reabrir'}</button></td>
            </tr>)}</tbody>
          </table></div>}
        </section>}

        {!loading && <section className="admin-block">
          <div className="section-heading"><div><span className="eyebrow">Atencion al cliente</span><h2>Reservas</h2></div><strong>{reservations.length} registradas</strong></div>
          {reservations.length === 0 && <div className="empty">No hay reservas registradas.</div>}
          {reservations.length > 0 && <div className="table-wrap"><table className="data-table">
            <thead><tr><th>Reserva</th><th>Cliente</th><th>Fecha y hora</th><th>Personas</th><th>Indicaciones</th><th>Estado</th><th>Acciones</th></tr></thead>
            <tbody>{reservations.map((reservation) => <tr key={reservation.id}>
              <td><strong>{reservation.id.slice(0, 8).toUpperCase()}</strong></td>
              <td>{reservation.customerName}<br /><small>{reservation.phone}</small></td>
              <td>{new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' }).format(new Date(`${reservation.reservationDate}T12:00:00`))}<br /><strong>{reservation.reservationTime.slice(0, 5)}</strong></td>
              <td>{reservation.partySize}</td>
              <td>{reservation.notes || <small>Sin indicaciones</small>}</td>
              <td><span className={`status ${reservation.status.toLowerCase()}`}>{reservationStatusLabel[reservation.status]}</span></td>
              <td><div className="action-group">
                {reservation.status !== 'CONFIRMED' && <button className="small-btn" disabled={updatingId === reservation.id} onClick={() => changeReservationStatus(reservation, 'CONFIRMED')}>Confirmar</button>}
                {reservation.status !== 'CANCELLED' && <button className="small-btn" disabled={updatingId === reservation.id} onClick={() => changeReservationStatus(reservation, 'CANCELLED')}>Cancelar</button>}
                {reservation.status !== 'PENDING' && <button className="small-btn" disabled={updatingId === reservation.id} onClick={() => changeReservationStatus(reservation, 'PENDING')}>Pendiente</button>}
              </div></td>
            </tr>)}</tbody>
          </table></div>}
        </section>}
      </div>
    </main>
  )
}

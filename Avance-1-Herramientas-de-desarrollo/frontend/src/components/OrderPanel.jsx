import { useState } from 'react'
import { api } from '../services/api'
import StatusMessage from './StatusMessage'

const money = (value) => `S/ ${Number(value).toFixed(2)}`
const initialForm = { customerName: '', phone: '', orderType: 'TAKEAWAY', notes: '' }

export default function OrderPanel({ cart, onQuantity, onConfirmed }) {
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const total = cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
  const count = cart.reduce((sum, item) => sum + item.quantity, 0)

  const updateForm = ({ target }) => setForm((current) => ({ ...current, [target.name]: target.value }))

  async function submit(event) {
    event.preventDefault()
    setError('')
    if (!cart.length) {
      setError('Agrega al menos un plato al pedido.')
      return
    }
    setSubmitting(true)
    try {
      const order = await api.createOrder({
        ...form,
        items: cart.map((item) => ({ menuItemId: item.id, quantity: item.quantity })),
      })
      setForm(initialForm)
      onConfirmed(order)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section id="pedidos" className="section order-section">
      <div className="container two-col">
        <div>
          <span className="eyebrow">Pedidos online</span>
          <h2>Arma tu pedido para llevar ahora mismo</h2>
          <p className="muted">Agrega productos desde el menú. El servidor verifica su disponibilidad y calcula el total definitivo.</p>
          <div className="notice">✓ Pedido persistido en PostgreSQL y visible en administración.</div>
        </div>
        <div className="cart-panel">
          <div className="panel-title"><h3>Tu pedido</h3><span>{count} {count === 1 ? 'producto' : 'productos'}</span></div>
          <div className="cart-items">
            {cart.length === 0 && <div className="empty">Aún no agregas productos.</div>}
            {cart.map((item) => (
              <div className="cart-item" key={item.id}>
                <div><strong>{item.name}</strong><br /><small>{money(item.price)} c/u</small></div>
                <div className="qty">
                  <button type="button" onClick={() => onQuantity(item.id, -1)} aria-label={`Quitar ${item.name}`}>−</button>
                  <span>{item.quantity}</span>
                  <button type="button" onClick={() => onQuantity(item.id, 1)} aria-label={`Agregar ${item.name}`}>+</button>
                </div>
                <strong>{money(Number(item.price) * item.quantity)}</strong>
              </div>
            ))}
          </div>
          <div className="cart-total"><span>Total referencial</span><strong>{money(total)}</strong></div>
          <form className="form-grid" onSubmit={submit}>
            <label>Nombre del cliente<input name="customerName" value={form.customerName} onChange={updateForm}
              minLength="2" maxLength="100" autoComplete="name" required /></label>
            <label>Teléfono<input name="phone" type="tel" value={form.phone} onChange={updateForm}
              pattern="[0-9+() -]{7,20}" maxLength="20" autoComplete="tel" required /></label>
            <label>Modalidad<select name="orderType" value={form.orderType} onChange={updateForm}>
              <option value="TAKEAWAY">Para llevar</option>
              <option value="DINE_IN">Consumo en local</option>
            </select></label>
            <label>Indicaciones<textarea name="notes" value={form.notes} onChange={updateForm}
              maxLength="500" rows="3" placeholder="Opcional" /></label>
            <StatusMessage type="error">{error}</StatusMessage>
            <button className="btn btn-primary full" disabled={submitting || !cart.length}>
              {submitting ? 'Confirmando…' : 'Confirmar pedido'}
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}

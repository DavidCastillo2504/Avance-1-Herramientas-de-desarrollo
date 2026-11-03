import { useMemo, useState } from 'react'
import { api } from '../services/api'
import StatusMessage from './StatusMessage'

const initialForm = { customerName: '', phone: '', reservationDate: '', reservationTime: '', partySize: 2, notes: '' }

function localDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function ReservationPanel() {
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const minDate = useMemo(() => localDate(), [])
  const maxDate = useMemo(() => localDate(90), [])

  const updateForm = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))
    setConfirmation('')
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const reservation = await api.createReservation({ ...form, partySize: Number(form.partySize) })
      setForm(initialForm)
      setConfirmation(`Reserva ${reservation.id.slice(0, 8).toUpperCase()} registrada. Queda pendiente de confirmacion.`)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section id="reservas" className="section reservation-section">
      <div className="container two-col">
        <div>
          <span className="eyebrow">Reservas online</span>
          <h2>Separa tu mesa con anticipacion</h2>
          <p>Atendemos reservas de 12:00 a 22:00, para grupos de hasta 20 personas y con 90 dias de anticipacion.</p>
          <div className="reservation-details">
            <article><strong>Confirmacion clara</strong><span>Recibiras un codigo y el equipo revisara tu solicitud.</span></article>
            <article><strong>Cupo por horario</strong><span>La disponibilidad se valida al momento de reservar.</span></article>
          </div>
        </div>
        <div className="reservation-card">
          <div className="panel-title"><h3>Datos de la reserva</h3><span>Completa los datos solicitados</span></div>
          <form className="form-grid" onSubmit={submit}>
            <label>Nombre completo<input name="customerName" value={form.customerName} onChange={updateForm} minLength="2" maxLength="100" autoComplete="name" required /></label>
            <label>Telefono<input name="phone" type="tel" value={form.phone} onChange={updateForm} pattern="[0-9+() -]{7,20}" maxLength="20" autoComplete="tel" required /></label>
            <div className="form-row">
              <label>Fecha<input name="reservationDate" type="date" value={form.reservationDate} min={minDate} max={maxDate} onChange={updateForm} required /></label>
              <label>Hora<input name="reservationTime" type="time" value={form.reservationTime} min="12:00" max="22:00" step="1800" onChange={updateForm} required /></label>
            </div>
            <label>Numero de personas<input name="partySize" type="number" value={form.partySize} min="1" max="20" onChange={updateForm} required /></label>
            <label>Indicaciones<textarea name="notes" value={form.notes} onChange={updateForm} maxLength="500" rows="3" placeholder="Cumpleanos, silla para bebe u otra solicitud (opcional)" /></label>
            <StatusMessage type="error">{error}</StatusMessage>
            <StatusMessage type="success">{confirmation}</StatusMessage>
            <button className="btn btn-primary full" disabled={submitting}>{submitting ? 'Registrando...' : 'Solicitar reserva'}</button>
          </form>
        </div>
      </div>
    </section>
  )
}

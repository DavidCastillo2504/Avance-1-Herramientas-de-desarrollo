import { useState } from 'react'
import { api, session } from '../services/api'
import StatusMessage from './StatusMessage'

export default function LoginForm({ onLogin }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const update = ({ target }) => setForm((current) => ({ ...current, [target.name]: target.value }))

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await api.login(form.username, form.password)
      session.save(result)
      onLogin(result)
    } catch (requestError) {
      setError(requestError.message)
      setForm((current) => ({ ...current, password: '' }))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="section admin-section">
      <div className="container login-wrap">
        <div className="reservation-card login-card">
          <span className="eyebrow">Panel administrativo</span>
          <h1 className="login-title">Iniciar sesión</h1>
          <p className="muted-text">Acceso exclusivo para el personal del restaurante.</p>
          <form className="form-grid" onSubmit={submit}>
            <label>Usuario<input name="username" value={form.username} onChange={update}
              autoComplete="username" maxLength="100" required /></label>
            <label>Contraseña<input name="password" type="password" value={form.password} onChange={update}
              autoComplete="current-password" maxLength="200" required /></label>
            <StatusMessage type="error">{error}</StatusMessage>
            <button className="btn btn-primary full" disabled={submitting}>
              {submitting ? 'Ingresando…' : 'Ingresar'}
            </button>
          </form>
        </div>
      </div>
    </main>
  )
}

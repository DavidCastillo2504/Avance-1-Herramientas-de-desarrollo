import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api'
import DishImage from './DishImage'
import StatusMessage from './StatusMessage'

const money = (value) => `S/ ${Number(value).toFixed(2)}`
const emptyForm = { name: '', category: '', price: '', emoji: '', description: '', available: true, imageUrl: '' }

export default function MenuAdmin({ onChange }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadEnabled, setUploadEnabled] = useState(false)
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [menu, imageStatus] = await Promise.all([api.getAdminMenu(), api.getImageStatus()])
      setItems(menu)
      setUploadEnabled(imageStatus.uploadEnabled)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const categories = [...new Set(items.map((item) => item.category))]
  const update = ({ target }) => setForm((current) => ({
    ...current, [target.name]: target.type === 'checkbox' ? target.checked : target.value,
  }))

  function startEdit(item) {
    setEditingId(item.id)
    setForm({ ...item, price: String(item.price), imageUrl: item.imageUrl ?? '' })
    setError('')
    setNotice('')
    document.getElementById('menu-form')?.scrollIntoView({ behavior: 'smooth' })
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyForm)
  }

  async function uploadImage(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      setError('La imagen no puede superar 5 MB')
      return
    }
    setError('')
    setUploading(true)
    try {
      const result = await api.uploadImage(file)
      setForm((current) => ({ ...current, imageUrl: result.url }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setUploading(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSaving(true)
    const payload = {
      name: form.name, category: form.category, price: Number(form.price), emoji: form.emoji,
      description: form.description, available: form.available, imageUrl: form.imageUrl.trim() || null,
    }
    try {
      if (editingId) {
        const updated = await api.updateMenuItem(editingId, payload)
        setItems((current) => current.map((item) => item.id === updated.id ? updated : item))
        setNotice(`${updated.name} actualizado.`)
      } else {
        const created = await api.createMenuItem(payload)
        setItems((current) => [...current, created])
        setNotice(`${created.name} agregado al menú.`)
      }
      resetForm()
      onChange?.()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  async function toggleAvailability(item) {
    setBusyId(item.id)
    setError('')
    try {
      const updated = await api.setMenuItemAvailability(item.id, !item.available)
      setItems((current) => current.map((entry) => entry.id === updated.id ? updated : entry))
      onChange?.()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusyId(null)
    }
  }

  async function remove(item) {
    if (!window.confirm(`¿Eliminar "${item.name}" del menú?`)) return
    setBusyId(item.id)
    setError('')
    try {
      await api.deleteMenuItem(item.id)
      setItems((current) => current.filter((entry) => entry.id !== item.id))
      if (editingId === item.id) resetForm()
      onChange?.()
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="admin-block">
      <div className="section-heading"><div><span className="eyebrow">Carta</span><h2>Menú</h2></div>
        <strong>{items.length} platos</strong></div>
      <StatusMessage type="error">{error}</StatusMessage>
      <StatusMessage type="success">{notice}</StatusMessage>

      <form id="menu-form" className="menu-form" onSubmit={submit}>
        <h3>{editingId ? 'Editar plato' : 'Nuevo plato'}</h3>
        <div className="form-grid menu-form-grid">
          <label>Nombre<input name="name" value={form.name} onChange={update} maxLength="120" required /></label>
          <label>Categoría<input name="category" value={form.category} onChange={update} maxLength="60"
            list="menu-categories" required /></label>
          <datalist id="menu-categories">{categories.map((category) => <option key={category} value={category} />)}</datalist>
          <label>Precio (S/)<input name="price" type="number" min="0.10" max="9999.99" step="0.01" value={form.price}
            onChange={update} required /></label>
          <label>Emoji (si no hay foto)<input name="emoji" value={form.emoji} onChange={update} maxLength="16"
            placeholder="🍽️" /></label>
          <label className="span-2">Descripción<textarea name="description" value={form.description} onChange={update}
            maxLength="300" rows="2" required /></label>
          <label className="span-2">URL de la imagen (S3)<input name="imageUrl" type="url" value={form.imageUrl}
            onChange={update} maxLength="500" placeholder="https://tu-bucket.s3.us-east-1.amazonaws.com/menu/plato.jpg" /></label>
          <div className="span-2 image-row">
            <div className="image-preview"><DishImage dish={{ ...form, emoji: form.emoji || '🍽️' }} /></div>
            <div>
              {uploadEnabled
                ? <label className="small-btn upload-btn">{uploading ? 'Subiendo…' : 'Subir foto (JPG, PNG o WEBP, máx. 5 MB)'}
                    <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadImage}
                      disabled={uploading} hidden /></label>
                : <small className="muted-text">La carga directa está desactivada en el servidor. Sube la imagen al
                    bucket S3 (carpeta <code>menu/</code>) y pega aquí su URL.</small>}
              {form.imageUrl && <button type="button" className="small-btn" onClick={() => setForm((current) => ({ ...current, imageUrl: '' }))}>Quitar imagen</button>}
            </div>
          </div>
          <label className="checkbox"><input name="available" type="checkbox" checked={form.available}
            onChange={update} /> Disponible para pedidos</label>
        </div>
        <div className="action-group form-actions">
          <button className="btn btn-primary" disabled={saving || uploading}>{saving ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Agregar plato'}</button>
          {editingId && <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancelar</button>}
        </div>
      </form>

      {loading && <div className="loading-card">Cargando el menú…</div>}
      {!loading && items.length === 0 && <div className="empty">El menú está vacío.</div>}
      {!loading && items.length > 0 && <div className="table-wrap"><table className="data-table">
        <thead><tr><th>Plato</th><th>Categoría</th><th>Precio</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{items.map((item) => <tr key={item.id}>
          <td><div className="dish-cell"><div className="thumb"><DishImage dish={item} /></div>
            <div><strong>{item.name}</strong><br /><small>{item.description}</small></div></div></td>
          <td>{item.category}</td>
          <td><strong>{money(item.price)}</strong></td>
          <td><span className={`status ${item.available ? '' : 'cancelled'}`}>{item.available ? 'Disponible' : 'Agotado'}</span></td>
          <td><div className="action-group">
            <button className="small-btn" disabled={busyId === item.id} onClick={() => toggleAvailability(item)}>
              {item.available ? 'Marcar agotado' : 'Marcar disponible'}</button>
            <button className="small-btn" disabled={busyId === item.id} onClick={() => startEdit(item)}>Editar</button>
            <button className="small-btn danger" disabled={busyId === item.id} onClick={() => remove(item)}>Eliminar</button>
          </div></td>
        </tr>)}</tbody>
      </table></div>}
    </section>
  )
}

import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import MenuSection from '../components/MenuSection'
import OrderPanel from '../components/OrderPanel'
import StatusMessage from '../components/StatusMessage'
import { api } from '../services/api'

export default function HomePage() {
  const location = useLocation()
  const [menu, setMenu] = useState([])
  const [cart, setCart] = useState([])
  const [activeCategory, setActiveCategory] = useState('Todos')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [confirmation, setConfirmation] = useState('')

  useEffect(() => {
    let active = true
    api.getMenu()
      .then((data) => active && setMenu(data))
      .catch((requestError) => active && setError(requestError.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (location.hash) document.querySelector(location.hash)?.scrollIntoView({ behavior: 'smooth' })
  }, [location.hash])

  function addToCart(dish) {
    setConfirmation('')
    setCart((current) => {
      const existing = current.find((item) => item.id === dish.id)
      if (existing) return current.map((item) => item.id === dish.id && item.quantity < 20
        ? { ...item, quantity: item.quantity + 1 }
        : item)
      return [...current, { ...dish, quantity: 1 }]
    })
  }

  function changeQuantity(id, delta) {
    setCart((current) => current
      .map((item) => item.id === id ? { ...item, quantity: Math.min(20, item.quantity + delta) } : item)
      .filter((item) => item.quantity > 0))
  }

  function orderConfirmed(order) {
    setCart([])
    setConfirmation(`Pedido ${order.id.slice(0, 8).toUpperCase()} registrado correctamente por ${Number(order.total).toFixed(2)} soles.`)
    document.getElementById('pedidos')?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <main>
      <section id="inicio" className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow">Tradición peruana, ahora también en línea</span>
            <h1>Tu comida favorita, <em>sin esperar de más.</em></h1>
            <p>Consulta el menú actualizado y realiza pedidos para llevar o consumir en el local.</p>
            <div className="hero-actions">
              <a className="btn btn-primary" href="#menu">Ver menú</a>
              <a className="btn btn-secondary" href="#pedidos">Hacer pedido</a>
            </div>
            <div className="hero-info">
              <div><strong>📍 Piura</strong><span>Av. Grau N.º 456</span></div>
              <div><strong>⏱ Atención ágil</strong><span>Pedidos digitales persistentes</span></div>
            </div>
          </div>
          <div className="hero-card">
            <div className="plate-card"><div className="plate-emoji">🍛</div><span>Especial de la casa</span><h3>Seco de cabrito</h3><p>Con frejoles y arroz blanco.</p><strong>S/ 28.00</strong></div>
            <div className="floating-tag tag-1">★ Cocina criolla</div>
            <div className="floating-tag tag-2">✓ Disponible hoy</div>
          </div>
        </div>
      </section>
      <div className="container confirmation-wrap"><StatusMessage type="success">{confirmation}</StatusMessage></div>
      <MenuSection menu={menu} loading={loading} error={error} activeCategory={activeCategory}
        onCategory={setActiveCategory} onAdd={addToCart} />
      <OrderPanel cart={cart} onQuantity={changeQuantity} onConfirmed={orderConfirmed} />
      <section className="section next-stage">
        <div className="container next-stage-card">
          <div><span className="eyebrow">Próxima etapa</span><h2>Reservas y clientes frecuentes</h2></div>
          <p>Estas funciones estaban en el prototipo y están deliberadamente marcadas como pendientes hasta definir capacidad de mesas, horarios y política de confirmación.</p>
        </div>
      </section>
    </main>
  )
}

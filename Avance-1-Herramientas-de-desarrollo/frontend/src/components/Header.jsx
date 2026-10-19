import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'

export default function Header() {
  const [open, setOpen] = useState(false)
  return (
    <header className="site-header">
      <div className="container nav-wrap">
        <Link className="brand" to="/" onClick={() => setOpen(false)}>
          <span className="brand-mark">S&T</span>
          <span><strong>Sabor & Tradición</strong><small>Comida en Piura</small></span>
        </Link>
        <button className="menu-toggle" aria-label="Abrir menú" onClick={() => setOpen(!open)}>☰</button>
        <nav className={`main-nav ${open ? 'open' : ''}`}>
          <Link to="/#inicio" onClick={() => setOpen(false)}>Inicio</Link>
          <Link to="/#menu" onClick={() => setOpen(false)}>Menú</Link>
          <Link to="/#pedidos" onClick={() => setOpen(false)}>Pedidos</Link>
          <NavLink className="nav-admin" to="/admin" onClick={() => setOpen(false)}>Administración</NavLink>
        </nav>
      </div>
    </header>
  )
}

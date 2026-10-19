import { Navigate, Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import HomePage from './pages/HomePage'
import AdminPage from './pages/AdminPage'

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <footer>
        <div className="container footer-grid">
          <div><strong>Sabor & Tradición E.I.R.L.</strong><p>Av. Grau N.º 456, Cercado de Piura.</p></div>
          <div><strong>Primera etapa funcional</strong><p>React, Spring Boot y PostgreSQL.</p></div>
        </div>
      </footer>
    </>
  )
}

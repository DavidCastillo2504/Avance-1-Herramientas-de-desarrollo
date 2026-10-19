import StatusMessage from './StatusMessage'

const money = (value) => `S/ ${Number(value).toFixed(2)}`

export default function MenuSection({ menu, loading, error, activeCategory, onCategory, onAdd }) {
  const categories = ['Todos', ...new Set(menu.map((item) => item.category))]
  const visibleItems = activeCategory === 'Todos'
    ? menu
    : menu.filter((item) => item.category === activeCategory)

  return (
    <section id="menu" className="section">
      <div className="container">
        <div className="section-heading">
          <div><span className="eyebrow">Menú digital</span><h2>Sabores peruanos para hoy</h2></div>
          {!loading && !error && (
            <div className="filters" aria-label="Categorías">
              {categories.map((category) => (
                <button key={category} className={`filter ${category === activeCategory ? 'active' : ''}`}
                  onClick={() => onCategory(category)}>{category}</button>
              ))}
            </div>
          )}
        </div>
        {loading && <div className="loading-card">Cargando el menú…</div>}
        <StatusMessage type="error">{error}</StatusMessage>
        {!loading && !error && (
          <div className="menu-grid">
            {visibleItems.map((dish) => (
              <article className="dish-card" key={dish.id}>
                <div className="dish-visual">
                  <span>{dish.emoji}</span>
                  <span className={`badge ${dish.available ? '' : 'off'}`}>{dish.available ? 'Disponible' : 'Agotado'}</span>
                </div>
                <div><h3>{dish.name}</h3><p>{dish.description}</p></div>
                <div className="dish-bottom">
                  <strong>{money(dish.price)}</strong>
                  <button className="add-btn" disabled={!dish.available} onClick={() => onAdd(dish)}
                    aria-label={`Agregar ${dish.name}`}>+</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

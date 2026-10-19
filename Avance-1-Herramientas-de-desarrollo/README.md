# Sabor Web App

Primera etapa funcional del sistema de **Sabor & Tradición**, construida a partir del prototipo original (`index.html`, `styles.css` y `script.js`, conservados sin cambios).

## Alcance implementado

- Menú digital persistido en PostgreSQL, con categorías, precios y disponibilidad.
- Carrito React con cantidades y total referencial.
- Registro real de pedidos; el backend vuelve a consultar precios y disponibilidad antes de calcular el total.
- Panel administrativo con resumen, listado paginado y cambio de estado `PENDING` / `READY`.
- Validaciones de frontend y backend, estados de carga y manejo uniforme de errores.
- Migración Flyway con esquema, restricciones, índices y nueve platos ficticios.

Las reservas, clientes frecuentes y mantenimiento del menú quedan para las siguientes etapas porque requieren definir reglas de capacidad, horarios, permisos y operación.

## Tecnologías

- Frontend: React, React Router y Vite.
- Backend: Java 21, Spring Boot 4.0.8, Spring MVC, Spring Data JPA y Jakarta Validation.
- Base de datos: PostgreSQL 17 y Flyway.

## Inicio rápido con Docker

Requisito: Docker Desktop en ejecución.

```bash
docker compose up --build
```

Después abre:

- Aplicación: <http://localhost:5173>
- Administración: <http://localhost:5173/admin>
- API: <http://localhost:8080/api/menu-items>

Para detener los contenedores usa `docker compose down`. Los datos se conservan en el volumen `sabor_postgres_data`. Si se necesita un reinicio totalmente limpio, `docker compose down -v` también elimina ese volumen.

## Desarrollo por separado

Base de datos:

```bash
docker compose up -d db
```

Backend (requiere Maven 3.9+ y Java 21):

```bash
cd backend
mvn spring-boot:run
```

Las credenciales se configuran mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Los valores predeterminados solo corresponden a la base local declarada en `compose.yaml`.

Frontend (requiere Node.js):

```bash
cd frontend
npm install
npm run dev
```

Puede copiarse `.env.example` a `.env` para cambiar la URL de la API.

## API REST

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/menu-items` | Lista el menú; acepta `?category=Criollos`. |
| `POST` | `/api/orders` | Registra un pedido y devuelve `201 Created`. |
| `GET` | `/api/orders?page=0&size=20` | Lista pedidos del más reciente al más antiguo. |
| `PATCH` | `/api/orders/{id}/status` | Cambia el estado a `PENDING` o `READY`. |
| `GET` | `/api/orders/dashboard` | Devuelve cifras del panel. |

Ejemplo de creación:

```json
{
  "customerName": "Ana Torres",
  "phone": "999 111 222",
  "orderType": "TAKEAWAY",
  "notes": "Sin ají",
  "items": [
    { "menuItemId": 1, "quantity": 2 }
  ]
}
```

## Verificación

```bash
cd frontend
npm run build

cd ../backend
mvn test
```

# Sabor Web App

Sistema web de **Sabor & Tradición**, construido a partir del prototipo original (`index.html`, `styles.css` y `script.js` en la raíz, conservados sin cambios como referencia).

Para desplegarlo en AWS sigue **[AWS_DEPLOYMENT_GUIDE.md](AWS_DEPLOYMENT_GUIDE.md)**.

## Funcionalidades

**Público (sin cuenta):**
- Menú digital con categorías, precios, disponibilidad y fotos servidas desde S3 (o un emoji si el plato no tiene foto).
- Carrito y registro de pedidos (para llevar o en el local). El servidor recalcula precios y disponibilidad.
- Solicitud de reservas de 1 a 20 personas, entre las 12:00 y las 22:00, en franjas de 30 minutos, con hasta 90 días de anticipación y un máximo de 40 comensales por franja.

**Panel `/admin` (requiere iniciar sesión):**
- Resumen: pedidos, platos disponibles y ventas.
- Pedidos: listado y cambio de estado `PENDING` / `READY`.
- Reservas: próximas o historial completo. Se pueden confirmar, cancelar o devolver a pendiente; al reactivar una reserva cancelada se vuelve a validar el cupo.
- Menú: crear, editar y eliminar platos, marcarlos como agotados y subir fotos a S3 o pegar la URL de una imagen ya subida.

## Tecnologías

- Frontend: React 19, React Router 7 y Vite 8 (sitio estático, desplegable en S3).
- Backend: Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA, Flyway y AWS SDK v2 (S3).
- Base de datos: PostgreSQL 17.
- Docker y Docker Compose.

## Seguridad

- Un único usuario administrador, definido por `ADMIN_USERNAME` / `ADMIN_PASSWORD`. La contraseña se guarda en memoria solo como hash BCrypt.
- Las sesiones usan tokens JWT firmados con `JWT_SECRET`, con una duración de 8 h por defecto.
- Tras 5 intentos fallidos desde una misma IP, el login se bloquea 15 minutos.
- Solo el origen configurado en `CORS_ALLOWED_ORIGINS` puede llamar a la API desde el navegador.
- Las imágenes se validan por su contenido real (JPG, PNG o WEBP, máximo 5 MB) antes de subirlas a S3. El acceso a S3 usa el IAM Role de la EC2, sin claves en el código.
- En producción, PostgreSQL no publica ningún puerto.

## Inicio rápido local con Docker

Requisito: Docker Desktop en ejecución.

```bash
docker compose up --build
```

- Aplicación: <http://localhost:5173>
- Administración: <http://localhost:5173/admin> (usuario `admin`, contraseña `admin-local-123`, solo para uso local)
- API: <http://localhost:8080/api/menu-items>

`docker compose down` detiene los contenedores y conserva los datos. `docker compose down -v` borra también la base de datos local.

En local, la carga de fotos a S3 está desactivada. Puedes pegar la URL de una imagen en el formulario del menú.

## Desarrollo por separado

```bash
docker compose up -d db                       # solo PostgreSQL (en 127.0.0.1:5432)
```

Backend (Java 21 y Maven 3.9+). Las variables `JWT_SECRET` y `ADMIN_PASSWORD` son obligatorias:

```bash
cd backend
JWT_SECRET=desarrollo-local-no-usar-en-produccion-123456 ADMIN_PASSWORD=admin-local-123 mvn spring-boot:run
```

En PowerShell, define antes `$env:JWT_SECRET="..."` y `$env:ADMIN_PASSWORD="..."`.

Frontend (Node.js 20+). Usa `.env.development`, que ya apunta a `http://localhost:8080/api`:

```bash
cd frontend
npm install
npm run dev
```

## Variables de entorno del backend

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Sí (con valores por defecto para uso local) | Conexión a PostgreSQL |
| `JWT_SECRET` | Sí | Mínimo 32 caracteres |
| `ADMIN_USERNAME` | No (`admin`) | Usuario del panel |
| `ADMIN_PASSWORD` | Sí | Mínimo 10 caracteres |
| `JWT_EXPIRATION_HOURS` | No (`8`) | Duración de la sesión |
| `CORS_ALLOWED_ORIGINS` | No (`http://localhost:5173`) | Orígenes permitidos, separados por comas |
| `S3_BUCKET` | No | Bucket de imágenes; si está vacío, la carga de fotos se desactiva |
| `AWS_REGION` | No (`us-east-1`) | Región del bucket |
| `S3_PUBLIC_BASE_URL` | No | Base de las URLs públicas de las imágenes |

Para producción, consulta `.env.example` y `compose.prod.yaml`.

## API REST

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `GET` | `/api/menu-items` | Público | Lista el menú; acepta `?category=Criollos` |
| `POST` | `/api/orders` | Público | Registra un pedido (`201`) |
| `POST` | `/api/reservations` | Público | Registra una reserva (`201`) |
| `POST` | `/api/auth/login` | Público | `{username, password}` → `{token, expiresAt}` |
| `GET` | `/actuator/health` | Público | Estado del servicio |
| `GET` | `/api/orders?page=0&size=20` | Admin | Pedidos, del más reciente al más antiguo |
| `PATCH` | `/api/orders/{id}/status` | Admin | Estado `PENDING` / `READY` |
| `GET` | `/api/orders/dashboard` | Admin | Cifras del panel |
| `GET` | `/api/reservations?upcoming=true` | Admin | Reservas desde hoy (`upcoming=false`: historial completo) |
| `PATCH` | `/api/reservations/{id}/status` | Admin | Estado `PENDING` / `CONFIRMED` / `CANCELLED` |
| `POST` | `/api/menu-items` | Admin | Crea un plato |
| `PUT` | `/api/menu-items/{id}` | Admin | Edita un plato |
| `PATCH` | `/api/menu-items/{id}/availability` | Admin | `{available: true/false}` |
| `DELETE` | `/api/menu-items/{id}` | Admin | Elimina un plato sin pedidos asociados |
| `GET` | `/api/images/status` | Admin | Indica si la carga a S3 está habilitada |
| `POST` | `/api/images` | Admin | Sube una foto (`multipart`, campo `file`) a S3 |

Los endpoints de tipo **Admin** requieren la cabecera `Authorization: Bearer <token>`.

Ejemplo de pedido:

```json
{
  "customerName": "Ana Torres",
  "phone": "999 111 222",
  "orderType": "TAKEAWAY",
  "notes": "Sin ají",
  "items": [{ "menuItemId": 1, "quantity": 2 }]
}
```

## Verificación

```bash
cd backend && mvn test
cd frontend && VITE_API_URL=http://localhost:8080/api npm run build
```

Para el build de producción hace falta `VITE_API_URL`; sin ella, el build falla a propósito.

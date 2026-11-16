# Guía de despliegue en AWS: Sabor & Tradición

Guía paso a paso para desplegar el sistema a mano. Todos los comandos son exactos; solo reemplaza lo que aparece en `MAYÚSCULAS`.

**Valores que usa esta guía (cámbialos por los tuyos):**

| Dato | Ejemplo |
|---|---|
| Región | `us-east-1` |
| Bucket del frontend | `sabor-frontend-TUNOMBRE` |
| Bucket de imágenes | `sabor-imagenes-TUNOMBRE` |
| IP elástica de la EC2 | `IP_ELASTICA` (p. ej. `3.90.12.34`) |

> Los nombres de bucket son únicos en todo AWS. Agrega un sufijo propio (tu nombre o la fecha).

---

## 1. Arquitectura final

```
Navegador
   │  http://sabor-frontend-XXX.s3-website-us-east-1.amazonaws.com     (HTML/JS/CSS estáticos)
   ├──────────────────────────────► S3: bucket del frontend (sitio web estático)
   │
   │  http://IP_ELASTICA/api/...                                         (llamadas a la API)
   ├──────────────────────────────► EC2 (Ubuntu + Docker Compose)
   │                                  ├─ contenedor backend  (Spring Boot, puerto 80 → 8080)
   │                                  └─ contenedor db       (PostgreSQL 17, SOLO red interna de Docker)
   │                                       └─ volumen sabor_postgres_data (datos persistentes)
   │
   │  https://sabor-imagenes-XXX.s3.us-east-1.amazonaws.com/menu/...     (fotos de platos)
   └──────────────────────────────► S3: bucket de imágenes (lectura pública solo de menu/*)
                                          ▲
           EC2 (IAM Role, sin claves) ────┘  sube fotos desde el panel /admin
```

- **Público (sin login):** ver menú, hacer pedidos, solicitar reservas.
- **Panel `/admin` (con usuario y contraseña):** pedidos, reservas, gestión del menú y carga de fotos.
- PostgreSQL **no** tiene puertos abiertos hacia Internet.
- El backend **no** guarda imágenes en disco: van directo a S3.

---

## 2. Servicios AWS que vas a crear

| Servicio | Para qué | Cantidad |
|---|---|---|
| S3 | Frontend estático | 1 bucket |
| S3 | Imágenes de los platos | 1 bucket |
| EC2 | Backend + PostgreSQL en Docker | 1 instancia |
| Elastic IP | IP pública fija para la API | 1 |
| Security Group | Firewall de la EC2 | 1 |
| IAM Role | Permiso de la EC2 para subir fotos a S3 | 1 rol + 1 política |

No necesitas RDS, ALB, CloudFront, Route 53 ni ningún otro servicio.

---

## 3. Bucket S3 del frontend

Consola de AWS → **S3** → **Create bucket**:

1. **Bucket name:** `sabor-frontend-TUNOMBRE`. **Region:** `us-east-1`.
2. **Block Public Access settings:** desmarca **Block all public access** y confirma con el aviso ("I acknowledge…").
3. Crea el bucket.
4. Entra al bucket → **Properties** → **Static website hosting** → **Edit**:
   - **Enable**.
   - **Index document:** `index.html`
   - **Error document:** `index.html` (necesario para que `/admin` funcione al recargar la página).
   - Guarda y **copia la URL** que aparece en *Bucket website endpoint*, por ejemplo
     `http://sabor-frontend-TUNOMBRE.s3-website-us-east-1.amazonaws.com`. La vas a necesitar para CORS.
5. **Permissions** → **Bucket policy** → **Edit**, pega esto y cambia el nombre del bucket:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "LecturaPublicaFrontend",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::sabor-frontend-TUNOMBRE/*"
    }
  ]
}
```

---

## 4. Bucket S3 de imágenes

**Estrategia:** un solo bucket. Todas las fotos del menú van en la carpeta (prefijo) `menu/`. Solo esa carpeta es pública y solo para lectura. El resto del bucket sigue privado.

1. **S3** → **Create bucket** → `sabor-imagenes-TUNOMBRE`, misma región.
2. **Object Ownership:** deja *ACLs disabled (recommended)*.
3. **Block Public Access settings:** desmarca **Block all public access**. Luego deja **marcadas** las dos opciones de ACL y **desmarca** las dos de *bucket policies*:
   - ☑ Block public access to buckets and objects granted through *new* access control lists (ACLs)
   - ☑ Block public access to buckets and objects granted through *any* access control lists (ACLs)
   - ☐ Block public access to buckets and objects granted through new public bucket or access point policies
   - ☐ Block public and cross-account access to buckets and objects through any public bucket or access point policies
4. Crea el bucket.
5. **Permissions** → **Bucket policy**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "LecturaPublicaSoloMenu",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::sabor-imagenes-TUNOMBRE/menu/*"
    }
  ]
}
```

No hace falta configurar CORS en este bucket: el navegador solo muestra las imágenes con `<img>` y las cargas desde el panel pasan por el backend.

---

## 5. Instancia EC2

**Recomendación:** `t3.small` (2 vCPU, 2 GB RAM). Alcanza para Java y PostgreSQL con carga razonable, y para compilar la imagen Docker dentro de la instancia con ayuda de swap. Una `t3.micro` (1 GB) se queda corta para compilar con Maven.

**EC2** → **Launch instance**:

1. **Name:** `sabor-backend`.
2. **AMI:** *Ubuntu Server 24.04 LTS* (64-bit x86).
3. **Instance type:** `t3.small`.
4. **Key pair:** crea uno nuevo (`sabor-key`, formato `.pem`) y guárdalo bien. Sin él no podrás entrar por SSH.
5. **Network settings** → **Edit** → crea un Security Group como se indica en la [sección 6](#6-security-group).
6. **Storage:** 20 GiB `gp3`.
7. **Advanced details:**
   - **IAM instance profile:** el rol de la [sección 11](#11-iam-role-para-que-la-ec2-suba-imágenes-a-s3). Puedes asignarlo después.
   - **Metadata version:** *V2 only (token required)*.
   - **Metadata response hop limit:** **`2`**. **Importante:** con el valor `1`, el contenedor Docker no puede leer las credenciales del IAM Role y la carga de fotos falla.
8. **Launch instance**.

**IP elástica:** **EC2** → **Elastic IPs** → **Allocate Elastic IP address** → **Allocate**. Luego **Actions** → **Associate Elastic IP address** → elige la instancia `sabor-backend`. A partir de aquí, esa es tu `IP_ELASTICA`. No cambia aunque reinicies la instancia.

---

## 6. Security Group

Nombre: `sabor-backend-sg`. **Inbound rules:**

| Tipo | Puerto | Origen | Motivo |
|---|---|---|---|
| SSH | 22 | **My IP** | Administrar la instancia (solo desde tu IP) |
| HTTP | 80 | `0.0.0.0/0` | API pública del backend |

**No abras** el 5432 (PostgreSQL) ni el 8080. PostgreSQL no publica ningún puerto en `compose.prod.yaml`, y el backend se expone solo por el 80. **Outbound:** deja la regla por defecto (todo permitido); la EC2 la necesita para descargar imágenes Docker y hablar con S3.

---

## 7. Instalar Docker y Docker Compose en la EC2

Desde tu computadora (en Windows usa PowerShell o Git Bash):

```bash
ssh -i sabor-key.pem ubuntu@IP_ELASTICA
```

Si en Linux/Mac SSH se queja de los permisos del `.pem`, ejecuta antes `chmod 400 sabor-key.pem`.

Ya dentro de la EC2:

```bash
# Docker oficial (incluye docker compose y buildx)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
sudo systemctl enable --now docker

# 2 GB de swap: evita que la compilación con Maven se quede sin memoria
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Herramientas
sudo apt-get update && sudo apt-get install -y git

# Cierra la sesión y vuelve a entrar para que el grupo "docker" surta efecto
exit
```

Vuelve a entrar y comprueba:

```bash
ssh -i sabor-key.pem ubuntu@IP_ELASTICA
docker --version
docker compose version
```

---

## 8. Variables de entorno

Descarga el código en la EC2. Si el repositorio es privado, GitHub te pedirá usuario y un *Personal Access Token* como contraseña.

```bash
cd ~
git clone https://github.com/TU_USUARIO/TU_REPO.git sabor
cd sabor/Avance-1-Herramientas-de-desarrollo
git checkout NOMBRE_DE_LA_RAMA_A_DESPLEGAR   # por ejemplo main o develop
cp .env.example .env
```

Genera los secretos (cada comando imprime un valor distinto):

```bash
openssl rand -base64 24   # -> DB_PASSWORD
openssl rand -base64 48   # -> JWT_SECRET
openssl rand -base64 18   # -> ADMIN_PASSWORD (o elige una propia de 10+ caracteres)
```

Edita el archivo con `nano .env` (guardar: `Ctrl+O`, Enter; salir: `Ctrl+X`):

```dotenv
DB_NAME=sabor_db
DB_USERNAME=sabor_user
DB_PASSWORD=<el primer valor generado>

JWT_SECRET=<el segundo valor generado>
JWT_EXPIRATION_HOURS=8
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<tu contraseña del panel>

CORS_ALLOWED_ORIGINS=http://sabor-frontend-TUNOMBRE.s3-website-us-east-1.amazonaws.com

S3_BUCKET=sabor-imagenes-TUNOMBRE
AWS_REGION=us-east-1
S3_PUBLIC_BASE_URL=

API_PORT=80
JAVA_OPTS=-XX:MaxRAMPercentage=75
```

Protege el archivo:

```bash
chmod 600 .env
```

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_PASSWORD` | Sí | Contraseña de PostgreSQL. **No la cambies después del primer arranque** (ver [sección 10](#10-postgresql-y-persistencia-de-datos)). |
| `JWT_SECRET` | Sí | Mínimo 32 caracteres. Firma las sesiones del panel. Si lo cambias, todas las sesiones abiertas se cierran. |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Sí | Credenciales de `/admin`. La contraseña debe tener 10+ caracteres. Para cambiarla, edita `.env` y reinicia el backend. |
| `CORS_ALLOWED_ORIGINS` | Sí | URL **exacta** del sitio web del bucket del frontend, sin `/` al final. Si son varias, sepáralas con comas. |
| `S3_BUCKET` | No | Si queda vacío, el panel no permite subir fotos (pegar URLs sigue funcionando). |
| `AWS_REGION` | No | Región del bucket de imágenes. |
| `S3_PUBLIC_BASE_URL` | No | Solo si quieres otra base para las URLs. Por defecto: `https://<S3_BUCKET>.s3.<AWS_REGION>.amazonaws.com`. |

**No** agregues `AWS_ACCESS_KEY_ID` ni `AWS_SECRET_ACCESS_KEY`: el backend usa el IAM Role de la instancia.

---

## 9. Construir y ejecutar los contenedores

En la EC2, dentro de `~/sabor/Avance-1-Herramientas-de-desarrollo`:

```bash
docker compose -f compose.prod.yaml up -d --build
```

La primera compilación tarda varios minutos (descarga dependencias de Maven). Luego:

```bash
docker compose -f compose.prod.yaml ps                  # db "healthy" y backend "running"
docker compose -f compose.prod.yaml logs -f backend     # busca "Started SaborApplication" (sal con Ctrl+C)
```

Al arrancar, Flyway crea las tablas y carga 9 platos de ejemplo de forma automática. Los dos contenedores tienen `restart: unless-stopped`, así que vuelven solos si la instancia se reinicia.

Si el backend no arranca, el log dice qué variable falta, por ejemplo `Define JWT_SECRET en .env` o `ADMIN_PASSWORD debe tener al menos 10 caracteres`.

---

## 10. PostgreSQL y persistencia de datos

- Los datos viven en el volumen Docker `sabor_sabor_postgres_data`. Sobreviven a `docker compose down`, a reinicios de los contenedores y de la instancia, y a reconstrucciones del backend.
- **Nunca ejecutes `docker compose -f compose.prod.yaml down -v`**: la opción `-v` borra el volumen y con él toda la base de datos.
- PostgreSQL solo es accesible desde el contenedor del backend (red interna `sabor_default`).
- Las credenciales se fijan **la primera vez** que se crea el volumen. Si después cambias `DB_PASSWORD` en `.env`, el backend ya no podrá conectarse. Para cambiarla, primero hazlo dentro de la base:

  ```bash
  docker compose -f compose.prod.yaml exec db psql -U sabor_user -d sabor_db -c "ALTER USER sabor_user PASSWORD 'NUEVA_PASSWORD';"
  # luego actualiza DB_PASSWORD en .env y reinicia:
  docker compose -f compose.prod.yaml up -d
  ```

Comprobar que los datos persisten:

```bash
docker compose -f compose.prod.yaml restart
docker compose -f compose.prod.yaml exec db psql -U sabor_user -d sabor_db -c "select count(*) from customer_orders;"
```

Consola SQL para revisar datos: `docker compose -f compose.prod.yaml exec db psql -U sabor_user -d sabor_db`.

---

## 11. IAM Role para que la EC2 suba imágenes a S3

1. **IAM** → **Policies** → **Create policy** → pestaña **JSON**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SubirImagenesMenu",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::sabor-imagenes-TUNOMBRE/menu/*"
    }
  ]
}
```

   Nombre: `SaborSubirImagenesMenu`.
2. **IAM** → **Roles** → **Create role** → *Trusted entity:* **AWS service** → *Use case:* **EC2** → adjunta `SaborSubirImagenesMenu` → nombre `sabor-ec2-role`.
3. **EC2** → selecciona la instancia → **Actions** → **Security** → **Modify IAM role** → `sabor-ec2-role` → **Update IAM role**.
4. Verifica que el *hop limit* de metadatos sea **2**. Si no lo pusiste al crear la instancia: **Actions** → **Instance settings** → **Modify instance metadata options** → *Metadata response hop limit* = `2`.
5. No necesitas reiniciar los contenedores: las credenciales del rol se obtienen en la primera carga.

El rol solo permite **escribir** en `menu/`. No permite leer ni borrar el resto del bucket.

---

## 12. Compilar y desplegar el frontend en S3

En **tu computadora** (necesitas Node.js 20 o superior), dentro del proyecto:

```bash
cd Avance-1-Herramientas-de-desarrollo/frontend
```

Crea el archivo `frontend/.env.production` (Git lo ignora, no se sube) con la IP elástica:

```dotenv
VITE_API_URL=http://IP_ELASTICA/api
```

Compila:

```bash
npm ci
npm run build
```

Si olvidas `VITE_API_URL`, `npm run build` se detiene con un mensaje explicativo. El resultado queda en `frontend/dist/`.

**Subida por consola:** bucket del frontend → **Upload** → arrastra **el contenido** de `dist/` (el archivo `index.html` y la carpeta `assets/`, no la carpeta `dist` misma) → **Upload**.

**Subida con AWS CLI** (opcional, si la tienes configurada en tu PC):

```bash
aws s3 sync dist/ s3://sabor-frontend-TUNOMBRE --delete
aws s3 cp dist/index.html s3://sabor-frontend-TUNOMBRE/index.html --cache-control "no-cache"
```

Abre `http://sabor-frontend-TUNOMBRE.s3-website-us-east-1.amazonaws.com`.

---

## 13. URL del backend y CORS

Dos valores deben coincidir:

| Dónde | Variable | Valor |
|---|---|---|
| `frontend/.env.production` (al compilar) | `VITE_API_URL` | `http://IP_ELASTICA/api` |
| `.env` en la EC2 | `CORS_ALLOWED_ORIGINS` | `http://sabor-frontend-TUNOMBRE.s3-website-us-east-1.amazonaws.com` |

- Copia el origen CORS **tal cual** aparece en *Static website hosting*. En algunas regiones el formato es `s3-website.REGION` (con punto) en lugar de `s3-website-REGION`. Va sin `/` al final y con `http://`.
- Si cambias `CORS_ALLOWED_ORIGINS`: `docker compose -f compose.prod.yaml up -d` (recrea el backend con el nuevo valor).
- Si cambias la IP del backend: vuelve a compilar y subir el frontend. La URL de la API se fija al compilar.

---

## 14. Comprobaciones

Desde tu computadora:

```bash
# 1. Backend vivo
curl http://IP_ELASTICA/actuator/health
# -> {"status":"UP"}

# 2. Menú público
curl http://IP_ELASTICA/api/menu-items

# 3. Endpoints de administración protegidos (debe responder 401)
curl -i http://IP_ELASTICA/api/orders

# 4. Login del panel (debe devolver un "token")
curl -X POST http://IP_ELASTICA/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_ADMIN_PASSWORD"}'

# 5. CORS: debe aparecer "Access-Control-Allow-Origin" con la URL del frontend
curl -i -X OPTIONS http://IP_ELASTICA/api/orders -H "Origin: http://sabor-frontend-TUNOMBRE.s3-website-us-east-1.amazonaws.com" -H "Access-Control-Request-Method: POST"

# 6. PostgreSQL NO accesible desde Internet (debe fallar o quedarse colgado)
curl --max-time 5 telnet://IP_ELASTICA:5432
```

En el navegador:

1. Abre el sitio del bucket del frontend. Debe cargar el menú.
2. Haz un pedido y una reserva de prueba.
3. Entra a `/admin`, inicia sesión y comprueba que aparecen el pedido y la reserva.
4. En **Menú**, edita un plato y sube una foto. Debe verse en el panel y en la página principal.
5. Recarga la página estando en `/admin`. Debe seguir funcionando (esto confirma el *Error document* = `index.html`).

| Síntoma | Causa probable |
|---|---|
| "No se pudo conectar con el servidor" | `VITE_API_URL` mal compilado, puerto 80 cerrado en el Security Group o backend caído |
| Error de CORS en la consola del navegador (F12) | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la URL del sitio |
| 403 al abrir la URL del frontend | Falta la bucket policy o *Block all public access* sigue activado |
| "No se pudo guardar la imagen en S3" | IAM Role no asignado, *hop limit* en 1 o `S3_BUCKET` mal escrito |
| La foto se sube pero no se ve (403) | Falta la bucket policy de `menu/*` en el bucket de imágenes |
| `/admin` da 404 al recargar | Falta *Error document* = `index.html` |

---

## 15. Resumen de comandos

**En la EC2 (primera vez):**

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && sudo systemctl enable --now docker
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo apt-get update && sudo apt-get install -y git
exit   # y vuelve a entrar
git clone https://github.com/TU_USUARIO/TU_REPO.git ~/sabor
cd ~/sabor/Avance-1-Herramientas-de-desarrollo
cp .env.example .env && nano .env && chmod 600 .env
docker compose -f compose.prod.yaml up -d --build
```

**Uso diario en la EC2:**

```bash
cd ~/sabor/Avance-1-Herramientas-de-desarrollo
docker compose -f compose.prod.yaml ps                 # estado
docker compose -f compose.prod.yaml logs -f backend    # logs
docker compose -f compose.prod.yaml restart backend    # reiniciar backend
docker compose -f compose.prod.yaml down               # detener (conserva datos)
docker compose -f compose.prod.yaml up -d              # iniciar
```

**En tu PC (frontend):**

```bash
cd Avance-1-Herramientas-de-desarrollo/frontend
npm ci && npm run build
aws s3 sync dist/ s3://sabor-frontend-TUNOMBRE --delete
```

---

## 16. Actualizar el sistema con nuevos cambios

**Backend** (en la EC2):

```bash
cd ~/sabor/Avance-1-Herramientas-de-desarrollo
./backup-db.sh                                   # respaldo antes de actualizar (sección 17)
git pull
docker compose -f compose.prod.yaml up -d --build
docker compose -f compose.prod.yaml logs -f backend
docker image prune -f                            # libera espacio de imágenes viejas
```

Si agregaste una migración nueva (`V4__...sql`), Flyway la aplica sola al arrancar. **Nunca edites** una migración que ya se ejecutó en producción: crea siempre una nueva.

**Frontend** (en tu PC): `git pull`, `npm ci && npm run build` y vuelve a subir `dist/` ([sección 12](#12-compilar-y-desplegar-el-frontend-en-s3)). Si tu navegador sigue mostrando la versión anterior, recarga con `Ctrl+F5`.

---

## 17. Respaldo de PostgreSQL

El proyecto incluye `backup-db.sh`. Crea un respaldo comprimido en `~/backups` y conserva los últimos 14:

```bash
cd ~/sabor/Avance-1-Herramientas-de-desarrollo
chmod +x backup-db.sh
./backup-db.sh
ls -lh ~/backups
```

**Respaldo automático diario a las 3:00 a. m.:**

```bash
crontab -e
# agrega esta línea al final:
0 3 * * * cd /home/ubuntu/sabor/Avance-1-Herramientas-de-desarrollo && ./backup-db.sh >> /home/ubuntu/backups/backup.log 2>&1
```

**Copia el respaldo fuera de la EC2** (si se pierde la instancia, se pierden también los respaldos guardados en ella). Desde tu PC:

```bash
scp -i sabor-key.pem "ubuntu@IP_ELASTICA:~/backups/*.sql.gz" .
```

**Restaurar un respaldo** (reemplaza los datos actuales):

```bash
cd ~/sabor/Avance-1-Herramientas-de-desarrollo
docker compose -f compose.prod.yaml stop backend
gunzip -c ~/backups/sabor_db_FECHA.sql.gz | docker compose -f compose.prod.yaml exec -T db psql -U sabor_user -d sabor_db
docker compose -f compose.prod.yaml start backend
```

El respaldo se genera con `--clean --if-exists`, así que al restaurarlo se borran y se recrean las tablas.

---

## Configuración y carga manual de imágenes en S3

### Cómo se organizan

| Qué | Dónde |
|---|---|
| Bucket | `sabor-imagenes-TUNOMBRE` |
| Carpeta de fotos de platos | `menu/` |
| Nombre recomendado | minúsculas, sin espacios ni tildes: `menu/lomo-saltado.jpg` |
| Formatos | JPG, PNG o WEBP |
| Tamaño recomendado | ~1200×800 px, menos de 500 KB (máximo aceptado por el panel: 5 MB) |

Solo `menu/*` es público. Si subes algo fuera de esa carpeta, no se verá en la web (403).

### Opción A: subir desde el panel (automático)

1. `/admin` → **Menú** → **Editar** en un plato (o crea uno nuevo).
2. **Subir foto** → elige el archivo. El backend lo valida, lo sube a `menu/<id-aleatorio>.jpg` y llena el campo **URL de la imagen**.
3. **Guardar cambios**.

Esto requiere `S3_BUCKET` en `.env` y el IAM Role de la [sección 11](#11-iam-role-para-que-la-ec2-suba-imágenes-a-s3). El backend comprueba el contenido real del archivo: un archivo que no sea imagen se rechaza aunque tenga extensión `.jpg`.

### Opción B: subir a mano por la consola de AWS

1. **S3** → `sabor-imagenes-TUNOMBRE` → **Create folder** → `menu` (solo la primera vez).
2. Entra a `menu/` → **Upload** → **Add files** → elige las fotos → **Upload**.
   La consola asigna el *Content-Type* correcto (`image/jpeg`, `image/png`…) a partir de la extensión.
3. Haz clic en el archivo subido y copia **Object URL**, por ejemplo:
   `https://sabor-imagenes-TUNOMBRE.s3.us-east-1.amazonaws.com/menu/lomo-saltado.jpg`
4. Pega esa URL en el navegador. Debe mostrarse la foto. Si da *AccessDenied*, revisa la bucket policy de la [sección 4](#4-bucket-s3-de-imágenes).
5. `/admin` → **Menú** → **Editar** el plato → pega la URL en **URL de la imagen** → **Guardar cambios**.

### Opción C: subir a mano con AWS CLI (desde tu PC)

```bash
aws s3 cp lomo-saltado.jpg s3://sabor-imagenes-TUNOMBRE/menu/lomo-saltado.jpg --content-type image/jpeg
# Varias a la vez desde una carpeta local "fotos":
aws s3 sync ./fotos s3://sabor-imagenes-TUNOMBRE/menu/
```

La URL resultante siempre sigue este formato:

```
https://<BUCKET>.s3.<REGION>.amazonaws.com/menu/<ARCHIVO>
```

### Cómo funciona en la aplicación

- Cada plato tiene un campo opcional `imageUrl` en la base de datos. La página pública muestra esa foto. Si está vacío o la imagen no carga, muestra el emoji del plato.
- Las fotos se descargan **directamente desde S3**, sin pasar por la EC2.
- Para cambiar la foto, sube una nueva y actualiza la URL. Para quitarla, usa **Quitar imagen** y guarda.
- Las fotos que dejan de usarse no se borran solas de S3. Puedes eliminarlas a mano desde la consola.

### Permisos necesarios (resumen)

| Quién | Permiso | Configurado en |
|---|---|---|
| Cualquier visitante | `s3:GetObject` sobre `menu/*` | Bucket policy del bucket de imágenes ([sección 4](#4-bucket-s3-de-imágenes)) |
| EC2 (backend) | `s3:PutObject` sobre `menu/*` | IAM Role `sabor-ec2-role` ([sección 11](#11-iam-role-para-que-la-ec2-suba-imágenes-a-s3)) |
| Tú (consola o CLI) | Tu usuario de AWS | — |

---

## Notas de seguridad y limitaciones conocidas

- **Sin HTTPS.** Los sitios web de S3 solo sirven HTTP y la API en la EC2 también usa HTTP, porque la arquitectura no incluye CloudFront ni balanceador. La contraseña del panel y los datos de los clientes viajan sin cifrar. Para un uso real con clientes, lo recomendable es tener un dominio y poner HTTPS delante del backend, por ejemplo con un contenedor Caddy o Nginx + Let's Encrypt en la misma EC2. Eso no está incluido en esta guía.
- El panel usa un único usuario administrador definido en `.env`.
- Tras 5 intentos de login fallidos desde una misma IP, el acceso se bloquea 15 minutos.
- La sesión del panel dura `JWT_EXPIRATION_HOURS` (8 h por defecto) y se cierra al cerrar la pestaña.
- Mantén el sistema operativo de la EC2 actualizado: `sudo apt-get update && sudo apt-get upgrade -y` de vez en cuando.

import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ command, mode }) => {
  const env = { ...loadEnv(mode, process.cwd(), 'VITE_'), ...process.env }

  // La compilación de producción (la que se sube a S3) exige la URL real del backend,
  // para que nunca se publique un build apuntando a localhost por accidente.
  if (command === 'build' && mode === 'production') {
    const apiUrl = env.VITE_API_URL ?? ''
    if (!/^https?:\/\/.+\/api\/?$/.test(apiUrl)) {
      throw new Error('Define VITE_API_URL (por ejemplo http://IP-ELASTICA-EC2/api) en frontend/.env.production '
        + 'o como variable de entorno antes de ejecutar "npm run build".')
    }
    if (/localhost|127\.0\.0\.1/.test(apiUrl)) {
      console.warn(`\n[aviso] VITE_API_URL apunta a ${apiUrl}. No uses este build para S3.\n`)
    }
  }

  return {
    plugins: [react()],
    server: { port: 5173 },
    build: { sourcemap: false },
  }
})

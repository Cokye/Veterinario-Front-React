import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Configuracion de Vite (la herramienta que compila y sirve el frontend).
// El puerto 5173 es el que autorizamos en el CORS del backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
})

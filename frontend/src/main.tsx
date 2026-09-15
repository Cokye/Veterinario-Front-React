import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './estilos.css'

// Punto de entrada del frontend: monta el componente App dentro del <div id="root">.
// El '!' le dice a TypeScript "confia, este elemento existe" (esta en index.html).
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)

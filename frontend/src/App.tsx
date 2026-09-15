import { useState } from 'react'
import Login from './Login.tsx'
import Dashboard from './Dashboard.tsx'
import type { UsuarioSesion } from './tipos'

/**
 * Componente raiz de la aplicacion.
 *
 * Su unica tarea es decidir QUE pantalla mostrar:
 *   - Si NO hay usuario logueado -> muestra el Login.
 *   - Si SI hay usuario logueado  -> muestra el Dashboard.
 *
 * El estado 'usuario' es de tipo UsuarioSesion | null: o hay un usuario, o es
 * null (nadie logueado). TypeScript nos obliga a contemplar ambos casos.
 *
 * Cuando el login es correcto, Login nos avisa con 'onLoginExitoso' y guardamos
 * el usuario. Ese cambio de estado hace que React vuelva a renderizar y aparece
 * el dashboard. Asi funciona la "redireccion" en una app de una sola pagina:
 * no cambiamos de URL, cambiamos que se muestra.
 */
export default function App() {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null)

  // Cerrar sesion = volver a no tener usuario, lo que muestra el login de nuevo.
  function cerrarSesion(): void {
    setUsuario(null)
  }

  if (!usuario) {
    return <Login onLoginExitoso={setUsuario} />
  }

  return <Dashboard usuario={usuario} onCerrarSesion={cerrarSesion} />
}

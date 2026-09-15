import { useState, type FormEvent } from 'react'
import { login } from './api'
import type { UsuarioSesion } from './tipos'

/**
 * Las props que recibe el Login. Definirlas como interface hace que, si alguien
 * usa <Login> sin pasar onLoginExitoso, TypeScript lo marque como error.
 */
interface LoginProps {
  onLoginExitoso: (usuario: UsuarioSesion) => void
}

/**
 * Pantalla de inicio de sesion.
 *
 * Recibe 'onLoginExitoso' desde App. Cuando el backend confirma las
 * credenciales, llamamos a esa funcion con el usuario, y App muestra el
 * dashboard. Este es el patron clave en React: el padre pasa una funcion al
 * hijo, y el hijo la llama para "avisar hacia arriba".
 */
export default function Login({ onLoginExitoso }: LoginProps) {
  // Un estado por cada campo (componentes "controlados"). <string> es el tipo.
  const [email, setEmail] = useState<string>('')
  const [password, setPassword] = useState<string>('')
  const [error, setError] = useState<string>('')
  const [cargando, setCargando] = useState<boolean>(false)

  async function manejarSubmit(evento: FormEvent<HTMLFormElement>): Promise<void> {
    // Evita que el formulario recargue la pagina (comportamiento por defecto).
    evento.preventDefault()
    setError('')
    setCargando(true)

    try {
      // Llamamos al backend a traves de nuestra capa api.ts
      const { ok, datos } = await login(email, password)

      if (ok && datos.exito && datos.nombre && datos.email) {
        const idRol = (datos as any).rol_id ?? (datos as any).rol?.id ?? (datos as any).rol
        const idUsuario = (datos as any).id ?? (datos as any).usuario_id
    
        // Login correcto: avisamos a App con los datos del usuario.
        onLoginExitoso({ nombre: datos.nombre, email: datos.email, rol_id: Number(idRol), id: Number(idUsuario)})
      } else {
        // Credenciales incorrectas: mostramos el mensaje del backend.
        setError(datos.mensaje || 'No se pudo iniciar sesion')
      }
    } catch {
      // Este catch se activa si el backend no responde (esta apagado, sin red...).
      setError('No se pudo conectar con el servidor. Verifica que el backend este corriendo en el puerto 8080.')
    } finally {
      // Pase lo que pase, dejamos de mostrar "cargando".
      setCargando(false)
    }
  }

  return (
    <div className="pantalla-centrada">
      <form className="tarjeta" onSubmit={manejarSubmit}>
        <h1 className="titulo">Iniciar sesion</h1>
        <p className="subtitulo">Curso: comunicacion front &harr; back</p>

        <label className="etiqueta" htmlFor="email">Email</label>
        <input
          id="email"
          className="campo"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ana@ejemplo.cl"
          required
        />

        <label className="etiqueta" htmlFor="password">Contrasena</label>
        <input
          id="password"
          className="campo"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="1234"
          required
        />

        {/* El mensaje de error solo aparece si hay algo en 'error' */}
        {error && <div className="alerta-error">{error}</div>}

        <button className="boton-principal" type="submit" disabled={cargando}>
          {cargando ? 'Ingresando...' : 'Ingresar'}
        </button>

        <div className="ayuda">
          <strong>Usuarios de prueba:</strong>
          <div>ana@ejemplo.cl / 1234</div>
          <div>luis@ejemplo.cl / 1234</div>
        </div>
      </form>
    </div>
  )
}

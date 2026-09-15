import { useEffect, useState, type FormEvent } from 'react'
import { listarUsuarios, crearUsuario, actualizarUsuario, listarAnimales, crearAnimal } from './api'
import type { Usuario, UsuarioSesion, Animales } from './tipos'

/** Props del Dashboard: el usuario logueado y la funcion para cerrar sesion. */
interface DashboardProps {
  usuario: UsuarioSesion
  onCerrarSesion: () => void
}

/**
 * Pantalla principal tras el login.
 *
 * Muestra:
 *   1. La lista de usuarios que trae del backend.
 *   2. Un formulario para crear un usuario nuevo (la parte "Create" del CRUD).
 */
export default function Dashboard({ usuario, onCerrarSesion }: DashboardProps) {
  const nombresRoles: Record<number, string> = {
    1: 'Veterinario',
    2: 'Cliente',
    21: 'Veterinario en Jefe'
  }

  // 1. Identificar si el usuario actual es Cliente (Rol ID = 2)
  // 1. Extraer el ID real del rol (soporta objeto { id: 2 } o número directo)
  const idDelRol = typeof usuario.rol === 'object' && usuario.rol !== null
    ? usuario.rol.id
    : (usuario.rol_id ?? usuario.rol)

  // 2. Comprobar si es Cliente
  const esCliente = Number(idDelRol) === 2

  // 3. Comprobar en consola qué detectó
  console.log("Datos del usuario en sesión:", usuario)
  console.log("ID del rol detectado:", idDelRol, "| ¿Es cliente?:", esCliente)

  // El estado de la lista es de tipo Usuario[]: un array de usuarios. Se le agrego constante aniamles
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [animales, setAnimales] = useState<Animales[]>([])
  const [cargando, setCargando] = useState<boolean>(true)
  const [error, setError] = useState<string>('')

  // Campos del formulario de creacion.
  const [nombre, setNombre] = useState<string>('')
  const [email, setEmail] = useState<string>('')
  const [password, setPassword] = useState<string>('')
  const [rolId, setRolId] = useState<number | 0>(0)
  const [mensajeForm, setMensajeForm] = useState<string>('')
  const [guardando, setGuardando] = useState<boolean>(false)

  // Nuevas contanstes para el popup de editar usuario
  const [usuarioAEditar, setUsuarioAEditar] = useState<Usuario | null>(null)
  const [editNombre, setEditNombre] = useState<string>('')
  const [editEmail, setEditEmail] = useState<string>('')
  const [editPassword, setEditPassword] = useState<string>('')
  const [guardandoEdit, setGuardandoEdit] = useState<boolean>(false)
  const [mensajeEdit, setMensajeEdit] = useState<string>('')

  // Nuevas constantes para el Pop up de Mascota
  const [modalMascotaAbierto, setModalMascotaAbierto] = useState<boolean>(false)
  const [nombreMascota, setNombreMascota] = useState<string>('')
  const [especieMascota, setEspecieMascota] = useState<string>('')
  const [razaMascota, setRazaMascota] = useState<string>('')
  const [edadMascota, setEdadMascota] = useState<number | ''>('')
  const [sexoMascota, setSexoMascota] = useState<string>('Macho')
  const [pesoMascota, setPesoMascota] = useState<number | ''>('')
  const [duenoId, setDuenoId] = useState<number | ''>('')
  const [guardandoMascota, setGuardandoMascota] = useState<boolean>(false)
  const [mensajeMascota, setMensajeMascota] = useState<string>('')

  function abrirModalEdicion(usuarioSeleccionado: Usuario) {
    setUsuarioAEditar(usuarioSeleccionado)
    setEditNombre(usuarioSeleccionado.nombre)
    setEditEmail(usuarioSeleccionado.email)
    setEditPassword('')
    setMensajeEdit('')
  }

  function cerrarModalEdicion() {
    setUsuarioAEditar(null)
    setEditNombre('')
    setEditEmail('')
    setEditPassword('')
    setMensajeEdit('')
  }

  async function cargarDatos(): Promise<void> {
    setCargando(true)
    setError('')
    try {
      const [listaUsuarios, listaAnimales] = await Promise.all([
        esCliente ? Promise.resolve([]) : listarUsuarios(),
        listarAnimales().catch(() => []) // Si falla animales, no bloquea usuarios
      ])
      setUsuarios(listaUsuarios)
      setAnimales(listaAnimales)
    } catch {
      setError('No se pudo cargar la lista. Verifica que el backend esté corriendo.')
    } finally {
      setCargando(false)
    }
  }

  async function manejarEditar(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault()
    if (!usuarioAEditar) return
    setGuardandoEdit(true)
    setMensajeEdit('')

    try {
      const { ok, datos } = await actualizarUsuario(
        0,
        editNombre,
        editEmail,
        editPassword,
        ''
      )

      if (ok) {
        await cargarDatos()
        cerrarModalEdicion()
      } else {
        setMensajeEdit(typeof datos === 'string' ? datos : 'No se pudo actualizar')
      }
    } catch {
      setMensajeEdit('Error al conectar con el servidor.')
    } finally {
      setGuardandoEdit(false)
    }
  }

  /**
   * useEffect con [] se ejecuta UNA vez, al montar el componente.
   * Lo usamos para cargar la lista de usuarios apenas entra al dashboard.
   */
  useEffect(() => {
    void cargarDatos()
  }, [])

  async function cargarUsuarios(): Promise<void> {
    await cargarDatos()
  }

  async function manejarCrear(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault()
    setMensajeForm('')
    setGuardando(true)

    try {
      const { ok, datos } = await crearUsuario(nombre, email, password,'' , rolId)

      if (ok) {
        // Se creo bien: limpiamos el formulario y recargamos la lista.
        setNombre('')
        setEmail('')
        setPassword('')
        setRolId(0)
        setMensajeForm('Usuario creado correctamente')
        await cargarDatos()
      } else {
        // El backend rechazo (por ejemplo, email repetido). 'datos' es el texto.
        setMensajeForm(typeof datos === 'string' ? datos : 'No se pudo crear el usuario')
      }
    } catch {
      setMensajeForm('No se pudo conectar con el servidor.')
    } finally {
      setGuardando(false)
    }
  }

  // Función auxiliar para obtener los nombres de las mascotas de un usuario
  function obtenerMascotasUsuario(usuarioId: number): string {
    const mascotas = animales.filter((a) => {
      // Soporta tanto si el backend devuelve usuario_id como si devuelve el objeto usuario anidado
      return (a as any).usuario_id === usuarioId || a.usuario?.id === usuarioId
    })

    if (mascotas.length === 0) return 'Sin mascotas'
    return mascotas.map((m) => `${m.nombre} (${m.especie})`).join(', ')
  }

  const misMascotas = animales.filter((a) => {
    return (a as any).usuario_id === usuario.id || a.usuario?.id === usuario.id
  })

  //Funciones para agregar un nuevo Animal
  function abrirModalMascota(idUsuarioInicial?: number) {
    setNombreMascota('')
    setEspecieMascota('')
    setRazaMascota('')
    setEdadMascota('')
    setSexoMascota('Macho')
    setPesoMascota('')
    setDuenoId(idUsuarioInicial ?? (usuarios.length > 0 ? usuarios[0].id : ''))
    setMensajeMascota('')
    setModalMascotaAbierto(true)
  }

  function cerrarModalMascota() {
    setModalMascotaAbierto(false)
  }

  async function manejarGuardarMascota(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault()
    if (!duenoId) {
      setMensajeMascota('Debes seleccionar un usuario responsable.')
      return
    }

    setGuardandoMascota(true)
    setMensajeMascota('')

    try {
      const { ok, datos } = await crearAnimal({
        nombre: nombreMascota,
        especie: especieMascota,
        raza: razaMascota || undefined,
        edad: edadMascota === '' ? undefined : Number(edadMascota),
        sexo: sexoMascota,
        peso: pesoMascota === '' ? undefined : Number(pesoMascota),
        usuario_id: Number(duenoId)
      })

      if (ok) {
        await cargarDatos()
        cerrarModalMascota()
      } else {
        setMensajeMascota(typeof datos === 'string' ? datos : 'Error al registrar mascota')
      }
    } catch {
      setMensajeMascota('Error de conexión con el backend.')
    } finally {
      setGuardandoMascota(false)
    }
  }

  return (
    <div className="pagina">
      {/* Barra superior con saludo y cerrar sesion */}
      <header className="barra-superior">
        <div>
          <strong>Panel de usuarios</strong>
          <span className="saludo"> · Hola, {usuario.nombre}</span>
        </div>
        <button className="boton-secundario" onClick={onCerrarSesion}>
          Cerrar sesion
        </button>
      </header>
      <main className="contenido">
        {esCliente ? (
          <section className="tarjeta">
            <div className="cabecera-lista">
              <h2 className="titulo-seccion">Mis Mascotas DEL VECINO QUE NO ESTAN</h2>
              <button className="boton-secundario" onClick={cargarDatos}>
                Recargar
              </button>
            </div>

            {cargando ? (
              <p className="texto-tenue">Cargando mascotas...</p>
            ) : error ? (
              <div className="alerta-error">{error}</div>
            ) : (
              <table className="tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Especie</th>
                    <th>Raza</th>
                    <th>Edad</th>
                    <th>Sexo</th>
                    <th>Peso</th>
                  </tr>
                </thead>
                <tbody>
                  {misMascotas.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center' }}>
                        No tienes mascotas registradas
                      </td>
                    </tr>
                  ) : (
                    misMascotas.map((m) => (
                      <tr key={m.id}>
                        <td>{m.id}</td>
                        <td>{m.nombre}</td>
                        <td>{m.especie}</td>
                        <td>{m.raza ?? '-'}</td>
                        <td>{m.edad ? `${m.edad} años` : '-'}</td>
                        <td>{m.sexo ?? '-'}</td>
                        <td>{m.peso ? `${m.peso} kg` : '-'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            )}
          </section>
        ) : (
          <>
            {/* --- Formulario para crear usuario --- */}
            <section className="tarjeta">
              <h2 className="titulo-seccion">Crear usuario</h2>
              <form className="form-inline" onSubmit={manejarCrear}>
                <input
                  className="campo"
                  placeholder="Nombre"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  required
                />
                <input
                  className="campo"
                  type="email"
                  placeholder="Email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
                <input
                  className="campo"
                  type="password"
                  placeholder="Contrasena"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <select
                  value={rolId}
                  onChange={(e) => {
                    const valor = e.target.value
                    setRolId(Number(valor) == 0 ? 0 : Number(valor))
                  }}
                  required
                >
                  <option value="">-- Selecciona un rol --</option>
                  <option value="1">Veterinario</option>
                  <option value="2">Cliente</option>
                  <option value="21">Veterinario en Jefe</option>
                </select>

                <button className="boton-principal" type="submit" disabled={guardando}>
                  {guardando ? 'Guardando...' : 'Agregar'}
                </button>
              </form>
              {mensajeForm && <div className="mensaje-form">{mensajeForm}</div>}
            </section>

            {/* --- Lista de usuarios --- */}
            <section className="tarjeta">
              <div className="cabecera-lista">
                <h2 className="titulo-seccion">Usuarios registrados</h2>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  {/* Botón para abrir el pop-up */}
                  <button className="boton-principal" onClick={() => abrirModalMascota()}>
                    + Registrar Mascota
                  </button>
                  <button className="boton-secundario" onClick={cargarUsuarios}>
                    Recargar
                  </button>
                </div>
              </div>

              {cargando ? (
                <p className="texto-tenue">Cargando usuarios...</p>
              ) : error ? (
                <div className="alerta-error">{error}</div>
              ) : (
                <table className="tabla">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nombre</th>
                      <th>Email</th>
                      <th>Rol</th>
                      <th>Mascota</th>
                      <th>Editar</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usuarios.map((u) => (
                      // 'key' ayuda a React a identificar cada fila de forma unica.
                      <tr key={u.id}>
                        <td>{u.id}</td>
                        <td>{u.nombre}</td>
                        <td>{u.email}</td>
                        <td>{u.rol_id ? nombresRoles[u.rol_id] : 'Sin rol'}</td>
                        <td>
                          <span className="texto-secundario">
                            {obtenerMascotasUsuario(u.id)}
                          </span>
                        </td>
                        <td>
                          <button
                            className="boton-secundario"
                            onClick={() => abrirModalEdicion(u)}
                          >
                            Editar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </>
        )}
      </main>

      {usuarioAEditar && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000
          }}
        >
          <div className="tarjeta" style={{ background: 'white', minWidth: '350px' }}>
            <h2 className="titulo-seccion">Editar Usuario #{usuarioAEditar.id}</h2>

            <form
              onSubmit={manejarEditar}
              style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}
            >
              <div>
                <label className="etiqueta">Nombre</label>
                <input
                  className="campo"
                  type="text"
                  value={editNombre}
                  onChange={(e) => setEditNombre(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="etiqueta">Nueva Contraseña</label>
                <input
                  className="campo"
                  type="password"
                  placeholder="Dejar vacía para no cambiar"
                  value={editPassword}
                  onChange={(e) => setEditPassword(e.target.value)}
                />
              </div>

              {mensajeEdit && <div className="alerta-error">{mensajeEdit}</div>}

              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={cerrarModalEdicion}
                  disabled={guardandoEdit}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="boton-principal"
                  disabled={guardandoEdit}
                >
                  {guardandoEdit ? 'Guardando...' : 'Guardar Cambios'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- POP-UP / MODAL REGISTRO DE MASCOTA --- */}
      {modalMascotaAbierto && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000
          }}
        >
          <div className="tarjeta" style={{ background: 'white', minWidth: '400px', maxWidth: '500px' }}>
            <h2 className="titulo-seccion">Registrar Nueva Mascota</h2>

            <form
              onSubmit={manejarGuardarMascota}
              style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem', marginTop: '1rem' }}
            >
              <div>
                <label className="etiqueta">Dueño / Responsable</label>
                <select
                  className="campo"
                  value={duenoId}
                  onChange={(e) => setDuenoId(Number(e.target.value))}
                  required
                >
                  <option value="">-- Selecciona el dueño --</option>
                  {usuarios.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre} ({u.email})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="etiqueta">Nombre del Animal</label>
                <input
                  className="campo"
                  placeholder="Ej: Thor, Misi..."
                  value={nombreMascota}
                  onChange={(e) => setNombreMascota(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                <div>
                  <label className="etiqueta">Especie</label>
                  <input
                    className="campo"
                    placeholder="Ej: Perro, Gato"
                    value={especieMascota}
                    onChange={(e) => setEspecieMascota(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="etiqueta">Raza</label>
                  <input
                    className="campo"
                    placeholder="Ej: Mestizo, Siamés"
                    value={razaMascota}
                    onChange={(e) => setRazaMascota(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem' }}>
                <div>
                  <label className="etiqueta">Edad (Años)</label>
                  <input
                    className="campo"
                    type="number"
                    min="0"
                    placeholder="Ej: 3"
                    value={edadMascota}
                    onChange={(e) => setEdadMascota(e.target.value === '' ? '' : Number(e.target.value))}
                  />
                </div>
                <div>
                  <label className="etiqueta">Sexo</label>
                  <select
                    className="campo"
                    value={sexoMascota}
                    onChange={(e) => setSexoMascota(e.target.value)}
                  >
                    <option value="Macho">Macho</option>
                    <option value="Hembra">Hembra</option>
                  </select>
                </div>
                <div>
                  <label className="etiqueta">Peso (Kg)</label>
                  <input
                    className="campo"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Ej: 12.5"
                    value={pesoMascota}
                    onChange={(e) => setPesoMascota(e.target.value === '' ? '' : Number(e.target.value))}
                  />
                </div>
              </div>

              {mensajeMascota && <div className="alerta-error">{mensajeMascota}</div>}

              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={cerrarModalMascota}
                  disabled={guardandoMascota}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="boton-principal"
                  disabled={guardandoMascota}
                >
                  {guardandoMascota ? 'Guardando...' : 'Registrar Mascota'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
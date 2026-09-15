// ===========================================================================
//  api.ts -- Capa de comunicacion con el backend (tipada con TypeScript)
// ===========================================================================
//
// Todas las llamadas al backend viven aqui. Cada funcion declara QUE devuelve
// (por ejemplo Promise<Usuario[]>), asi el resto de la app sabe con que datos
// trabaja y TypeScript valida que los usemos bien.
//
// Las peticiones ya no usan fetch directo: pasan por apiFetch (httpClient.ts),
// que agrega la URL base, cifra el cuerpo y adjunta la cookie de sesion.

import type { Usuario, LoginResponse, Animales } from './tipos'
import { apiFetch } from './httpClient'

/** Resultado generico de una llamada: si salio bien (ok) y el cuerpo (datos). */
export interface Resultado<T> {
  ok: boolean
  datos: T
}

/**
 * Llama al endpoint de login.
 *
 * OJO: fetch NO lanza error en respuestas 401/500; hay que revisar 'res.ok'
 * a mano. Este es uno de los detalles que mas confunde al empezar.
 *
 * Si el login sale bien, el backend NO devuelve el token en el cuerpo: lo manda
 * en una cookie HttpOnly (cabecera Set-Cookie). Por eso aqui no hay nada que
 * guardar; el navegador se encarga solo de adjuntarla en las siguientes
 * peticiones.
 */
export async function login(
  email: string,
  password: string
): Promise<Resultado<LoginResponse>> {
  const res = await apiFetch('/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })

  // res.ok es true para codigos 200-299. Un 401 (credenciales malas) da false.
  const datos = (await res.json()) as LoginResponse

  return { ok: res.ok, datos }
}

/**
 * Cierra la sesion en el servidor.
 *
 * Cuando el token vivia en localStorage bastaba con borrarlo desde aqui. Ahora
 * esta en una cookie HttpOnly que el JavaScript no puede tocar, asi que hay que
 * pedirle al backend que la borre (responde con la cookie vencida).
 */
export async function logout(): Promise<void> {
  try {
    await apiFetch('/logout', { method: 'POST' })
  } catch {
    // Si el backend no responde igual cerramos sesion en la interfaz: la cookie
    // caduca sola y el usuario no queda atrapado en el dashboard.
  }
}

/**
 * Pide la lista de usuarios al backend.
 * Es un GET simple; devuelve el array de usuarios ya tipado.
 */
export async function listarUsuarios(): Promise<Usuario[]> {
  const res = await apiFetch('/usuarios')
  if (!res.ok) {
    throw new Error('No se pudo obtener la lista de usuarios')
  }
  return (await res.json()) as Usuario[]
}

/**
 * Crea un usuario nuevo.
 * Devuelve si salio bien y el cuerpo: el Usuario creado, o un texto de error
 * (por ejemplo, "Ya existe un usuario con ese email").
 */
export async function crearUsuario(
  nombre: string,
  email: string,
  password: string,
  rut: string,
  rolId: number,
): Promise<Resultado<Usuario | string>> {
  const res = await apiFetch('/usuarios', {
    method: 'POST',
    body: JSON.stringify({ nombre, email, password, rut, rolId })
  })

  // Si fue exito, el backend responde JSON (el usuario); si no, texto.
  const cuerpo: Usuario | string = res.ok
    ? ((await res.json()) as Usuario)
    : await res.text()
  return { ok: res.ok, datos: cuerpo }
}

export async function listarAnimales(): Promise<Animales[]>{
    const res = await apiFetch('/animales', {
    method: 'GET',
  })

  return (await res.json()) as Animales[]
}

export async function crearAnimal(
  animal: Animales,
): Promise<Resultado<Animales | string>> {
  const res = await apiFetch('/animales', {
    method: 'POST',
    body: JSON.stringify(animal)
  })

  // Si fue exito, el backend responde JSON (el usuario); si no, texto.
  const cuerpo: Animales | string = res.ok
    ? ((await res.json()) as Animales)
    : await res.text()
  return { ok: res.ok, datos: cuerpo }
}

/**
 * Actualiza un usuario existente (popup de edicion del dashboard).
 *
 * password y rut son opcionales: si van vacios, el backend conserva el valor
 * que el usuario ya tenia.
 */
export async function actualizarUsuario(
  id: number,
  nombre: string,
  email: string,
  password: string,
  rut: string
): Promise<Resultado<Usuario | string>> {
  const res = await apiFetch(`/usuarios/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ nombre, email, password, rut })
  })

  const cuerpo: Usuario | string = res.ok
    ? ((await res.json()) as Usuario)
    : await res.text()
  return { ok: res.ok, datos: cuerpo }
}
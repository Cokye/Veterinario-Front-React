// ===========================================================================
//  httpClient.ts -- Interceptor de peticiones hacia el backend
// ===========================================================================
//
// Envuelve fetch para no repetir en cada funcion de api.ts la misma logica:
//   - anteponer la URL base del backend
//   - CIFRAR el cuerpo que sale y DESCIFRAR el cuerpo que vuelve
//   - mandar la cookie de sesion (credentials: 'include')
//   - si el backend responde 401 (sesion vencida/invalida), avisar al resto de
//     la app para que cierre la sesion, sin que cada llamada lo revise a mano
//
// SOBRE EL TOKEN: antes este archivo lo guardaba en localStorage y lo ponia a
// mano en el header Authorization. Ya no. El JWT vive en una cookie HttpOnly
// que este codigo NO PUEDE LEER (document.cookie no la muestra): la adjunta el
// navegador solo, gracias a credentials: 'include'. Por eso desaparecieron
// guardarToken/obtenerToken/borrarToken. Ese es justamente el punto: si el
// JavaScript no puede tocar el token, un XSS tampoco puede robarlo.
//
// La clave del diseno: apiFetch sigue devolviendo un objeto Response normal,
// con .ok, .status, .json() y .text(). Por eso api.ts y los componentes no
// necesitan saber nada del cifrado ni de la cookie.

import {
  CABECERA_CLAVE,
  CABECERA_IV,
  CABECERA_MARCA,
  CABECERA_TIPO,
  CABECERA_ERROR,
  crearSobre,
  cifrarTexto,
  descifrarTexto
} from './cripto'
import { API_URL } from './config'

export { API_URL }

/**
 * Evento que este interceptor dispara cuando el backend responde 401.
 * App.tsx lo escucha para cerrar la sesion automaticamente.
 */
export const EVENTO_NO_AUTORIZADO = 'no-autorizado'

/** Codigos de estado que por norma HTTP no pueden llevar cuerpo. */
const ESTADOS_SIN_CUERPO = [204, 205, 304]

/**
 * Reemplazo de fetch para llamar al backend.
 *
 * 'ruta' es relativa a API_URL (ej: '/usuarios'). Cifra el cuerpo saliente,
 * descifra el entrante, adjunta la cookie de sesion, y dispara
 * EVENTO_NO_AUTORIZADO si el backend responde 401.
 */
export async function apiFetch(ruta: string, opciones: RequestInit = {}): Promise<Response> {
  return enviarCifrado(ruta, opciones, false)
}

/**
 * Hace el trabajo real. 'esReintento' evita bucles infinitos: solo se permite
 * un reintento, y es para el caso de que el backend se haya reiniciado con un
 * par de claves RSA nuevo.
 */
async function enviarCifrado(
  ruta: string,
  opciones: RequestInit,
  esReintento: boolean
): Promise<Response> {
  // 1. Clave AES desechable para esta peticion, envuelta con la RSA del server.
  const sobre = await crearSobre(esReintento)

  const headers = new Headers(opciones.headers)
  headers.set(CABECERA_CLAVE, sobre.claveEnvueltaBase64)

  // 2. Si la peticion lleva cuerpo, se cifra. El JSON original nunca sale
  //    en claro: por la red viaja base64 de bytes AES-GCM.
  let cuerpo: string | undefined
  if (opciones.body != null) {
    const textoPlano =
      typeof opciones.body === 'string' ? opciones.body : JSON.stringify(opciones.body)

    const { ivBase64, cuerpoBase64 } = await cifrarTexto(sobre.claveAes, textoPlano)
    headers.set(CABECERA_IV, ivBase64)
    // Hacia afuera esto son bytes opacos; el backend lo vuelve a marcar como
    // JSON despues de descifrarlo (ver PeticionDescifrada.java).
    headers.set('Content-Type', 'application/octet-stream')
    cuerpo = cuerpoBase64
  }

  const respuesta = await fetch(`${API_URL}${ruta}`, {
    ...opciones,
    headers,
    body: cuerpo,
    // 'include' es lo que hace que el navegador adjunte la cookie HttpOnly de
    // sesion, aunque la API este en otro puerto (:8080) que el frontend (:5173).
    // Requiere que el backend responda Access-Control-Allow-Credentials: true.
    credentials: 'include'
  })

  // 3. Si el backend no reconoce nuestra clave (se reinicio y genero otro par
  //    RSA), pedimos la clave publica de nuevo y reintentamos una sola vez.
  if (!esReintento && respuesta.headers.get(CABECERA_ERROR) === 'clave-desconocida') {
    return enviarCifrado(ruta, opciones, true)
  }

  // 4. Descifrar el cuerpo de la respuesta.
  const descifrada = await descifrarRespuesta(respuesta, sobre.claveAes)

  if (descifrada.status === 401) {
    // Ya no hay nada que borrar aqui: la cookie la controla el servidor.
    window.dispatchEvent(new Event(EVENTO_NO_AUTORIZADO))
  }

  return descifrada
}

/**
 * Convierte la respuesta cifrada en un Response normal y corriente.
 *
 * Si no viene marcada con X-Enc (por ejemplo, un error del propio canal de
 * cifrado, que se manda en claro a proposito), se devuelve tal cual.
 */
async function descifrarRespuesta(respuesta: Response, claveAes: CryptoKey): Promise<Response> {
  if (respuesta.headers.get(CABECERA_MARCA) !== '1') {
    return respuesta
  }

  const ivBase64 = respuesta.headers.get(CABECERA_IV)
  if (!ivBase64) {
    throw new Error('El servidor marco la respuesta como cifrada pero no envio el IV')
  }

  const cuerpoBase64 = await respuesta.text()
  const textoPlano = await descifrarTexto(claveAes, ivBase64, cuerpoBase64)

  // Reconstruimos las cabeceras con el Content-Type real que iba adentro.
  const cabeceras = new Headers()
  const tipoOriginal = respuesta.headers.get(CABECERA_TIPO)
  if (tipoOriginal) {
    cabeceras.set('Content-Type', tipoOriginal)
  }

  const sinCuerpo = ESTADOS_SIN_CUERPO.includes(respuesta.status)

  return new Response(sinCuerpo ? null : textoPlano, {
    status: respuesta.status,
    statusText: respuesta.statusText,
    headers: cabeceras
  })
}

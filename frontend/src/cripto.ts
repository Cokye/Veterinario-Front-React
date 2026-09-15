// ===========================================================================
//  cripto.ts -- Cifrado de la comunicacion en el lado del navegador
// ===========================================================================
//
// Usa la Web Crypto API (window.crypto.subtle), que viene incluida en todos
// los navegadores modernos. No hace falta instalar ninguna libreria.
//
// ESQUEMA (el mismo que implementa CifradoService.java en el backend):
//
//   1. Le pedimos al backend su clave publica RSA (una sola vez, y se cachea).
//   2. Por CADA peticion generamos una clave AES-256 nueva y desechable.
//   3. El cuerpo se cifra con AES-256-GCM (rapido, y ademas detecta si alguien
//      manipulo el mensaje).
//   4. La clave AES se envia "envuelta" con RSA-OAEP en la cabecera X-Enc-Key.
//      Solo el backend, con su clave privada, puede abrirla.
//   5. El backend responde cifrado con esa MISMA clave AES, que solo vive en
//      esta pestana y en la memoria del servidor durante esa peticion.
//
// NOTA IMPORTANTE: crypto.subtle solo existe en un "contexto seguro", es decir
// https:// o http://localhost. En localhost (nuestro caso) funciona perfecto.

import { API_URL } from './config'

/** Nombres de las cabeceras del protocolo, iguales que en CifradoFiltro.java. */
export const CABECERA_CLAVE = 'X-Enc-Key'
export const CABECERA_IV = 'X-Enc-Iv'
export const CABECERA_MARCA = 'X-Enc'
export const CABECERA_TIPO = 'X-Enc-Content-Type'
export const CABECERA_ERROR = 'X-Enc-Error'

/** Lo que devuelve GET /api/crypto/public-key. */
interface ClavePublicaResponse {
  algoritmo: string
  formato: string
  cifradoCuerpo: string
  clavePublica: string
  idClave: string
}

/** El "sobre" listo para enviar: la clave AES y su version envuelta con RSA. */
export interface SobreCifrado {
  claveAes: CryptoKey
  claveEnvueltaBase64: string
}

// Cache de la clave publica del servidor: se pide una vez y se reutiliza.
let clavePublicaServidor: CryptoKey | null = null
let idClaveServidor: string | null = null
let peticionEnCurso: Promise<CryptoKey> | null = null

// ---------------------------------------------------------------------------
//  Utilidades base64 <-> bytes
// ---------------------------------------------------------------------------

export function bytesABase64(bytes: ArrayBuffer | Uint8Array): string {
  const arreglo = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes)
  let binario = ''
  // Se hace por trozos para no reventar la pila con arrays grandes.
  const TROZO = 0x8000
  for (let i = 0; i < arreglo.length; i += TROZO) {
    binario += String.fromCharCode(...arreglo.subarray(i, i + TROZO))
  }
  return btoa(binario)
}

export function base64ABuffer(texto: string): ArrayBuffer {
  const binario = atob(texto.trim())
  // Se reserva el ArrayBuffer explicitamente porque las APIs de crypto.subtle
  // esperan un BufferSource respaldado por un ArrayBuffer normal.
  const buffer = new ArrayBuffer(binario.length)
  const bytes = new Uint8Array(buffer)
  for (let i = 0; i < binario.length; i++) {
    bytes[i] = binario.charCodeAt(i)
  }
  return buffer
}

// ---------------------------------------------------------------------------
//  Clave publica del servidor
// ---------------------------------------------------------------------------

function verificarSoporte(): void {
  if (!globalThis.crypto?.subtle) {
    throw new Error(
      'Este navegador no expone crypto.subtle. Abre la app en http://localhost o por https.'
    )
  }
}

/**
 * Pide la clave publica al backend y la importa como CryptoKey.
 * Se cachea; varias llamadas simultaneas comparten la misma peticion.
 */
export async function obtenerClavePublica(forzarRecarga = false): Promise<CryptoKey> {
  verificarSoporte()

  if (forzarRecarga) {
    clavePublicaServidor = null
    idClaveServidor = null
    peticionEnCurso = null
  }
  if (clavePublicaServidor) {
    return clavePublicaServidor
  }
  if (peticionEnCurso) {
    return peticionEnCurso
  }

  peticionEnCurso = (async () => {
    // Esta es la UNICA llamada que viaja en claro: es el handshake inicial.
    const res = await fetch(`${API_URL}/crypto/public-key`)
    if (!res.ok) {
      throw new Error('No se pudo obtener la clave publica del servidor')
    }

    const datos = (await res.json()) as ClavePublicaResponse

    // 'spki' es el formato en que Java exporta una PublicKey con getEncoded().
    const clave = await crypto.subtle.importKey(
      'spki',
      base64ABuffer(datos.clavePublica),
      { name: 'RSA-OAEP', hash: 'SHA-256' },
      false, // no necesita ser exportable
      ['encrypt']
    )

    clavePublicaServidor = clave
    idClaveServidor = datos.idClave
    return clave
  })()

  try {
    return await peticionEnCurso
  } finally {
    peticionEnCurso = null
  }
}

export function obtenerIdClaveServidor(): string | null {
  return idClaveServidor
}

// ---------------------------------------------------------------------------
//  Cifrado y descifrado
// ---------------------------------------------------------------------------

/**
 * Genera una clave AES-256-GCM desechable y la envuelve con la clave publica
 * RSA del servidor. Se llama UNA vez por peticion: si alguien capturara el
 * trafico y rompiera una clave, solo veria esa peticion.
 */
export async function crearSobre(forzarRecargaClave = false): Promise<SobreCifrado> {
  const clavePublica = await obtenerClavePublica(forzarRecargaClave)

  const claveAes = await crypto.subtle.generateKey(
    { name: 'AES-GCM', length: 256 },
    true, // exportable: hay que poder envolverla para mandarla
    ['encrypt', 'decrypt']
  )

  const claveEnBruto = await crypto.subtle.exportKey('raw', claveAes)
  const envuelta = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, clavePublica, claveEnBruto)

  return { claveAes, claveEnvueltaBase64: bytesABase64(envuelta) }
}

/**
 * Cifra un texto (el JSON de la peticion) con AES-256-GCM.
 * Devuelve el IV y el cuerpo, ambos en base64.
 *
 * El IV es aleatorio en CADA mensaje: reutilizar el par (clave, IV) en GCM
 * rompe la seguridad del cifrado por completo.
 */
export async function cifrarTexto(
  claveAes: CryptoKey,
  texto: string
): Promise<{ ivBase64: string; cuerpoBase64: string }> {
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const datos = new TextEncoder().encode(texto)

  const cifrado = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, claveAes, datos)

  return { ivBase64: bytesABase64(iv), cuerpoBase64: bytesABase64(cifrado) }
}

/**
 * Descifra la respuesta del backend. Si el mensaje fue alterado por el camino,
 * AES-GCM falla aqui (lanza excepcion) en vez de devolver datos corruptos.
 */
export async function descifrarTexto(
  claveAes: CryptoKey,
  ivBase64: string,
  cuerpoBase64: string
): Promise<string> {
  const iv = base64ABuffer(ivBase64)
  const cifrado = base64ABuffer(cuerpoBase64)

  const plano = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, claveAes, cifrado)
  return new TextDecoder().decode(plano)
}

// ===========================================================================
//  tipos.ts -- Los "contratos" de datos entre el frontend y el backend
// ===========================================================================
//
// Esta es la gran ventaja de TypeScript en un proyecto front-back: describimos
// AQUI la forma exacta de los datos que enviamos y recibimos. Si el backend
// devuelve algo distinto, o si usamos mal un campo, el editor nos avisa ANTES
// de ejecutar. Es documentacion viva del contrato de la API.

export interface Animales{
  id?: number;
  nombre: string;
  especie: string;
  usuario?: Usuario;
  raza?: string;
  edad?: number;
  sexo: string;
  peso?: number;
  usuario_id: number;
}


export interface Roles {
  id: number;
  nombre: string;
  descripcion?: string;
}

/** Un usuario tal como lo devuelve el backend (fijate: sin la contrasena). */
export interface Usuario {
  id: number
  nombre: string
  email: string
  rol_id?: number;
}

/** Lo que responde el backend al endpoint de login. */
export interface LoginResponse {
  exito: boolean
  mensaje: string
  nombre: string | null
  email: string | null
  token: string | null
}

/** El usuario ya autenticado que guardamos en la app tras un login correcto. */
export interface UsuarioSesion {
  id: number;
  nombre: string
  email: string
  rol?: { id: number; nombre: string } | number | null
  rol_id?: number
}

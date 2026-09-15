# Demo Usuarios — Comunicación Front ↔ Back

Proyecto didáctico para un curso básico. Enseña **cómo se comunican un frontend (React) y un backend (Spring Boot)** a través de una API REST, sin la complejidad de seguridad ni base de datos.

- **Backend (Spring Boot):** expone 2 endpoints principales (login y listar usuarios) + 1 extra para crear. Guarda todo **en memoria** (una lista).
- **Frontend (React + TypeScript + Vite):** pantalla de login que, al validar contra el backend, lleva a un dashboard con la lista de usuarios y un formulario para crear. Usa **Yarn** como gestor de paquetes.

> ⚠️ **No usar en producción.** Las contraseñas van en texto plano y no hay seguridad, a propósito, para que el foco esté en la comunicación front-back.

---

## Qué se aprende aquí

1. Cómo el frontend **envía datos** al backend (POST con JSON en el login y al crear).
2. Cómo el frontend **pide datos** al backend (GET para listar).
3. Cómo el backend responde con **distintos códigos HTTP** (200 ok, 401 credenciales malas, 201 creado, 409 email repetido) y cómo el frontend reacciona a cada uno.
4. Qué es **CORS** y por qué hay que configurarlo cuando front y back corren en puertos distintos.
5. Cómo se hace una "redirección" en una app de una sola página (SPA): no se cambia de URL, se cambia **qué componente se muestra**.
6. Cómo **TypeScript** describe el contrato de datos entre front y back (archivo `src/tipos.ts`): si el backend cambiara la forma de la respuesta, el editor avisaría al instante.

---

## Requisitos

- **Java 17 o superior** (para el backend).
- **Maven** (para compilar/ejecutar el backend). Si no lo tienes, puedes usar el wrapper `mvnw` si lo generas, o instalarlo.
- **Node.js 18 o superior** y **Yarn** (para el frontend). Este proyecto usa Vite 5, que funciona en Node 20.9.
  - Si no tienes Yarn: `npm install -g yarn` (o actívalo con `corepack enable`).

---

## Cómo ejecutar

Necesitas **dos terminales**: una para el backend y otra para el frontend. **Levanta primero el backend.**

### 1. Backend (puerto 8080)

```bash
cd backend
mvn spring-boot:run
```

Cuando veas en la consola `API lista en http://localhost:8080`, ya está corriendo.

Puedes probarlo directo en el navegador o con curl:

```bash
curl http://localhost:8080/api/usuarios
```

### 2. Frontend (puerto 5173)

En **otra** terminal:

```bash
cd frontend
yarn install    # solo la primera vez
yarn dev
```

Abre lo que indique la consola: **http://localhost:5173**

---

## Cómo se usa

1. Entra a http://localhost:5173 → verás la pantalla de **login**.
2. Ingresa con un usuario de prueba:
   - `ana@ejemplo.cl` / `1234`
   - `luis@ejemplo.cl` / `1234`
   - `marta@ejemplo.cl` / `1234`
3. Al iniciar sesión correctamente, pasas al **dashboard** con la lista de usuarios.
4. Usa el formulario **Crear usuario** para agregar uno nuevo; la lista se actualiza al instante.
5. Prueba a crear un usuario con un email ya existente: el backend responde **409** y verás el mensaje de error. Prueba también a poner una contraseña incorrecta en el login: verás el error del **401**.

> Como los datos están en memoria, **al reiniciar el backend se pierden** y vuelven a aparecer los 3 usuarios de ejemplo. Es lo esperado en esta demo.

---

## Los endpoints del backend

| Método | Ruta | Qué hace | Respuestas |
|--------|------|----------|------------|
| `POST` | `/api/login` | Valida email + contraseña | `200` ok · `401` credenciales incorrectas |
| `GET`  | `/api/usuarios` | Devuelve la lista de usuarios | `200` con el array |
| `POST` | `/api/usuarios` | Crea un usuario nuevo | `201` creado · `409` email repetido · `400` datos inválidos |

Ejemplo de login con curl:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@ejemplo.cl","password":"1234"}'
```

---

## Estructura del proyecto

```
demo-usuarios/
├── backend/                        # Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/cl/curso/usuarios/
│       │   ├── Aplicacion.java             # arranque
│       │   ├── model/Usuario.java          # el modelo de datos
│       │   ├── service/UsuarioService.java # la lista en memoria + logica
│       │   └── web/
│       │       ├── UsuarioController.java  # los endpoints
│       │       ├── ConfiguracionCors.java  # permite las llamadas del front
│       │       ├── LoginRequest.java       # datos que entran al login
│       │       ├── LoginResponse.java      # datos que salen del login
│       │       └── CrearUsuarioRequest.java
│       └── resources/
│           └── application.properties      # puerto 808
│
└── frontend/                       # React + TypeScript + Vite (Yarn)
    ├── package.json
    ├── yarn.lock
    ├── tsconfig.json               # configuracion de TypeScript
    ├── tsconfig.node.json
    ├── vite.config.ts              # puerto 5173
    ├── index.html
    └── src/
        ├── main.tsx                # monta la app
        ├── App.tsx                 # decide: login o dashboard
        ├── Login.tsx               # pantalla de login
        ├── Dashboard.tsx           # lista + crear usuario
        ├── api.ts                  # TODAS las llamadas al backend (tipadas)
        ├── tipos.ts                # los "contratos" de datos (interfaces)
        └── estilos.css
```

Los archivos clave para entender la comunicación son **`frontend/src/api.ts`** (todas las llamadas al backend) y **`frontend/src/tipos.ts`** (la forma exacta de los datos que viajan).

---

## El flujo completo, paso a paso

1. Escribes email y contraseña en **Login.jsx** y presionas *Ingresar*.
2. Login.jsx llama a `login()` de **api.ts**, que hace un `POST /api/login` con los datos en JSON.
3. En el backend, **UsuarioController** recibe la petición, y **UsuarioService** busca el usuario en la lista.
4. El backend responde `200` (con el nombre) o `401` (credenciales malas).
5. Si fue `200`, Login.jsx avisa a **App.jsx**, que guarda el usuario y muestra el **Dashboard**.
6. Al montarse, Dashboard.jsx llama a `listarUsuarios()` → `GET /api/usuarios` y pinta la tabla.
7. Al crear un usuario, hace `POST /api/usuarios`; si el email se repite, el backend responde `409` y se muestra el mensaje.

Ese ida y vuelta —frontend que pide, backend que responde con datos y códigos— es exactamente lo que el proyecto busca enseñar.

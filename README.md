# ⚛️ Frontend - Gestión de Usuarios (React)

[![React](https://img.shields.io/badge/React-18+-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)

Cliente web SPA desarrollado en **React** para el módulo de autenticación y visualización de usuarios. Proporciona una interfaz ágil para iniciar sesión y consultar el listado de usuarios del sistema comunicándose con una API REST en Spring Boot.

> **Nota:** Este repositorio contiene exclusivamente la interfaz frontend. Requiere el servicio backend en ejecución: [Backend Spring Boot](https://github.com/Cokye/Backend-Clinica-React-angular).

---

## 📌 Funcionalidades

* **Inicio de Sesión:** Formulario de login controlado con validación de entradas.
* **Manejo de Estado:** Gestión del estado de usuario autenticado mediante hooks (`useState`, `useEffect` / Context).
* **Listado de Usuarios:** Renderizado dinámico de la lista de usuarios recuperada desde el backend.

---

## 🛠️ Stack Tecnológico y Dependencias

* **Librería base:** React (v18+)
* **Herramienta de compilación:** Vite
* **Lenguaje:** TypeScript / JavaScript
* **Dependencias principales:**
  * `react-router-dom`: Enrutamiento y navegación entre la pantalla de login y el listado.
  * `axios`: Cliente HTTP para el consumo de los endpoints REST.

---

## 📋 Requisitos Previos

* **Node.js:** Versión 18.x o 20.x (LTS) y `npm`.
* **Backend:** API Spring Boot en ejecución (puerto `8080`).

---

## 🚀 Instalación y Puesta en Marcha

### 1. Clonar el repositorio
```bash
git clone https://github.com/Cokye/Veterinario-Front-React
cd TU_REPOSITORIO_REACT

### 2. Instalar dependencias

```bash
npm install
```

---

### 3. Configurar Endpoint Backend

Crea o revisa el archivo `.env` en la raíz del proyecto:

```env
VITE_API_URL=http://localhost:8080/api
```

---

### 4. Ejecutar en modo desarrollo

```bash
npm run dev
```

Disponible en: `http://localhost:5173`

---

## 🔗 Integración

```
[ React (:5173) ] ---> POST /api/auth/login  ---> [ Spring Boot API (:8080) ]
[ React (:5173) ] ---> GET  /api/usuarios    ---> [ Spring Boot API (:8080) ]
```

---

## 👤 Autor

Desarrollado por **Felipe** ([@Cokye](https://github.com/Cokye))

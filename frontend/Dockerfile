# syntax=docker/dockerfile:1
# ===========================================================================
#  Dockerfile del FRONTEND (React + Vite + nginx)
# ===========================================================================
#
# Tambien multi-stage, y aqui la diferencia es todavia mas grande:
#
#   Etapa 1 (build) -> Node compila el proyecto y produce /app/dist:
#                      HTML, CSS y JS estaticos. Node ya no hace falta.
#   Etapa 2 (nginx) -> un servidor web minimo que sirve esos archivos.
#
# La imagen final NO lleva Node ni node_modules (que pesan cientos de MB):
# queda en ~50 MB. En produccion React no "se ejecuta" en el servidor, se
# ejecuta en el navegador del usuario; el servidor solo entrega archivos.

# ---------------------------------------------------------------------------
#  Etapa 1: compilar
# ---------------------------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

# Igual que en el backend: primero las dependencias (cambian poco), despues el
# codigo (cambia siempre). Asi Docker reutiliza la capa de node_modules.
# --frozen-lockfile obliga a respetar yarn.lock: si no calza, falla en vez de
# instalar versiones distintas a las que probaste.
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile

COPY . .

# Ejecuta "tsc && vite build" (ver package.json). Si hay un error de tipos, el
# build del contenedor falla aqui, que es justo lo que quieres.
RUN yarn build

# ---------------------------------------------------------------------------
#  Etapa 2: servir
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine

# Configuracion propia: hace falta para que las rutas de la SPA funcionen.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Solo el resultado del build. Nada de codigo fuente ni de node_modules.
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Se usa 127.0.0.1 y NO localhost: el servidor escucha solo en IPv4, pero
# dentro del contenedor "localhost" resuelve primero a ::1 (IPv6) y el chequeo
# fallaria con "Connection refused" aunque el servidor este perfecto.
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --spider -q http://127.0.0.1/ || exit 1

# La imagen de nginx ya trae su propio CMD para arrancar en primer plano.
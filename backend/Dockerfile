# syntax=docker/dockerfile:1
# ===========================================================================
#  Dockerfile del BACKEND (Spring Boot + Java 17)
# ===========================================================================
#
# Es una imagen "multi-stage": se construye en dos etapas.
#
#   Etapa 1 (build)   -> tiene Maven y el JDK completo. Compila y genera el .jar.
#   Etapa 2 (runtime) -> solo tiene el JRE. Copia el .jar de la etapa anterior.
#
# Por que dos etapas: la imagen final NO lleva Maven, ni el codigo fuente, ni
# el repositorio de dependencias. Pasa de ~800 MB a ~250 MB, y ademas reduce la
# superficie de ataque (menos cosas instaladas = menos vulnerabilidades).

# ---------------------------------------------------------------------------
#  Etapa 1: compilar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiamos SOLO el pom.xml primero y descargamos las dependencias.
# Truco de cache: mientras no cambies el pom.xml, Docker reutiliza esta capa y
# no vuelve a descargar internet entera en cada build.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Ahora si, el codigo fuente. Como cambia mas seguido, va en una capa aparte.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package

# ---------------------------------------------------------------------------
#  Etapa 2: ejecutar
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# curl es solo para el chequeo de salud de mas abajo. Se limpia la cache de apt
# en la MISMA instruccion RUN: si se hiciera en otra, los archivos ya habrian
# quedado grabados en la capa anterior y no se ahorraria nada.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# No ejecutar como root: si alguien logra escaparse de la aplicacion, se
# encuentra con un usuario sin privilegios en vez de con el administrador.
RUN useradd --system --uid 1001 --create-home spring
USER spring

# El asterisco evita tener que escribir la version a mano cada vez que cambie.
COPY --from=build /app/target/*.jar app.jar

# Documenta que el contenedor escucha aqui (no publica nada por si solo:
# eso lo hace -p en docker run, o "ports" en docker-compose).
EXPOSE 8080

# Chequeo de salud REAL: pega al endpoint de la clave publica, que es el unico
# que responde 200 sin autenticacion y sin cifrado. Si la app no levanto (por
# ejemplo, no pudo conectarse a Oracle), el contenedor se marca "unhealthy".
# start-period le da 40s de gracia al arranque de Spring antes de contar fallos.
#
# Se usa 127.0.0.1 y NO localhost: dentro del contenedor "localhost" puede
# resolver primero a ::1 (IPv6) y dar un "Connection refused" enganoso
# aunque el servidor este funcionando perfectamente en IPv4.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/api/crypto/public-key || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
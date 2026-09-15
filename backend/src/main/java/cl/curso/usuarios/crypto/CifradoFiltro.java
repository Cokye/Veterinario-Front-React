package cl.curso.usuarios.crypto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * EL CORAZON DE LA IMPLEMENTACION.
 *
 * Es un filtro servlet que se ejecuta ANTES que Spring Security (ver
 * CifradoConfig, que lo registra con orden -200; la cadena de Spring Security
 * usa el orden -100). Que corra primero es clave por dos motivos:
 *
 *   1. El cuerpo llega descifrado a todo lo que viene despues (Spring Security,
 *      Spring MVC, el controlador). Nadie mas necesita saber de cifrado.
 *   2. La respuesta 401 que genera Spring Security cuando falta el JWT tambien
 *      pasa de vuelta por aqui, asi que TAMBIEN sale cifrada.
 *
 * FORMATO EN EL CABLE
 *
 *   Peticion del navegador:
 *     X-Enc-Key: base64( RSA-OAEP-SHA256( clave AES-256 aleatoria ) )
 *     X-Enc-Iv : base64( IV de 12 bytes )          (solo si hay cuerpo)
 *     Content-Type: application/octet-stream
 *     cuerpo   : base64( AES-256-GCM( JSON original ) )
 *
 *   Respuesta del servidor (misma clave AES, IV distinto):
 *     X-Enc: 1
 *     X-Enc-Iv: base64( IV de 12 bytes )
 *     X-Enc-Content-Type: el Content-Type real de adentro
 *     cuerpo: base64( AES-256-GCM( respuesta original ) )
 *
 * Si algo falla al abrir el sobre, respondemos EN CLARO con la cabecera
 * X-Enc-Error, porque el navegador no tendria como descifrar ese error.
 */
public class CifradoFiltro extends OncePerRequestFilter {

    public static final String CABECERA_CLAVE = "X-Enc-Key";
    public static final String CABECERA_IV = "X-Enc-Iv";
    public static final String CABECERA_MARCA = "X-Enc";
    public static final String CABECERA_TIPO = "X-Enc-Content-Type";
    public static final String CABECERA_ERROR = "X-Enc-Error";
    public static final String CABECERA_ID_CLAVE = "X-Enc-Key-Id";

    /** Rutas que por definicion viajan en claro (si no, seria el huevo y la gallina). */
    private static final List<String> RUTAS_EN_CLARO = List.of("/api/crypto/public-key");

    private final CifradoService cifrado;
    private final boolean obligatorio;
    private final List<String> origenesPermitidos;

    public CifradoFiltro(CifradoService cifrado, boolean obligatorio, List<String> origenesPermitidos) {
        this.cifrado = cifrado;
        this.obligatorio = obligatorio;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ruta = request.getRequestURI();

        // El preflight CORS (OPTIONS) no lleva cuerpo ni debe tocarse: lo
        // resuelve la configuracion CORS de Spring Security.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || esRutaEnClaro(ruta)) {
            filterChain.doFilter(request, response);
            return;
        }

        String claveEnvuelta = request.getHeader(CABECERA_CLAVE);

        // ---- Peticion SIN cifrar -------------------------------------------
        if (claveEnvuelta == null || claveEnvuelta.isBlank()) {
            if (obligatorio && ruta.startsWith("/api/")) {
                responderErrorEnClaro(request, response, HttpServletResponse.SC_BAD_REQUEST,
                        "peticion-sin-cifrar",
                        "Esta API solo acepta peticiones cifradas. Falta la cabecera " + CABECERA_CLAVE + ".");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // ---- 1. Abrir el sobre RSA para recuperar la clave AES --------------
        SecretKey claveAes;
        try {
            claveAes = cifrado.abrirClaveAes(claveEnvuelta);
        } catch (Exception excepcion) {
            // Tipicamente pasa cuando el backend se reinicio y genero un par RSA
            // nuevo: el frontend ve este codigo, vuelve a pedir la clave publica
            // y reintenta la peticion una vez.
            responderErrorEnClaro(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "clave-desconocida",
                    "No se pudo abrir la clave de sesion. Vuelve a pedir la clave publica.");
            return;
        }

        // ---- 2. Descifrar el cuerpo de la peticion (si trae) ----------------
        HttpServletRequest peticionFinal = request;
        byte[] cuerpoCifrado = request.getInputStream().readAllBytes();

        if (cuerpoCifrado.length > 0) {
            String ivBase64 = request.getHeader(CABECERA_IV);
            if (ivBase64 == null || ivBase64.isBlank()) {
                responderErrorEnClaro(request, response, HttpServletResponse.SC_BAD_REQUEST,
                        "iv-faltante", "Falta la cabecera " + CABECERA_IV + ".");
                return;
            }
            try {
                byte[] iv = Base64.getDecoder().decode(ivBase64.trim());
                byte[] bytes = Base64.getDecoder().decode(new String(cuerpoCifrado, StandardCharsets.US_ASCII).trim());
                peticionFinal = new PeticionDescifrada(request, cifrado.descifrar(claveAes, iv, bytes));
            } catch (Exception excepcion) {
                // AES-GCM falla si el mensaje fue alterado: eso es una feature.
                responderErrorEnClaro(request, response, HttpServletResponse.SC_BAD_REQUEST,
                        "cuerpo-invalido", "No se pudo descifrar el cuerpo de la peticion.");
                return;
            }
        }

        // ---- 3. Dejar correr la aplicacion, guardando su respuesta ----------
        RespuestaEnBuffer respuestaBuffer = new RespuestaEnBuffer(response);
        try {
            filterChain.doFilter(peticionFinal, respuestaBuffer);
        } finally {
            enviarRespuestaCifrada(response, respuestaBuffer, claveAes);
        }
    }

    /** Cifra lo que la aplicacion escribio y recien ahi lo manda a la red. */
    private void enviarRespuestaCifrada(
            HttpServletResponse real, RespuestaEnBuffer buffer, SecretKey claveAes) throws IOException {

        byte[] cuerpo = buffer.getCuerpo();
        real.setStatus(buffer.getStatus());

        // Respuestas sin cuerpo (204, 404 vacio...) no tienen nada que cifrar.
        if (cuerpo.length == 0) {
            real.setContentLength(0);
            return;
        }

        try {
            byte[] iv = cifrado.nuevoIv();
            byte[] cifradoBytes = cifrado.cifrar(claveAes, iv, cuerpo);
            byte[] salida = Base64.getEncoder().encode(cifradoBytes);

            real.setHeader(CABECERA_MARCA, "1");
            real.setHeader(CABECERA_IV, Base64.getEncoder().encodeToString(iv));
            real.setHeader(CABECERA_ID_CLAVE, cifrado.getIdClave());
            if (buffer.getTipoContenidoOriginal() != null) {
                real.setHeader(CABECERA_TIPO, buffer.getTipoContenidoOriginal());
            }
            real.setContentType("application/octet-stream");
            real.setContentLength(salida.length);
            real.getOutputStream().write(salida);
            real.getOutputStream().flush();
        } catch (Exception excepcion) {
            real.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            real.setHeader(CABECERA_ERROR, "cifrado-respuesta");
            real.setContentType("text/plain;charset=UTF-8");
            real.getOutputStream().write(
                    "No se pudo cifrar la respuesta".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Errores del propio canal de cifrado: van EN CLARO a proposito, porque el
     * navegador todavia no tiene (o perdio) la clave con que descifrarlos.
     * Nunca contienen datos del usuario, solo el motivo tecnico.
     */
    private void responderErrorEnClaro(
            HttpServletRequest request, HttpServletResponse response,
            int estado, String codigo, String mensaje) throws IOException {

        // Este filtro corre ANTES del CORS de Spring Security, asi que la
        // cabecera de origen la ponemos nosotros o el navegador ocultaria el error.
        String origen = request.getHeader("Origin");
        if (origen != null && origenesPermitidos.contains(origen)) {
            response.setHeader("Access-Control-Allow-Origin", origen);
            // El frontend manda las peticiones con credentials: 'include' (por la
            // cookie de sesion). Sin esta cabecera el navegador descarta la
            // respuesta y el error nunca llega al codigo que sabe reaccionar.
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Expose-Headers",
                    String.join(", ", CABECERA_MARCA, CABECERA_IV, CABECERA_TIPO,
                            CABECERA_ERROR, CABECERA_ID_CLAVE));
        }

        response.setStatus(estado);
        response.setHeader(CABECERA_ERROR, codigo);
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"error\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    private boolean esRutaEnClaro(String ruta) {
        return RUTAS_EN_CLARO.stream().anyMatch(ruta::startsWith);
    }
}

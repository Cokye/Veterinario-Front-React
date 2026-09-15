package cl.curso.usuarios.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Guarda el JWT en una cookie HttpOnly en vez de en localStorage.
 *
 * POR QUE ES MAS SEGURO
 *
 * Con localStorage, CUALQUIER JavaScript que se ejecute en la pagina puede
 * hacer localStorage.getItem('token') y llevarse la sesion. Basta un XSS (un
 * comentario con <script>, una libreria de npm comprometida, una extension del
 * navegador) para robar el token.
 *
 * Una cookie marcada HttpOnly NO es visible desde JavaScript: document.cookie
 * ni siquiera la muestra. El navegador la adjunta solo en cada peticion, pero
 * el codigo de la pagina jamas la puede leer. Un XSS ya no puede robarla.
 *
 * ATRIBUTOS QUE USAMOS
 *
 *   HttpOnly  -> invisible para JavaScript (esta es la proteccion principal)
 *   SameSite  -> el navegador no manda la cookie en peticiones que vengan de
 *                otro sitio. Es la defensa contra CSRF.
 *   Secure    -> la cookie solo viaja por HTTPS. En desarrollo (http://localhost)
 *                va en false; en produccion SIEMPRE debe ir en true.
 *   Path=/api -> la cookie solo se manda a la API, no a recursos estaticos.
 *   Max-Age   -> la cookie caduca junto con el JWT, no antes ni despues.
 *
 * SOBRE CSRF: normalmente pasar de localStorage a cookies abre la puerta a
 * ataques CSRF, porque el navegador adjunta la cookie automaticamente aunque la
 * peticion la dispare otro sitio. En ESTE proyecto ese riesgo ya esta cubierto
 * por dos capas: SameSite=Strict, y sobre todo el cifrado del canal (ver
 * CIFRADO-COMUNICACION.md), porque un atacante externo no puede construir un
 * cuerpo cifrado valido sin mandar la cabecera X-Enc-Key, y esa cabecera obliga
 * a un preflight que CORS rechaza.
 */
@Component
public class CookieTokenService {

    /** Nombre de la cookie. Tambien lo usa JwtAuthFilter para leerla. */
    public static final String NOMBRE_COOKIE = "token";

    private final boolean segura;
    private final String sameSite;
    private final Duration duracion;

    public CookieTokenService(
            @Value("${app.cookie.secure:false}") boolean segura,
            @Value("${app.cookie.same-site:Strict}") String sameSite,
            @Value("${jwt.expiration-ms}") long expiracionMs) {
        this.segura = segura;
        this.sameSite = sameSite;
        // La cookie dura exactamente lo mismo que el JWT que lleva dentro.
        this.duracion = Duration.ofMillis(expiracionMs);
    }

    /** Cookie de inicio de sesion, con el JWT dentro. */
    public ResponseCookie crear(String token) {
        return base(token).maxAge(duracion).build();
    }

    /**
     * Cookie de cierre de sesion: misma cookie pero vacia y con Max-Age=0, que
     * es la forma estandar de decirle al navegador "borrala ahora".
     *
     * Ojo: hay que repetir Path y los demas atributos, porque el navegador solo
     * reemplaza la cookie si coinciden. Si no, quedaria la vieja viva.
     */
    public ResponseCookie borrar() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE_COOKIE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite(sameSite)
                .path("/api");
    }

    /** Busca el token en las cookies que mando el navegador. */
    public String leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NOMBRE_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

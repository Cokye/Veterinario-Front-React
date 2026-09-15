package cl.curso.usuarios.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cl.curso.usuarios.crypto.CifradoFiltro;

/**
 * Configuracion central de seguridad.
 *
 * Reemplaza a la antigua ConfiguracionCors: como Spring Security intercepta
 * las peticiones ANTES de que lleguen a Spring MVC, el CORS tiene que
 * declararse aqui (con .cors(...)) para que el preflight (OPTIONS) del
 * navegador tambien pase, y no solo las peticiones normales.
 *
 * Reglas:
 *   - GET  /api/crypto/public-key -> publica y EN CLARO (hay que poder pedir la
 *     clave publica antes de saber cifrar)
 *   - POST /api/login              -> publica, pero ya llega cifrada
 *   - POST /api/logout             -> publica (solo borra la cookie de sesion)
 *   - cualquier otra ruta /api/**  -> requiere un JWT valido, que llega en la
 *     cookie HttpOnly "token" (o, para curl/Postman, en el header
 *     Authorization: Bearer <token>)
 *
 * La app queda "stateless": el servidor no guarda sesiones, cada peticion se
 * autentica sola gracias al token (ver JwtAuthFilter).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final List<String> origenesPermitidos;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            @Value("${app.cors.origenes-permitidos}") List<String> origenesPermitidos) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(configuracionCors()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(manejo -> manejo.authenticationEntryPoint(
                        (request, response, excepcion) -> {
                            // OJO: antes se usaba response.sendError(...). Con el cifrado
                            // activo eso hacia que Tomcat generara su propia pagina de
                            // error FUERA de nuestro filtro, es decir, sin cifrar.
                            // Escribiendo el cuerpo aqui, el 401 pasa por CifradoFiltro
                            // igual que cualquier otra respuesta y sale cifrado.
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getOutputStream().write(
                                    "{\"error\":\"No autenticado\"}".getBytes(StandardCharsets.UTF_8));
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/crypto/public-key").permitAll()
                        .requestMatchers("/api/login").permitAll()
                        // Cerrar sesion debe funcionar aunque el token ya haya vencido:
                        // su trabajo es borrar la cookie, no leerla.
                        .requestMatchers("/api/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource configuracionCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite las cabeceras del cifrado (X-Enc-Key, X-Enc-Iv) ademas de Authorization.
        configuracion.setAllowedHeaders(List.of("*"));

        // Necesario para la cookie HttpOnly: sin esto el navegador NO adjunta la
        // cookie en peticiones a otro origen (el front va en :5173 y la API en
        // :8080), aunque el frontend use credentials: 'include'.
        // Ojo: con credenciales activas, allowedOrigins NO puede ser "*"; por eso
        // los origenes se declaran uno por uno en app.cors.origenes-permitidos.
        configuracion.setAllowCredentials(true);

        // SIN esto el navegador recibe las cabeceras del cifrado pero JavaScript
        // no puede leerlas, y el frontend no sabria con que IV descifrar.
        configuracion.setExposedHeaders(List.of(
                CifradoFiltro.CABECERA_MARCA,
                CifradoFiltro.CABECERA_IV,
                CifradoFiltro.CABECERA_TIPO,
                CifradoFiltro.CABECERA_ERROR,
                CifradoFiltro.CABECERA_ID_CLAVE));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}

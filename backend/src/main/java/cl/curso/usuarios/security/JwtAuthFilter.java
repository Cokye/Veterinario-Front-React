package cl.curso.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que se ejecuta en CADA peticion HTTP, antes de llegar al controlador.
 *
 * DE DONDE SACA EL TOKEN (en este orden):
 *
 *   1. La cookie HttpOnly "token": es como viaja desde el navegador. El
 *      frontend no la maneja ni la puede leer; la adjunta el navegador solo.
 *   2. El header "Authorization: Bearer <token>": se mantiene como alternativa
 *      para poder probar la API con curl o Postman, donde no hay cookies.
 *
 * Si el token es valido, marca la peticion como autenticada
 * (SecurityContextHolder), y entonces SecurityConfig la deja pasar. Si no hay
 * token o es invalido, simplemente no autentica: SecurityConfig se encarga de
 * rechazar (401) las rutas que requieren estar logueado.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final CookieTokenService cookieTokenService;

    public JwtAuthFilter(JwtService jwtService, CookieTokenService cookieTokenService) {
        this.jwtService = jwtService;
        this.cookieTokenService = cookieTokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null && jwtService.esValido(token)) {
            String email = jwtService.extraerEmail(token);

            var autenticacion = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }

        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        // Primero la cookie: es el camino normal desde el navegador.
        String desdeCookie = cookieTokenService.leer(request);
        if (desdeCookie != null) {
            return desdeCookie;
        }

        // Alternativa para clientes sin cookies (curl, Postman, tests).
        String cabeceraAuth = request.getHeader("Authorization");
        if (cabeceraAuth != null && cabeceraAuth.startsWith(PREFIJO_BEARER)) {
            return cabeceraAuth.substring(PREFIJO_BEARER.length());
        }

        return null;
    }
}

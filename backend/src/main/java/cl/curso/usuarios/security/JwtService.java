package cl.curso.usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los tokens JWT.
 *
 * Un JWT es un texto firmado digitalmente que contiene datos (claims), por
 * ejemplo el email del usuario y una fecha de vencimiento. El backend lo firma
 * con una clave secreta (jwt.secret); como el frontend no conoce esa clave, no
 * puede fabricar ni alterar un token valido. En cada peticion, el backend solo
 * necesita volver a verificar la firma: no hace falta guardar sesiones en el
 * servidor (por eso se dice que JWT es "stateless").
 */
@Component
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(
            @Value("${jwt.secret}") String secreto,
            @Value("${jwt.expiration-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    /** Crea un token nuevo para el usuario que acaba de iniciar sesion. */
    public String generarToken(String email, String nombre) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("nombre", nombre)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(clave)
                .compact();
    }

    /** Extrae el email (subject) desde un token ya validado. */
    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    /** true si la firma es valida y el token no esta vencido. */
    public boolean esValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException excepcion) {
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
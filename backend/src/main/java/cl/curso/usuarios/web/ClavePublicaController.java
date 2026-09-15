package cl.curso.usuarios.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.curso.usuarios.crypto.CifradoService;

/**
 * Unico endpoint que viaja EN CLARO, y tiene que ser asi:
 *
 *   GET http://localhost:8080/api/crypto/public-key
 *   -> { "algoritmo": "RSA-OAEP-256", "clavePublica": "MIIBIjANBg...", "idClave": "a1b2..." }
 *
 * Es el "handshake": el frontend necesita la clave publica ANTES de poder
 * cifrar nada. Publicarla no es un riesgo: con la clave publica solo se puede
 * CIFRAR; para descifrar hace falta la privada, que nunca sale del servidor.
 *
 * 'idClave' es una huella corta de la clave. El frontend la guarda para
 * detectar cuando el backend se reinicio y genero un par nuevo.
 */
@RestController
@RequestMapping("/api/crypto")
public class ClavePublicaController {

    private final CifradoService cifradoService;

    public ClavePublicaController(CifradoService cifradoService) {
        this.cifradoService = cifradoService;
    }

    @GetMapping("/public-key")
    public Map<String, String> clavePublica() {
        return Map.of(
                "algoritmo", "RSA-OAEP-256",
                "formato", "spki",
                "cifradoCuerpo", "AES-256-GCM",
                "clavePublica", cifradoService.getClavePublicaBase64(),
                "idClave", cifradoService.getIdClave());
    }
}

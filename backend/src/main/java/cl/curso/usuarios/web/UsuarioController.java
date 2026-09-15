package cl.curso.usuarios.web;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.curso.usuarios.model.Usuario;
import cl.curso.usuarios.security.CookieTokenService;
import cl.curso.usuarios.security.JwtService;
import cl.curso.usuarios.service.UsuarioService;
import jakarta.validation.Valid;

/**
 * Controlador REST: aqui viven los endpoints que el frontend llama.
 *
 * @RestController le dice a Spring que esta clase maneja peticiones HTTP y que
 * lo que devuelvan sus metodos se convierte automaticamente a JSON.
 *
 * @RequestMapping("/api") pone ese prefijo a todas las rutas de abajo.
 *
 * El servicio se recibe por el constructor (inyeccion de dependencias): Spring
 * nos entrega la instancia de UsuarioService sin que tengamos que crearla.
 */
@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtService jwtservice;
    private final CookieTokenService cookie;

    public UsuarioController(UsuarioService usuarioService, JwtService jwtservice, CookieTokenService cookie) {
        this.usuarioService = usuarioService;
        this.jwtservice = jwtservice;
        this.cookie = cookie;
    }

    /**
     * ENDPOINT 1 -- Iniciar sesion.
     *
     *   POST http://localhost:8080/api/login
     *   Cuerpo: { "email": "ana@ejemplo.cl", "password": "1234" }
     *
     * @RequestBody convierte el JSON que llega en un objeto LoginRequest.
     * @Valid activa las validaciones (@NotBlank) del DTO.
     *
     * Si las credenciales son correctas, responde 200 con los datos del usuario.
     * Si no, responde 401 (no autorizado) con un mensaje.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest peticion) {

        Optional<Usuario> encontrado =
                usuarioService.buscarPorCredenciales(peticion.getEmail(), peticion.getPassword());

        if (encontrado.isPresent()) {
            Usuario u = encontrado.get();
            String token = jwtservice.generarToken(u.getEmail(), u.getNombre());

            LoginResponse ok = new LoginResponse(true, "Bienvenido/a " + u.getNombre(), u.getNombre(), u.getEmail(), u.getRol_id(), u.getId(), token);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE , cookie.crear(token).toString()).body(ok);
        }

        // Credenciales incorrectas: 401 Unauthorized.
        LoginResponse error = new LoginResponse(
                false, "Email o contrasena incorrectos", null, null, null, null, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * ENDPOINT 2 -- Listar usuarios.
     *
     *   GET http://localhost:8080/api/usuarios
     *
     * Devuelve la lista completa de usuarios en formato JSON.
     */
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        return usuarioService.listar();
    }

    /**
     * ENDPOINT EXTRA -- Crear un usuario (para el CRUD del dashboard).
     *
     *   POST http://localhost:8080/api/usuarios
     *   Cuerpo: { "nombre": "...", "email": "...", "password": "..." }
     *
     * Comprueba que el email no este repetido. Si lo esta, responde 409
     * (conflicto). Si todo va bien, responde 201 (creado) con el usuario nuevo.
     */
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody CrearUsuarioRequest peticion) {

        if (usuarioService.existeEmail(peticion.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ya existe un usuario con ese email");
        }

        Usuario nuevo = usuarioService.crear(new Usuario(
                null, peticion.getNombre(), peticion.getEmail(), peticion.getPassword(), peticion.getRol_id()));

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }
    @PutMapping("/actualizarUsuarios")
    public ResponseEntity<?> actualizarUsuario(@RequestBody UpdateUsuarioRequest updatepassword){
         
        Usuario encontrado =
                usuarioService.actualizarUsuario(updatepassword);

        if (encontrado != null) {
            return ResponseEntity.ok(encontrado);
        }
        return ResponseEntity.notFound().build();
    }
}

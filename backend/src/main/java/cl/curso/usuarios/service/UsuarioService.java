package cl.curso.usuarios.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import cl.curso.usuarios.model.Usuario;
import cl.curso.usuarios.repository.UsuarioRepository;
import cl.curso.usuarios.web.UpdateUsuarioRequest;

/**
 * Guarda y gestiona los usuarios EN MEMORIA (en una lista).
 *
 * Importante para el curso: al estar en memoria, los datos se pierden cada vez
 * que reinicias la aplicacion. En un proyecto real, aqui iria una base de datos
 * (con Spring Data JPA, por ejemplo). Lo dejamos en una lista para que se
 * entienda el flujo sin la complejidad de configurar una base.
 *
 * La anotacion @Service le dice a Spring que administre esta clase como un
 * "bean": crea una unica instancia y la inyecta donde se necesite (en el
 * controlador). Asi todos comparten la misma lista.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    // La "base de datos" en memoria: una simple lista.
    private final List<Usuario> usuarios = new ArrayList<>();

    // Generador de ids. AtomicLong asegura que el numero sea unico aunque haya
    // varias peticiones a la vez.
    private final AtomicLong secuenciaId = new AtomicLong(0);

    // Antes de guardar hay que validar que el rol exista
    //private final RolRepository rolRepository;

    /**
     * Al crear el servicio, cargamos algunos usuarios de ejemplo para que la
     * lista no aparezca vacia la primera vez.
     */
    public UsuarioService(UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;  //Con esto se puede usar todos los metodos

    }

    /** Devuelve todos los usuarios. */
    public List<Usuario> listar() {
        return this.usuarioRepository.findAll();
    }

    /**
     * Agrega un usuario nuevo a la lista.
     * Le asignamos un id automatico antes de guardarlo.
     */
    public Usuario crear(Usuario usuario) {

        return this.usuarioRepository.save(usuario);
    }

    /**
     * Busca un usuario por su email y contrasena.
     *
     * Devuelve un Optional: si lo encuentra, viene con el usuario dentro; si no,
     * viene vacio. Es la forma en Java de decir "puede que haya resultado o no"
     * sin arriesgar un null.
     */
    public Optional<Usuario> buscarPorCredenciales(String email, String password) {
        
        return this.usuarioRepository.findByEmailAndPassword(email, password);
    }

       public Optional<Usuario> buscarPorEmail(String email) {

        return this.usuarioRepository.findByEmail(email);
    }

    /** Indica si ya existe un usuario con ese email (para no repetirlos). */

    // PREGUNTAR AL PROFE SOBRE ESTA FUNCION
    public boolean existeEmail(String email) {
        return usuarios.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public Usuario actualizarUsuario(UpdateUsuarioRequest updateUsuario){
        
        Optional<Usuario> encontrado =
                buscarPorEmail(updateUsuario.getEmail());
                
        if (encontrado.isPresent()) {
            Usuario u = encontrado.get();
            u.setPassword(updateUsuario.getPassword());
            u.setEmail(updateUsuario.getEmail());
            u.setNombre(updateUsuario.getNombre());
            return this.usuarioRepository.save(u);
        }
        return null;
    }
}

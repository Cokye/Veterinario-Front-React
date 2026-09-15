package cl.curso.usuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa un usuario del sistema.
 *
 * Es una clase de datos sencilla: solo guarda informacion. En un proyecto real
 * la contrasena NUNCA se guardaria en texto plano (se usaria un hash como
 * BCrypt), pero como esto es un curso basico sobre la comunicacion front-back,
 * lo dejamos simple a proposito.
 */
@Table(name = "usuarios")
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String email;
    private String password;
    private Long rol_id;

    // Un constructor vacio es necesario para que Jackson (la libreria que
    // convierte JSON a objetos Java) pueda crear la instancia.
    public Usuario() {
    }

    
    public Usuario(Long id, String nombre, String email, String password, Long rol_id) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol_id = rol_id;
    }

    // Getters y setters: Jackson los usa para leer y escribir los campos.
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getRol_id() { return rol_id; }
    public void setRol_id(Long rol_id) { this.rol_id = rol_id; }
    
}

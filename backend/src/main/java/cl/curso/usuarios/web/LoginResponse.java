package cl.curso.usuarios.web;

/**
 * Respuesta que el backend devuelve tras un login.
 *
 * Fijate que NO incluimos la contrasena: solo devolvemos lo que el frontend
 * necesita para mostrar (el nombre y el email del usuario que entro).
 * Un DTO de respuesta sirve justamente para controlar que datos salen.
 */
public class LoginResponse {

    private boolean exito;
    private String mensaje;
    private String nombre;
    private String email;
    private Long rol_id;
    private Long id;
    private String token;

    public LoginResponse(boolean exito, String mensaje, String nombre, String email, Long rol_id, Long id, String token) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.nombre = nombre;
        this.email = email;
        this.rol_id = rol_id;
        this.id = id;
        this.token = token;
    }

    public boolean isExito() { return exito; }
    public String getMensaje() { return mensaje; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public Long getRol_id() {return rol_id;}
    public Long getId() {return id;}
    public String getToken() {return token;}
    

    
    
}

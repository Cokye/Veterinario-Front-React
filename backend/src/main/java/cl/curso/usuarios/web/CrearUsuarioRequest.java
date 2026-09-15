package cl.curso.usuarios.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear un usuario nuevo desde el formulario del dashboard.
 *
 * Igual que LoginRequest, es un DTO de entrada. @Email comprueba que el correo
 * tenga un formato valido.
 */
public class CrearUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    @NotNull(message = "El Rol es obligatorio")
    private Long rol_id;


    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getRol_id() { return rol_id; }
    public void setRol_id(Long rol_id) { this.rol_id = rol_id; }
}

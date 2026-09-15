package cl.curso.usuarios.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos que el frontend envia al iniciar sesion.
 *
 * Esto es un "DTO" (Data Transfer Object): un objeto cuyo unico proposito es
 * transportar los datos de una peticion. El frontend manda un JSON como:
 *   { "email": "ana@ejemplo.cl", "password": "1234" }
 * y Spring lo convierte automaticamente en un objeto LoginRequest.
 *
 * Las anotaciones @NotBlank validan que ninguno venga vacio; si vienen vacios,
 * Spring responde un error 400 sin llegar siquiera a nuestro codigo.
 */
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

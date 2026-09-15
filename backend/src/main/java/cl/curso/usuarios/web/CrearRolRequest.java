package cl.curso.usuarios.web;

import jakarta.validation.constraints.NotBlank;

public class CrearRolRequest {
    
    @NotBlank( message  = "El nombre es obligatorio")
    private String nombre;

    @NotBlank( message = "La descripcion es obligatoria")
    private String descripcion;

    public String getNombre(){ return nombre;}
    public void setNombre(String nombre){this.nombre = nombre;}

    public String getDescripcion(){ return descripcion;}
    public void setDecripcion(String descripcion){ this.descripcion = descripcion;}


}

package cl.curso.usuarios.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CrearAnimalesRequest {

    @NotBlank( message= "El nombre es obligatorio")
    private String nombre;

    @NotBlank( message= "La especie es obligatorio")
    private String especie;

    @NotBlank( message= "La raza es obligatorio")
    private String raza;

    @NotNull( message= "La edad es obligatorio")
    private Integer edad;

    @NotBlank( message= "El sexo es obligatorio")
    private String sexo;

    @NotNull( message= "El peso es obligatorio")
    private Double peso;

    @NotNull( message= "El Responsable es obligatorio")
    private Long usuario_id;


    public String getNombre() {return nombre;}
    public String getEspecie() {return especie;}
    public String getRaza() {return raza;}
    public Integer getEdad() {return edad;}
    public String getSexo() {return sexo;}
    public Double getPeso() {return peso;}
    public Long getUsuario_id() {return usuario_id;}
    
    // Todos los setters
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setEspecie(String especie) {this.especie = especie;}
    public void setRaza(String raza) {this.raza = raza;}
    public void setEdad(Integer edad) {this.edad = edad;}
    public void setSexo(String sexo) {this.sexo = sexo;}
    public void setPeso(Double peso) {this.peso = peso;}
    public void setUsuario_id(Long usuario_id) {this.usuario_id = usuario_id;}
}

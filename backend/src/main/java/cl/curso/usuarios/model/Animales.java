package cl.curso.usuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name= "animales")
@Entity
public class Animales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private Integer edad;
    private String sexo;
    private Double peso;
    private Long usuario_id;

    public Animales(){

    }

    public Animales(Long id, String nombre, String especie, String raza, Integer edad, String sexo, Double peso, Long usuario_id){
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.edad = edad;
        this.sexo = sexo;
        this.peso = peso;
        this.usuario_id = usuario_id;
    }

    // Todos los geters
    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public String getEspecie() {return especie;}
    public String getRaza() {return raza;}
    public Integer getEdad() {return edad;}
    public String getSexo() {return sexo;}
    public Double getPeso() {return peso;}
    public Long getUsuario_id() {return usuario_id;}
    
    // Todos los setters
    public void setId(Long id) {this.id = id;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setEspecie(String especie) {this.especie = especie;}
    public void setRaza(String raza) {this.raza = raza;}
    public void setEdad(Integer edad) {this.edad = edad;}
    public void setSexo(String sexo) {this.sexo = sexo;}
    public void setPeso(Double peso) {this.peso = peso;}
    public void setUsuario_id(Long usuario_id) {this.usuario_id = usuario_id;}
    
    
    
    
}

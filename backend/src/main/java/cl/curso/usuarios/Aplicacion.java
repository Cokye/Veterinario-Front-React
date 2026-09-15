package cl.curso.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion.
 *
 * La anotacion @SpringBootApplication le dice a Spring Boot que este es el
 * arranque de la app. Al ejecutar el metodo main, se levanta un servidor web
 * (Tomcat embebido) que queda escuchando peticiones en http://localhost:8080.
 */
@SpringBootApplication
public class Aplicacion {

    public static void main(String[] args) {
        SpringApplication.run(Aplicacion.class, args);
        System.out.println("\n  API lista en http://localhost:8080\n");
    }
}

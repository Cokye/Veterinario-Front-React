package cl.curso.usuarios.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.curso.usuarios.model.Animales;



@Repository
public interface AnimalesRepository extends JpaRepository<Animales, Long> {
    Optional<Animales> findById(Long id);
    Optional<Animales> findByNombre(String nombre);
}

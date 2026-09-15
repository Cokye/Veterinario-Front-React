package cl.curso.usuarios.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.curso.usuarios.model.Roles;

@Repository
public interface RolRepository extends JpaRepository<Roles, Long> {
    
    Optional<Roles> findById(Long id);
    Optional<Roles> findByNombre(String nombre);

}
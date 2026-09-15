package cl.curso.usuarios.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.curso.usuarios.model.Roles;
import cl.curso.usuarios.repository.RolRepository;



@Service
public class RolesService {
    
    private final RolRepository rolRepository;

    private final List<Roles> roles = new ArrayList<>();

    public RolesService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<Roles> listar() {
        return this.rolRepository.findAll();
    }

    public Optional<Roles> buscarPorId(Long id) {
        return this.rolRepository.findById(id);
    }


    // Poder crear nuevos roles 
    public Roles crear(Roles roles) {

        return this.rolRepository.save(roles);
    }

    //Revisar si existe otro nombre
    public boolean existeNombre(String nombre){
        return roles.stream()
                .anyMatch(r -> r.getNombre().equalsIgnoreCase(nombre));
    }

}

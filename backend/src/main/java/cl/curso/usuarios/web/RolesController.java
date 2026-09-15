package cl.curso.usuarios.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.curso.usuarios.model.Roles;
import cl.curso.usuarios.service.RolesService;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api")
public class RolesController {
    
    private final RolesService rolesService;

    public RolesController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    @GetMapping("/roles")
    public List<Roles> listarUsuarios() {
        return rolesService.listar();
    }

    // Crear roles nuevos
    @PostMapping("/roles")
    public ResponseEntity<?> crearRol(@Valid @RequestBody CrearRolRequest peticion) {
        
        if (rolesService.existeNombre(peticion.getNombre())){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ya existe un rol con este nombre");
        }
        
        Roles nuevo = rolesService.crear(new Roles(
                null, peticion.getNombre(), peticion.getDescripcion()));

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
      
    }
    
}

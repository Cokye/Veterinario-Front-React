package cl.curso.usuarios.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.curso.usuarios.model.Animales;
import cl.curso.usuarios.service.AnimalesService;
import cl.curso.usuarios.service.RolesService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AnimalesController {

    private final AnimalesService animalesService;

    public AnimalesController(AnimalesService animalesService, RolesService rolesService){
        this.animalesService = animalesService;
    }

    @GetMapping("/animales")
    public List<Animales> listarAnimales(){
        return animalesService.listar();
    }
    
    @PostMapping("/animales")
    public ResponseEntity<?> crearAnimal(@Valid @RequestBody CrearAnimalesRequest peticion){
        
        if(animalesService.existeNombre(peticion.getNombre())){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ya existe un rol con este nombre");
        }
        
        Animales nuevo = animalesService.crear(new Animales
            (null, peticion.getNombre(), peticion.getEspecie(), peticion.getRaza(), peticion.getEdad(), peticion.getSexo(), peticion.getPeso(), peticion.getUsuario_id()));

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);

    }

}

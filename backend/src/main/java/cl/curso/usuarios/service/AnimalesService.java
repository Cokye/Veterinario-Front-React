package cl.curso.usuarios.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.curso.usuarios.model.Animales;
import cl.curso.usuarios.repository.AnimalesRepository;

@Service
public class AnimalesService {
    
    private final AnimalesRepository animalesRepository;

    private final List<Animales> animales = new ArrayList<>();

    public AnimalesService(AnimalesRepository animalesRepository){
        this.animalesRepository = animalesRepository;
    }

    public List<Animales> listar(){
        return this.animalesRepository.findAll();
    }


    //Estos dos buscan por apartados distintos
    public Optional<Animales> buscarPorId(Long id){
        return this.animalesRepository.findById(id);
    }

    //Crea un nuemo animal
    public Animales crear(Animales animales){
        return this.animalesRepository.save(animales);
    }

    //Revisar si existe otro
    public boolean existeNombre(String nombre){
        return animales.stream()
                .anyMatch(a -> a.getNombre().equalsIgnoreCase(nombre));
    }
}

package data.repositories;

import jakarta.persistence.EntityManager;
import model.Comentario;

public class ComentarioRepositorio extends BaseRepositorio {

    public boolean borrar(Long id, EntityManager em) {
        return borrarBase(id, Comentario.class, em);
    }

    public Comentario buscarPorId(Long id, EntityManager em) {
        return buscarPorIdBase(id, Comentario.class, em);
    }
}

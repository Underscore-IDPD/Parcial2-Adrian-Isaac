package service;

import data.repositories.ComentarioRepositorio;
import jakarta.persistence.EntityManager;
import model.*;

import java.time.LocalDateTime;
import java.util.Objects;

public class ComentarioServicio {

    private final ComentarioRepositorio comentarioRepositorio;

    public ComentarioServicio(ComentarioRepositorio repositorio) {
        comentarioRepositorio = repositorio;
    }

    public void agregarComentario(String texto, Usuario autor, Evento evento, EntityManager em) {

        Comentario c = new Comentario(
                texto,
                autor,
                evento,
                LocalDateTime.now()
        );

        em.getTransaction().begin();
        boolean estado = comentarioRepositorio.guardar(c,em);
        if(!estado) {
            em.getTransaction().rollback();
            throw new RuntimeException("No se pudo guardar comentario");
        }
        else em.getTransaction().commit();
    }

    public void eliminarComentario(long id, EntityManager em) {
        if(!comentarioRepositorio.borrar(id,em))
            throw new RuntimeException("Comentario no encontrado");
    }

    public Comentario buscarPorId(long id, EntityManager em) {
        Comentario cmt = comentarioRepositorio.buscarPorId(id, em);
        if (cmt == null) {
            throw new RuntimeException("Comentario no encontrado");
        }
        return cmt;
    }

    public void modificarComentario(Long id, String texto, EntityManager em) {
        Comentario c = buscarPorId(id,em);
        if(Objects.nonNull(c)) {
            c.setTexto(texto);
            em.getTransaction().begin();
            comentarioRepositorio.actualizar(c,em);
            em.getTransaction().commit();
        }
    }
}

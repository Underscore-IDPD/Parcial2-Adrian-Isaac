package data.repositories;

import jakarta.persistence.*;
import model.Evento;

import java.util.List;

public class EventoRepositorio extends BaseRepositorio {

    public boolean borrar(Long id, EntityManager em) {
        return borrarBase(id, Evento.class, em);
    }

    public Evento buscarPorId(Long id, EntityManager em) {
        return buscarPorIdBase(id, Evento.class, em);
    }

    public List<Evento> listarPaginado(int pagina, EntityManager em) {
        assert em != null;
        return em.createQuery(
                        "SELECT e FROM Evento e ORDER BY e.fechaEvento DESC", Evento.class)
                .setFirstResult(pagina * 5)
                .setMaxResults(5)
                .getResultList();

    }

    public Evento buscarPorConceptoYOrganizador(String concepto, Long uid, EntityManager em) {
        assert em != null;
        TypedQuery<Evento> query = em.createQuery(
                "SELECT e FROM Evento e WHERE e.concepto = :concepto AND e.organizador.id = :idOrganizador",
                Evento.class);
        query.setParameter("concepto", concepto);
        query.setParameter("idOrganizador", uid);

        List<Evento> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    public Evento buscarPorConceptoYDescripcion(String concepto, String descripcion, EntityManager em) {
        assert em != null;
        TypedQuery<Evento> query = em.createQuery(
                "SELECT e FROM Evento e WHERE e.concepto = :concepto AND e.descripcion = :descripcion",
                Evento.class);
        query.setParameter("concepto", concepto);
        query.setParameter("descripcion", descripcion);

        List<Evento> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    public List<Evento> buscarPorEtiqueta(String nombre, EntityManager em) {
        return em.createQuery(
                        "SELECT a FROM Evento a JOIN a.etiquetas e WHERE e.nombre = :nombre",
                        Evento.class)
                .setParameter("nombre", nombre)
                .getResultList();
    }

}

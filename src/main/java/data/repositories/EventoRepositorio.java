package data.repositories;

import jakarta.persistence.*;
import model.Evento;
import model.Inscripcion;

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

    public Inscripcion buscarInscripcionPorToken(String token, EntityManager em){
        TypedQuery<Inscripcion> query = em.createQuery(
                        "SELECT i FROM Inscripcion i WHERE i.token = :token ", Inscripcion.class)
                .setParameter("token", token);
        List<Inscripcion> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    public int contarInscritos(Long id, EntityManager em){
        return em.createQuery(
                        "SELECT COUNT i FROM Inscripcion i WHERE i.evento.id = :id ", Inscripcion.class)
                .setParameter("id", id).getFirstResult();
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

    public boolean verificarInscripcion(Long uid, long id, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT i FROM Inscripcion i WHERE i.evento.id = :id AND i.usuario = uid ", Inscripcion.class)
                .setParameter("id", id)
                .setParameter("uid",uid)
                .getFirstResult() == 1;
    }
}

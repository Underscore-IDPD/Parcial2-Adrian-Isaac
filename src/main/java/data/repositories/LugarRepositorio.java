package data.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Lugar;

import java.util.List;

public class LugarRepositorio extends BaseRepositorio {

    public boolean borrar(Long id, EntityManager em) {
        em.getTransaction().begin();
        Lugar undefined = buscarPorId(0L,em);

        em.createQuery("UPDATE Evento e SET e.lugar = :undefined WHERE e.lugar.id = :idLugar")
                .setParameter("idLugar", id)
                .setParameter("undefined", undefined)
                .executeUpdate();

        em.getTransaction().commit();
        return borrarBase(id, Lugar.class, em);
    }

    public void crearLugarBaseSQL(EntityManager em) {

        if(buscarPorId(0L,em) != null) return;
        String sql = "INSERT INTO lugar (id, nombre, maxcapacidad, activo) VALUES (?, ?, ?, TRUE)";

        em.createNativeQuery(sql)
                .setParameter(1, 0L)
                .setParameter(2, "<undefined>")
                .setParameter(3, 0)
                .executeUpdate();
    }

    public void desactivar(Long id, EntityManager em) {
        Lugar undefined = buscarPorId(0L,em);

        em.createQuery("UPDATE Evento e SET e.lugar = :undefined WHERE e.lugar.id = :idLugar AND e.estado = util.Estado.Pendiente")
                .setParameter("idLugar", id)
                .setParameter("undefined", undefined)
                .executeUpdate();
    }

    public Lugar buscarPorId(Long id, EntityManager em) {
        return buscarPorIdBase(id, Lugar.class, em);
    }

    public Lugar buscarPorNombre(String nombre, EntityManager em) {
        TypedQuery<Lugar> query = em.createQuery(
                "SELECT u FROM Lugar u WHERE u.nombre = :nombre",
                Lugar.class
        );
        query.setParameter("nombre", nombre);

        List<Lugar> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    public List<Lugar> listar(EntityManager em) {
        return listarBase(Lugar.class, em);
    }
}

package data.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import model.Etiqueta;

import java.util.List;

public class EtiquetaRepositorio extends BaseRepositorio {

    public boolean borrar(Long id, EntityManager em) {
        return borrarBase(id, Etiqueta.class, em);
    }

    public Etiqueta buscarPorId(Long id, EntityManager em) {
        return buscarPorIdBase(id, Etiqueta.class, em);
    }

    public List<Etiqueta> listar(EntityManager em) {
        return listarBase(Etiqueta.class,em);
    }

    public void eliminarEtiquetasHuerfanas(EntityManager em) {
        List<Etiqueta> huerfanas = em.createQuery(
                "SELECT e FROM Etiqueta e WHERE e.eventos IS EMPTY",
                Etiqueta.class
        ).getResultList();

        for (Etiqueta e : huerfanas) {
            em.remove(e);
        }
    }

    public Etiqueta obtenerOCrear(String nombre, EntityManager em) {
        List<Etiqueta> existentes = em.createQuery(
                        "SELECT e FROM Etiqueta e " +
                                "WHERE LOWER(e.nombre) = :nombre",
                        Etiqueta.class)
                .setParameter("nombre", nombre)
                .getResultList();

        if (!existentes.isEmpty()) {
            return existentes.getFirst();
        }
        else{
            try {
                Etiqueta nueva = new Etiqueta(nombre.toLowerCase());
                guardar(nueva, em);
                return nueva;
            } catch (PersistenceException ex) {
                return em.createQuery(
                        "SELECT e FROM Etiqueta e WHERE LOWER(e.nombre) = :nombre",
                        Etiqueta.class
                ).setParameter("nombre", nombre).getSingleResult();
            }
        }
    }

}

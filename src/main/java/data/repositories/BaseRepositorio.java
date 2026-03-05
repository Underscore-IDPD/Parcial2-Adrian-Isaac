package data.repositories;

import jakarta.persistence.EntityManager;
import java.util.List;

public abstract class BaseRepositorio {

    public <T> boolean guardar(T objeto, EntityManager em) {
        try {
            assert em != null;
            em.persist(objeto);
            return true;
        }
        catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        }
    }

    public <T> boolean actualizar(T object, EntityManager em) {
        try {
            em.merge(object);
            return true;
        }
        catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        }
    }

    protected <T> boolean borrarBase(Long id, Class<T> clase, EntityManager em) {
        try {
            assert em != null;
            em.getTransaction().begin();

            Object obj = em.find(clase, id);
            if (obj == null) {
                em.getTransaction().rollback();
                return false;
            }

            em.remove(obj);
            em.getTransaction().commit();
            return true;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        }
    }

    protected <T> T buscarPorIdBase(Long id, Class<T> clase, EntityManager em) {
        assert em != null;
        return em.find(clase, id);
    }

    protected <T> List<T> listarBase(Class<T> clase, EntityManager em) {
        assert em != null;
        return em.createQuery(
                        "SELECT c FROM " + clase.getSimpleName() + " c ORDER BY c.id DESC", clase)
                .getResultList();
    }

    protected <T> Long totalElementos(EntityManager em, Class<T> clase) {
        return em.createQuery("SELECT COUNT(*) FROM " + clase.getSimpleName(), Long.class)
                .getSingleResult();
    }
}

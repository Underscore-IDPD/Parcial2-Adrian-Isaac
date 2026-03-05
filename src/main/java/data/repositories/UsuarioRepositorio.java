package data.repositories;

import jakarta.persistence.*;
import model.Usuario;

import java.util.List;

public class UsuarioRepositorio extends BaseRepositorio {

    public boolean borrar(Long id, EntityManager em) {
        em.getTransaction().begin();
        Usuario deleted = buscarPorId(0L,em);

        em.createQuery("UPDATE Evento a SET a.organizador = :deleted WHERE a.organizador.id = :idAutor")
                .setParameter("idAutor", id)
                .setParameter("deleted", deleted)
                .executeUpdate();

        em.getTransaction().commit();
        return borrarBase(id, Usuario.class, em);
    }

    public void crearUsuarioBaseSQL(EntityManager em) {

        if(buscarPorId(0L,em) != null) return;
        String sql = "INSERT INTO usuario (id, username) VALUES (?, ?)";

        em.createNativeQuery(sql)
                .setParameter(1, 0L)
                .setParameter(2, "<deleted>")
                .executeUpdate();
    }

    public Usuario buscarPorId(Long id, EntityManager em) {
        return buscarPorIdBase(id, Usuario.class, em);
    }

    public Usuario buscarPorUsername(String username, EntityManager em) {
        TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.username = :username",
                Usuario.class
        );
        query.setParameter("username", username);

        List<Usuario> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    public List<Usuario> listar(EntityManager em) {
        return listarBase(Usuario.class, em);
    }
}

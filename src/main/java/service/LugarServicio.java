package service;

import data.repositories.LugarRepositorio;
import jakarta.persistence.EntityManager;
import model.Lugar;
import util.Rol;

import java.util.List;

public class LugarServicio {

    private final LugarRepositorio lugarRepositorio;

    public LugarServicio(LugarRepositorio repositorio) {
        lugarRepositorio = repositorio;
    }

    public void crearLugarBase(EntityManager em){
        em.getTransaction().begin();
        lugarRepositorio.crearLugarBaseSQL(em);
        em.getTransaction().commit();
    }

    public Lugar crearLugar(String nombre, int cantMax, String fotoBase64, String tipoImagen, EntityManager em) {

        Lugar l = new Lugar(
                nombre,
                cantMax,
                fotoBase64,
                tipoImagen
        );

        em.getTransaction().begin();
        if(buscarPorNombre(nombre,em) == null) {
            boolean estado = lugarRepositorio.guardar(l, em);
            if (!estado) {
                em.getTransaction().rollback();
                throw new RuntimeException("No se pudo crear lugar");
            }
            else em.getTransaction().commit();
        }
        else{
            em.getTransaction().rollback();
        }
        return l;
    }

    public Lugar buscarPorId(Long id, EntityManager em){
        Lugar u = lugarRepositorio.buscarPorId(id,em);
        if(u==null)
            throw new RuntimeException("Lugar no encontrado");
        return u;
    }

    public List<Lugar> listar (EntityManager em){
        return lugarRepositorio.listar(em);
    }

    public Lugar buscarPorNombre(String nombre, EntityManager em) {
        return lugarRepositorio.buscarPorNombre(nombre,em);
    }

    public void desactivarLugar(long id, EntityManager em){
        Lugar u = lugarRepositorio.buscarPorId(id, em);
        if (u == null) {
            throw new RuntimeException("Lugar no encontrado");
        }
        u.desactivar();
        em.getTransaction().begin();
        lugarRepositorio.desactivar(u.getId(),em);
        lugarRepositorio.actualizar(u,em);
        em.getTransaction().commit();
    }

    public void eliminarLugar(long id, EntityManager em) {

        em.getTransaction().begin();
        try {
            Lugar u = lugarRepositorio.buscarPorId(id, em);
            if (u == null) {
                em.getTransaction().rollback();
                throw new RuntimeException("Lugar no encontrado");
            }

            if (!lugarRepositorio.borrar(id, em)) {
                em.getTransaction().rollback();
                throw new RuntimeException("No se pudo eliminar");
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public boolean modificarLugar(Long id, String nombre, int cantMax, String fotoBase64,
                                 String tipoImagen, EntityManager em) {
        Lugar l = buscarPorId(id,em);

        if(l != null) {
            if(nombre != null && !nombre.trim().isEmpty()) {
                l.setNombre(nombre);
            }

            if(cantMax != l.getMaxCapacidad() && cantMax > 2){
                l.setMaxCapacidad(cantMax);
            }

            if(fotoBase64 != null && !fotoBase64.trim().isEmpty() &&
                    tipoImagen != null && !tipoImagen.trim().isEmpty()) {
                l.setFotoBase64(fotoBase64);
                l.setTipoImagen(tipoImagen);
            }

            em.getTransaction().begin();
            lugarRepositorio.actualizar(l, em);
            em.getTransaction().commit();
            return true;
        }
        return false;
    }

}

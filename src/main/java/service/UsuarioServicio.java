package service;

import data.repositories.UsuarioRepositorio;
import jakarta.persistence.EntityManager;
import model.Usuario;
import org.jasypt.util.text.BasicTextEncryptor;
import util.*;

import java.util.List;

public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final BasicTextEncryptor encryptor;

    public UsuarioServicio(UsuarioRepositorio repositorio) {
        usuarioRepositorio = repositorio;
        encryptor = new BasicTextEncryptor();
        encryptor.setPassword("vaadinflow");
    }

    public void crearUsuarioBase(EntityManager em){
        em.getTransaction().begin();
        usuarioRepositorio.crearUsuarioBaseSQL(em);
        em.getTransaction().commit();
    }

    public Usuario crearUsuario(String username, String password, Rol rol, String fotoBase64, String tipoImagen, EntityManager em) {

        Usuario u = new Usuario(
                username,
                encryptor.encrypt(password),
                rol,
                fotoBase64,
                tipoImagen
        );

        em.getTransaction().begin();
        if(buscarPorUsername(username,em) == null) {
            boolean estado = usuarioRepositorio.guardar(u, em);
            if (!estado) {
                em.getTransaction().rollback();
                throw new RuntimeException("No se pudo crear usuario");
            }
            else em.getTransaction().commit();
        }
        else{
            em.getTransaction().rollback();
        }
        return u;
    }

    public Usuario buscarPorId(Long id, EntityManager em){
        Usuario u = usuarioRepositorio.buscarPorId(id,em);
        if(u==null)
            throw new RuntimeException("Usuario no encontrado");
        return u;
    }

    public Usuario autenticar(String username, String password, EntityManager em) {

        if(username==null || username.equals("<deleted>") || password==null) return null;

        Usuario u = usuarioRepositorio.buscarPorUsername(username,em);
        if(u!=null) {
            if (encryptor.decrypt(u.getPassword()).equals(password)) {
                return u;
            }
        }
        return null;
    }

    public List<Usuario> listar (EntityManager em){
        return usuarioRepositorio.listar(em);
    }

    public Usuario buscarPorUsername(String username, EntityManager em) {
        return usuarioRepositorio.buscarPorUsername(username,em);
    }

    public void eliminarUsuario(long id, EntityManager em) {

        try {
            Usuario u = usuarioRepositorio.buscarPorId(id, em);
            if (u == null) {
                em.getTransaction().rollback();
                throw new RuntimeException("Usuario no encontrado");
            }

            if (!usuarioRepositorio.borrar(id, em)) {
                em.getTransaction().rollback();
                throw new RuntimeException("No se pudo eliminar");
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public void modificarUsuario(Long id, String username, String password, Rol rol, String fotoBase64,
                                 String tipoImagen, EntityManager em) {
        Usuario u = buscarPorId(id,em);

        if(u != null) {
            if(username != null && !username.trim().isEmpty()) {
                u.setUsername(username);
            }

            if(password != null && !password.trim().isEmpty()){
                u.setPassword(encryptor.encrypt(password));
            }

            if(rol != null){
                u.setRol(rol);
            }

            if(fotoBase64 != null && !fotoBase64.trim().isEmpty() &&
                    tipoImagen != null && !tipoImagen.trim().isEmpty()) {
                u.setFotoBase64(fotoBase64);
                u.setTipoImagen(tipoImagen);
            }

            em.getTransaction().begin();
            usuarioRepositorio.actualizar(u, em);
            em.getTransaction().commit();
        }
    }

    public String encriptarCookie(String texto) {
        return this.encryptor.encrypt(texto);
    }

    public String desencriptarCookie(String textoEncriptado) {
        try {
            return this.encryptor.decrypt(textoEncriptado);
        } catch (Exception e) {
            return null;
        }
    }

}

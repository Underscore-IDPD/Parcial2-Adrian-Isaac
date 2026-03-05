package service;

import jakarta.persistence.EntityManager;
import model.*;
import io.javalin.http.Context;
import data.repositories.UsuarioRepositorio;
import util.Rol;

public class AuthServicio {

    private final UsuarioRepositorio usuarioRepo;

    public AuthServicio(UsuarioRepositorio usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    public boolean estaLoggeado(Context ctx) {
        return ctx.sessionAttribute("usuarioId") != null;
    }

    public Usuario obtenerUsuarioLoggeado(Context ctx) {
        Long uid = ctx.sessionAttribute("usuarioId");
        EntityManager em = ctx.attribute("em");
        if (uid == null) return null;
        return usuarioRepo.buscarPorId(uid,em);
    }

    public boolean esAdmin(Context ctx) {
        Usuario u = obtenerUsuarioLoggeado(ctx);
        return u != null && u.getRol() == Rol.Administrador;
    }

    public boolean esAutor(Context ctx) {
        Usuario u = obtenerUsuarioLoggeado(ctx);
        return u != null && u.getRol() == Rol.Organizador;
    }

    public boolean esCreador(Context ctx, Evento evento) {
        Usuario u = obtenerUsuarioLoggeado(ctx);
        return u != null && evento.getOrganizador().getId().equals(u.getId());
    }

    public boolean esCreador(Context ctx, Comentario comentario) {
        Usuario u = obtenerUsuarioLoggeado(ctx);
        return u != null && comentario.getAutor().getId().equals(u.getId());
    }

    public boolean permitirBorrado(Context ctx, Comentario comentario) {
        return esCreador(ctx, comentario) || esAdmin(ctx);
    }

    public boolean permitirBorrado(Context ctx, Evento evento) {
        return esCreador(ctx, evento) || esAdmin(ctx);
    }
}
package controller;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;
import model.*;
import service.*;

public class ComentarioControlador {

    private final ComentarioServicio comentarioServicio;
    private final EventoServicio eventoServicio;
    private final AuthServicio authServicio;
    private final UsuarioServicio usuarioServicio;

    public ComentarioControlador(ComentarioServicio comentarioServicio, EventoServicio eventoServicio, AuthServicio authServicio, UsuarioServicio usuarioServicio) {
        this.comentarioServicio = comentarioServicio;
        this.authServicio = authServicio;
        this.usuarioServicio = usuarioServicio;
        this.eventoServicio = eventoServicio;
    }

    public void registrarRutas(JavalinConfig app) {

        app.routes.post("/eventos/{id}/comentarios", this::crearComentario);
        app.routes.post("/comentarios/{id}/delete", this::eliminarComentario);
        app.routes.post("/comentarios/{id}/edit", this::modificarComentario);
        app.routes.post("/comentarios/{id}/modoEdit", this::modoEdit);

    }

    private void crearComentario(Context ctx) {

        if (!authServicio.estaLoggeado(ctx)) {
            ctx.status(401);
            return;
        }

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuario = usuarioServicio.buscarPorId(uid,em);

        long eventoId = Long.parseLong(ctx.pathParam("id"));
        Evento a = eventoServicio.buscarPorId(eventoId, em);

        String texto = ctx.formParam("texto");

        assert usuario != null;
        comentarioServicio.agregarComentario(texto, usuario, a, em);

        ctx.redirect("/eventos/" + eventoId);

    }

    public void eliminarComentario(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Comentario c = comentarioServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(c == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.permitirBorrado(ctx,c)){
            ctx.status(403);
            return;
        }

        comentarioServicio.eliminarComentario(c.getId(), em);

        ctx.redirect("/eventos/" + c.getEventoId());
    }

    public void modificarComentario(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Comentario c = comentarioServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(c == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.permitirBorrado(ctx,c)){
            ctx.status(403);
            return;
        }

        comentarioServicio.modificarComentario(c.getId(),ctx.formParam("texto"), em);

        ctx.redirect("/eventos/" + c.getEventoId());
    }

    public void modoEdit(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        if (uid == null) {
            ctx.status(401);
            return;
        }

        Comentario c = comentarioServicio
                .buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if (c == null) {
            ctx.status(404);
            return;
        }

        if (!authServicio.permitirBorrado(ctx, c)) {
            ctx.status(403);
            return;
        }

        ctx.sessionAttribute("modoEdit", c.getId());
        ctx.redirect("/eventos/" + c.getEventoId());
    }



}

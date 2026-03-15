package controller;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.persistence.EntityManager;
import model.*;
import service.AuthServicio;
import service.EventoServicio;
import service.LugarServicio;
import service.UsuarioServicio;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class LugarControlador {

    private final LugarServicio lugarServicio;
    private final AuthServicio authServicio;
    private final UsuarioServicio usuarioServicio;
    private final EventoServicio eventoServicio;

    public LugarControlador(LugarServicio lugarServicio, AuthServicio authServicio, UsuarioServicio usuarioServicio, EventoServicio eventoServicio) {
        this.lugarServicio = lugarServicio;
        this.authServicio = authServicio;
        this.usuarioServicio = usuarioServicio;
        this.eventoServicio = eventoServicio;
    }

    public void registrarRutas(JavalinConfig app) {

        app.routes.get("/lugares", this::listarLugares);

        app.routes.get("/lugares/crear", this::crearLugarVisual);

        app.routes.get("/lugares/{id}/edit", this::modificarLugarVisual);

        app.routes.post("/lugares/{id}/edit", this::modificarLugar);

        app.routes.post("/lugares/{id}/delete", this::eliminarLugar);

        app.routes.post("/lugares", this::crearLugar);

        app.routes.post("/lugares/{id}/deactivate", this::desactivarLugar);

        app.routes.post("/lugares/{id}/activate", this::activarLugar);

    }

    private void listarLugares(Context ctx){
        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuario = usuarioServicio.buscarPorId(uid, em);

        List<Lugar> lugares = lugarServicio.listar(em);
        lugares.remove(lugarServicio.buscarPorId(0L, em));

        List<Evento> eventosTranscurso = eventoServicio.listarTodos("En_Transcurso", em);

        Set<Long> lugaresConEventos = new HashSet<>();

        for(Lugar l: lugares) {
            for (Evento e : eventosTranscurso) {
                if (e.getLugar().getId().equals(l.getId())) lugaresConEventos.add(l.getId());
            }
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuario", usuario);
        modelo.put("lugares", lugares);


        modelo.put("lugaresConEventos", lugaresConEventos);

        ctx.render("templates/lista-lugares.html", modelo);
    }

    private void crearLugarVisual(Context ctx) {

        if(!authServicio.esAutor(ctx) && !authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        EntityManager em = ctx.attribute("em");

        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario u = usuarioServicio.buscarPorId(uid, em);

        String errorParam = ctx.queryParam("error");
        int error = 0;

        if (errorParam != null) {
            try {
                error = Integer.parseInt(errorParam);
            } catch (NumberFormatException ignored) {}
        }

        String errormsg = switch (error) {
            case 1 -> "Archivo debe ser una imagen válida";
            default -> "";
        };

        HashMap<String,Object> modelo = new HashMap<>();

        modelo.put("usuarioActual", u);
        modelo.put("errormsg", errormsg);
        modelo.put("lugar", null);

        modelo.put("lugares",
                lugarServicio.listar(em));

        ctx.render("templates/form-lugar.html", modelo);
    }


    private void modificarLugarVisual(Context ctx) {

        EntityManager em = ctx.attribute("em");
        long id = Long.parseLong(ctx.pathParam("id"));

        Lugar l = lugarServicio.buscarPorId(id, em);

        if (l == null) {
            ctx.status(404);
            return;
        }

        if (!authServicio.esAdmin(ctx)) {
            ctx.status(403);
            return;
        }

        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario u = usuarioServicio.buscarPorId(uid, em);

        Map<String,Object> modelo = new HashMap<>();

        modelo.put("modo","editar");
        modelo.put("lugar", l);
        modelo.put("usuarioActual", u);
        modelo.put("lugares", lugarServicio.listar(em));

        ctx.render("templates/form-lugar.html", modelo);
    }

    private void crearLugar(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuario = usuarioServicio.buscarPorId(uid,em);

        if(!authServicio.esAutor(ctx) && !authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        UploadedFile archivo = ctx.uploadedFile("foto");

        String fotoBase64 = null;
        String tipoImagen = null;

        if (archivo != null && archivo.size() > 0) {

            if (!archivo.contentType().startsWith("image/")) {
                ctx.redirect("/lugares/crear?error=1");
                return;
            }

            try {
                byte[] bytes = archivo.content().readAllBytes();
                fotoBase64 = Base64.getEncoder().encodeToString(bytes);
                tipoImagen = archivo.contentType();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String nombre = ctx.formParam("nombre");
        String capacidadString = ctx.formParam("capacidad");

        int capacidad = 0;

        if(capacidadString != null){
            capacidad = Integer.parseInt(capacidadString);
        }

        if(lugarServicio.crearLugar(nombre, capacidad, fotoBase64, tipoImagen, em) != null)
            ctx.redirect("/");
        else
            ctx.redirect("/lugares?error=1");
    }

    private void desactivarLugar(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Lugar l = lugarServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(l == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        List <Evento> eventosTranscurso = eventoServicio.listarTodos("En_Transcurso",em);
        if(eventosTranscurso.stream().anyMatch(ev -> (ev.getLugar().getId().equals(Long.parseLong(ctx.pathParam("id")))))){
            ctx.status(400);
            return;
        }

        lugarServicio.desactivarLugar(l.getId(), em);

        ctx.redirect("/");
    }

    private void activarLugar(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Lugar l = lugarServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(l == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        lugarServicio.activarLugar(l.getId(), em);

        ctx.redirect("/lugares");
    }

    private void modificarLugar(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        String luid = ctx.pathParam("id");

        if(!authServicio.esAutor(ctx) && !authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        UploadedFile archivo = ctx.uploadedFile("foto");

        String fotoBase64 = null;
        String tipoImagen = null;

        if (archivo != null && archivo.size() > 0) {

            if (!archivo.contentType().startsWith("image/")) {
                ctx.redirect("/lugares/crear?error=1");
                return;
            }

            try {
                byte[] bytes = archivo.content().readAllBytes();
                fotoBase64 = Base64.getEncoder().encodeToString(bytes);
                tipoImagen = archivo.contentType();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String nombre = ctx.formParam("nombre");
        String capacidadString = ctx.formParam("capacidad");

        int capacidad = 0;

        if(capacidadString != null){
            capacidad = Integer.parseInt(capacidadString);
        }

        if(lugarServicio.modificarLugar(Long.parseLong(luid),nombre,capacidad,fotoBase64,tipoImagen,em))
            ctx.redirect("/");
        else
            ctx.redirect("/lugares?error=1");
    }

    private void eliminarLugar(Context ctx) {

        EntityManager em = ctx.attribute("em");

        if(!authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        long id = Long.parseLong(ctx.pathParam("id"));

        List <Evento> eventosTranscurso = eventoServicio.listarTodos("En_Transcurso",em);
        if(eventosTranscurso.stream().anyMatch(ev -> (ev.getLugar().getId().equals(id)))){
            ctx.status(400);
            return;
        }

        try {
            lugarServicio.eliminarLugar(id, em);
            ctx.redirect("/lugares");
        } catch (RuntimeException e) {
            e.printStackTrace();
            ctx.status(404);
        }
    }

}

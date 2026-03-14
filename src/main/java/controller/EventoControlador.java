package controller;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.persistence.EntityManager;
import model.*;
import org.jetbrains.annotations.NotNull;
import service.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class EventoControlador {

    private final EventoServicio eventoServicio;
    private final AuthServicio authServicio;
    private final UsuarioServicio usuarioServicio;
    private final LugarServicio lugarServicio;

    public EventoControlador(EventoServicio eventoServicio, AuthServicio authServicio, UsuarioServicio usuarioServicio, LugarServicio lugarServicio) {
        this.eventoServicio = eventoServicio;
        this.authServicio = authServicio;
        this.usuarioServicio = usuarioServicio;
        this.lugarServicio = lugarServicio;
    }

    public void registrarRutas(JavalinConfig app) {

        
        app.routes.get("/", this::index);

        app.routes.get("/eventos/{id}", this::verEvento);

        app.routes.get("/eventos", this::crearEventoVisual);

        app.routes.post("/eventos", this::crearEvento);

        app.routes.post("/eventos/{id}/delete", this::eliminarEvento);

        app.routes.get("/eventos/{id}/edit", this::modificarEventoVisual);

        app.routes.post("/eventos/{id}/cancel", this::cancelarEvento);

        app.routes.post("/eventos/{id}", this::modificarEvento);

        app.routes.get("/etiqueta/{nombre}", this::filtrarPorEtiqueta);


    }

    private void index(Context ctx) {
        EntityManager em = ctx.attribute("em");
        int pagina = 0;
        String paginaParam = ctx.queryParam("pagina");
        if (paginaParam != null) {
            try {
                pagina = Integer.parseInt(paginaParam);
            } catch (NumberFormatException _) {
            }
        }

        List<Evento> eventos = eventoServicio.listarEventos(pagina, em);

        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario actual = null;

        if (uid != null) {
            actual = usuarioServicio.buscarPorId(uid, em);
        }

        List<Usuario> usuarioList = usuarioServicio.listar(em);

        long totalEventos = eventoServicio.totalEventos(em);
        int totalPaginas = (int) Math.ceil((double) totalEventos / 5);

        int maxBotones = 5;
        int inicioRango = Math.max(0, pagina - maxBotones / 2);
        int finRango = Math.min(totalPaginas - 1, inicioRango + maxBotones - 1);


        if (finRango - inicioRango + 1 < maxBotones) {
            inicioRango = Math.max(0, finRango - maxBotones + 1);
        }

        Map<Long, Usuario> mapaUsuarios = new HashMap<>();
        for(Usuario u : usuarioList){
            mapaUsuarios.put(u.getId(), u);
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("eventos", eventos);
        modelo.put("usuario",actual);
        modelo.put("usuarios",mapaUsuarios);
        modelo.put("etiquetas", eventoServicio.listarEtiquetas(em));
        modelo.put("paginaActual", pagina);
        modelo.put("totalPaginas", totalPaginas);
        modelo.put("inicioRango", inicioRango);
        modelo.put("finRango", finRango);

        ctx.render("templates/index.html",modelo);
    }

    private void crearEventoVisual(Context ctx) {

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
        modelo.put("error", error);
        modelo.put("errormsg", errormsg);
        modelo.put("evento", null);

        modelo.put("lugares",
                lugarServicio.listar(em));

        ctx.render("templates/form-evento.html", modelo);
    }


    private void modificarEventoVisual(Context ctx) {

        EntityManager em = ctx.attribute("em");
        long id = Long.parseLong(ctx.pathParam("id"));

        Evento e = eventoServicio.buscarPorId(id, em);

        if (e == null) {
            ctx.status(404);
            return;
        }

        if (!authServicio.permitirBorrado(ctx, e)) {
            ctx.status(403);
            return;
        }

        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario u = usuarioServicio.buscarPorId(uid, em);

        String etiquetasTexto = "";

        if (e.getEtiquetas() != null) {
            etiquetasTexto = e.getEtiquetas()
                    .stream()
                    .map(Etiqueta::getNombre)
                    .collect(Collectors.joining(","));
        }

        Map<String,Object> modelo = new HashMap<>();

        modelo.put("modo","editar");
        modelo.put("evento", e);
        modelo.put("usuarioActual", u);
        modelo.put("etiquetasTexto", etiquetasTexto);
        modelo.put("lugares", lugarServicio.listar(em));

        ctx.render("templates/form-evento.html", modelo);
    }


    private void filtrarPorEtiqueta(Context ctx) {
        
        EntityManager em = ctx.attribute("em");
        String nombre = ctx.pathParam("nombre");

        List<Evento> filtrados = eventoServicio.buscarPorEtiqueta(nombre,em);

        List<Usuario> usuarioList = usuarioServicio.listar(em);
        Map<Long, Usuario> mapaUsuarios = new HashMap<>();
        for(Usuario u : usuarioList){
            mapaUsuarios.put(u.getId(), u);
        }
        ctx.attribute("eventos", filtrados);

        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario actual = null;

        if (uid != null) {
            actual = usuarioServicio.buscarPorId(uid, em);
        }

        ctx.attribute("usuario", actual);
        ctx.attribute("usuarios", mapaUsuarios);
        ctx.attribute("etiquetas", eventoServicio.listarEtiquetas(em));

        ctx.render("templates/index.html");
    }


    private void verEvento(Context ctx) {

        EntityManager em = ctx.attribute("em");

        long id = Long.parseLong(ctx.pathParam("id"));
        Evento e = eventoServicio.buscarPorId(id, em);

        if (e == null) {
            ctx.status(404);
            return;
        }

        Map<String, Object> modelo = new HashMap<>();

        Long uid = ctx.sessionAttribute("usuarioId");

        Usuario usuarioSesion = null;

        if (uid != null) {
            usuarioSesion = usuarioServicio.buscarPorId(uid, em);
        }

        Usuario organizador = e.getOrganizador();

        List<Comentario> comentarios =
                eventoServicio.listarComentarios(id, em);

        List<Usuario> usuarios =
                usuarioServicio.listar(em);

        Map<Long, Usuario> mapaUsuarios = new HashMap<>();

        for (Usuario u : usuarios) {
            mapaUsuarios.put(u.getId(), u);
        }

        int inscritos = e.getInscripciones().size();
        int cuposDisponibles = e.getCupoMaximo() - inscritos;

        modelo.put("evento", e);
        modelo.put("organizador", organizador);
        modelo.put("comentarios", comentarios);
        modelo.put("usuarios", mapaUsuarios);
        modelo.put("usuario", usuarioSesion);

        modelo.put("inscritos", inscritos);
        //modelo.put("asistentes", asistentes);
        //float porcentajeAsistencia = (float) (asistentes / inscritos) * 100;
        //modelo.put("porcentajeAsistencia",porcentajeAsistencia);
        modelo.put("cuposDisponibles", cuposDisponibles);

        modelo.put("puedeEditar",
                authServicio.esCreador(ctx, e));

        modelo.put("puedeCancelar",
                authServicio.permitirBorrado(ctx, e));

        modelo.put("puedeEliminar",
                authServicio.permitirBorrado(ctx, e));

        ctx.render("templates/evento.html", modelo);
    }



    private void crearEvento(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuario = usuarioServicio.buscarPorId(uid,em);

        if(!authServicio.esAutor(ctx) && !authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        UploadedFile archivo = ctx.uploadedFile("foto");

        String bannerBase64 = null;
        String tipoImagen = null;

        if (archivo != null && archivo.size() > 0) {

            if (!archivo.contentType().startsWith("image/")) {
                ctx.redirect("/usuarios/crear?error=1");
                return;
            }

            try {
                byte[] bytes = archivo.content().readAllBytes();
                bannerBase64 = Base64.getEncoder().encodeToString(bytes);
                tipoImagen = archivo.contentType();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String concepto = ctx.formParam("concepto");

        String descripcion = ctx.formParam("descripcion");

        String etiquetas = ctx.formParam("etiqueta");

        String fechaString = ctx.formParam("fecha");
        LocalDate fechaEvento = LocalDate.parse(fechaString);
        String horaString = ctx.formParam("hora");
        LocalTime horaEvento = LocalTime.parse(horaString);
        LocalDateTime fechaHoraEvento = LocalDateTime.of(fechaEvento, horaEvento);

        String lugarString = ctx.formParam("lugar");

        Long lugarId = 0L;

        if(lugarString != null){
            lugarId = Long.parseLong(lugarString);
        }

        Lugar lugar = lugarServicio.buscarPorId(lugarId,em);

        String duracionString = ctx.formParam("duracion");

        int duracionMinutos = Integer.parseInt(duracionString);
        int cupoMaximo = Integer.parseInt(ctx.formParam("cupoMaximo"));

        if(eventoServicio.crearEvento(concepto, descripcion, cupoMaximo, duracionMinutos, usuario, lugar, fechaHoraEvento, etiquetas, bannerBase64, tipoImagen, em) != null)
            ctx.redirect("/");
        else
            ctx.redirect("/eventos?error=1");
    }

    private void eliminarEvento(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Evento a = eventoServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(a == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.permitirBorrado(ctx,a)){
            ctx.status(403);
            return;
        }

        long id = Long.parseLong(ctx.pathParam("id"));

        eventoServicio.eliminarEvento(id, em);

        ctx.redirect("/");
    }

    private void cancelarEvento(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Evento a = eventoServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(a == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.permitirBorrado(ctx,a)){
            ctx.status(403);
            return;
        }

        long id = Long.parseLong(ctx.pathParam("id"));

        String razon = ctx.formParam("razon");

        eventoServicio.cancelarEvento(id, razon,  em);

        ctx.redirect("/");
    }

    public void modificarEvento(Context ctx) {

        EntityManager em = ctx.attribute("em");
        Evento e = eventoServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(e == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.permitirBorrado(ctx,e)){
            ctx.status(403);
            return;
        }

        String fechaString = ctx.formParam("fecha");
        LocalDate fechaEvento = LocalDate.parse(fechaString);
        String horaString = ctx.formParam("hora");
        LocalTime horaEvento = LocalTime.parse(horaString);
        LocalDateTime fechaHoraEvento = LocalDateTime.of(fechaEvento, horaEvento);

        String lugarString = ctx.formParam("lugar");

        Long lugarId = 0L;

        if(lugarString != null){
            lugarId = Long.parseLong(lugarString);
        }

        Lugar lugar = lugarServicio.buscarPorId(lugarId,em);

        if(eventoServicio.modificarEvento(e.getId(),
                ctx.formParam("concepto"),
                ctx.formParam("descripcion"),
                Integer.parseInt(ctx.formParam("cupoMaximo")),
                Integer.parseInt(ctx.formParam("duracion")),
                fechaHoraEvento,
                ctx.formParam("etiqueta"),
                lugar, em)
        != null)
            ctx.redirect("/eventos/" + e.getId());
        else
            ctx.redirect("/eventos?error=3");
    }

}

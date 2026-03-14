package com.pucmm.csti18104833.parcial2;

import controller.EventoControlador;
import controller.ComentarioControlador;
import controller.LugarControlador;
import controller.UsuarioControlador;
import data.repositories.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import jakarta.servlet.SessionTrackingMode;
import jakarta.persistence.EntityManager;
import model.Evento;
import model.Comentario;
import model.Inscripcion;
import model.Usuario;
import service.*;
import util.Javanator;
import util.Rol;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    static void main() throws SQLException {

        EventoRepositorio eventoRepositorio = new EventoRepositorio();
        UsuarioRepositorio usuarioRepositorio = new UsuarioRepositorio();
        ComentarioRepositorio comentarioRepositorio = new ComentarioRepositorio();
        EtiquetaRepositorio etiquetaRepositorio = new EtiquetaRepositorio();
        LugarRepositorio lugarRepositorio = new LugarRepositorio();

        EventoServicio eventoServicio = new EventoServicio(eventoRepositorio,etiquetaRepositorio);
        ComentarioServicio comentarioServicio = new ComentarioServicio(comentarioRepositorio);
        UsuarioServicio usuarioServicio = new UsuarioServicio(usuarioRepositorio);
        AuthServicio authServicio = new AuthServicio(usuarioRepositorio);
        LugarServicio lugarServicio = new LugarServicio(lugarRepositorio);

        EventoControlador EventoControlador = new EventoControlador(
                eventoServicio,authServicio,usuarioServicio,lugarServicio
        );
        UsuarioControlador usuarioControlador = new UsuarioControlador(
                authServicio,usuarioServicio
        );
        ComentarioControlador comentarioControlador = new ComentarioControlador(
                comentarioServicio, eventoServicio,authServicio,usuarioServicio
        );
        LugarControlador lugarControlador = new LugarControlador(lugarServicio,authServicio,usuarioServicio);

        var app = Javalin.create(config ->{
            config.jetty.modifyServletContextHandler(handler -> {
                handler.getSessionHandler().setSessionTrackingModes(
                        java.util.Set.of(SessionTrackingMode.COOKIE)
                );
            });

            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "public";
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.aliasCheck = null;
            });

            config.routes.before(ctx -> {
                ctx.attribute("em", Javanator.getEntityManager());
            });

            config.routes.after(ctx -> {
                EntityManager em = ctx.attribute("em");
                if (em != null && em.isOpen()) {
                    em.close();
                }
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                EntityManager em = ctx.attribute("em");
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace(); // ver el error real en consola
                ctx.status(500).result("Error: " + e.getMessage()); // ver en navegador
            });

            config.routes.before("/*", ctx -> {
                if (ctx.sessionAttribute("usuarioId") == null) {

                    String cookieRecordar = ctx.cookie("recordar-usuario");

                    if (cookieRecordar != null) {
                        try {
                            String id = usuarioServicio.desencriptarCookie(cookieRecordar);
                            Long uid = Long.parseLong(id);

                            EntityManager em = ctx.attribute("em");
                            Usuario u = usuarioServicio.buscarPorId(uid, em);

                            if (u != null) {
                                ctx.sessionAttribute("usuarioId", u.getId());
                            }
                        } catch (Exception e) {
                            ctx.removeCookie("recordar-usuario");
                        }
                    }
                }
            });

            config.routes.before("/login", ctx -> {
                if(authServicio.estaLoggeado(ctx)) ctx.redirect("/");
            });

            config.routes.before("/usuarios", ctx ->{
                if(!authServicio.esAdmin(ctx)) ctx.redirect("/");
            });

            config.routes.before("/usuarios/*", ctx -> {
                if (!authServicio.esAdmin(ctx)) ctx.redirect("/");
            });

            config.routes.before("/eventos", ctx -> {
                if(!authServicio.esAdmin(ctx) && !authServicio.esAutor(ctx)){
                    ctx.redirect("/");
                }
            });

            config.routes.before("/eventos/*", ctx -> {
                if(ctx.path().matches("/eventos/\\d+") || ctx.path().contains("/etiqueta/")) {
                    return;
                }

                if(!authServicio.estaLoggeado(ctx)) {
                    ctx.redirect("/login");
                    return;
                }

                if(ctx.path().endsWith("/comentarios") || ctx.path().contains("/inscribirse") || ctx.path().contains("/desinscribirse")) return;

                if(!authServicio.esAdmin(ctx) && !authServicio.esAutor(ctx)) {
                    ctx.redirect("/");
                }


            });

            config.routes.before("/eventos/{id}/*", ctx ->{
                if (authServicio.esAdmin(ctx) || ctx.path().endsWith("/comentarios") || ctx.path().contains("/etiqueta")) return;

                long idEvento = Long.parseLong(ctx.pathParam("id"));
                EntityManager em = ctx.attribute("em");
                Evento e = eventoServicio.buscarPorId(idEvento,em);
                Long idUsuario = ctx.sessionAttribute("usuarioId");

                if (e != null && !e.getOrganizador().getId().equals(idUsuario) && !ctx.path().contains("/inscribirse") && !ctx.path().contains("/desinscribirse")) {
                    ctx.redirect("/");
                }
            });


            config.routes.before("/comentarios/{id}/*", ctx -> {
                if (!authServicio.estaLoggeado(ctx)) {
                    ctx.redirect("/login");
                    return;
                }

                if (authServicio.esAdmin(ctx)) return;

                long idComentario = Long.parseLong(ctx.pathParam("id"));
                EntityManager em = ctx.attribute("em");
                Comentario com = comentarioServicio.buscarPorId(idComentario, em);

                Long idUsuario = ctx.sessionAttribute("usuarioId");

                if (com != null && !com.getAutor().getId().equals(idUsuario)) {
                    ctx.redirect("/");
                }
            });

            EventoControlador.registrarRutas(config);
            usuarioControlador.registrarRutas(config);
            comentarioControlador.registrarRutas(config);
            lugarControlador.registrarRutas(config);

            config.fileRenderer(new JavalinThymeleaf());
        });

        app.start(7070);
        probarConexion();

        EntityManager em = Javanator.getEntityManager();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                eventoServicio.sincronizarEstados(em);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);

        usuarioServicio.crearUsuarioBase(em);
        lugarServicio.crearLugarBase(em);
        if(usuarioRepositorio.listar(em).size() == 1){
            crearDatosDePrueba(usuarioServicio, eventoServicio, lugarServicio, em);
        }
        em.close();


    }

    private static void crearDatosDePrueba(
            UsuarioServicio us,
            EventoServicio es,
            LugarServicio ls,
            EntityManager em) {

        var admin = us.crearUsuario(
                "admin",
                "1234",
                Rol.Administrador,
                "","",
                em
        );

        String fotoBase64 = imagenBase64DesdeResources("campus-PUCMM.jpg");
        String tipoImagen = "image/jpg";

        ls.crearLugar(
          "Lab Ingeniería Mecánica e Industrial",
                60,
                fotoBase64,
                tipoImagen,
                em
        );

        fotoBase64 = imagenBase64DesdeResources("default-banner.png");
        tipoImagen = "image/jpg";

        es.crearEvento(
                "Charla sobre Vaadin",
                "Vaadin es lo mejor",
                60,
                60,
                admin,
                ls.buscarPorId(1L,em),
                LocalDateTime.of(2026,12,31,8,30,0),
                "vaadin, java",
                fotoBase64, tipoImagen,
                em
        );
        Evento evento = em.find(Evento.class, 1L);

        Random random = new Random();

        try {
            for (int ind = 1; ind <= 30; ind++) {
                Usuario estudiante = us.crearUsuario(
                        "estudiante" + ind, "1234", Rol.Participante, "", "", em);

                Inscripcion inscripcion = new Inscripcion(evento, estudiante);

                int diasAntes = random.nextInt(15) + 1;
                inscripcion.setFechaInscripcion(evento.getFechaEvento().minusDays(diasAntes));

                if (random.nextInt(100) < 80) {
                    inscripcion.marcarAsistio();

                    int minutosVariacion = random.nextInt(120) - 60;
                    inscripcion.setFechaAsistencia(evento.getFechaEvento().plusMinutes(minutosVariacion));
                }

                em.persist(inscripcion);
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        }
    }

    public static void probarConexion() {
        try (Connection conn =
                     DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"))) {

            System.out.println("Conexión exitosa a Cockroach!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String imagenBase64DesdeResources(String nombre) {
        try (InputStream is = Main.class.getResourceAsStream("/public/img/" + nombre)) {

            if (is == null) throw new RuntimeException("Imagen no encontrada");

            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

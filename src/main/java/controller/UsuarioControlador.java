package controller;

import io.javalin.config.JavalinConfig;
import io.javalin.http.UploadedFile;
import jakarta.persistence.EntityManager;
import model.Usuario;
import service.UsuarioServicio;
import service.AuthServicio;
import util.Rol;

import io.javalin.http.Context;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuarioControlador {

    private final AuthServicio authServicio;
    private final UsuarioServicio usuarioServicio;

    public UsuarioControlador(AuthServicio authServicio, UsuarioServicio usuarioServicio) {
        this.authServicio = authServicio;
        this.usuarioServicio = usuarioServicio;
    }

    public void registrarRutas(JavalinConfig app) {

        app.routes.get("/login", this::loginVisual);

        app.routes.post("/login", this::login);

        app.routes.get("/logout", this::logout);

        app.routes.get("/usuarios", this::listarUsuarios);

        app.routes.get("/usuarios/crear", this::crearUsuarioVisual);

        app.routes.post("/usuarios", this::crearUsuario);

        app.routes.get("/usuarios/{id}/edit", this::modificarUsuarioVisual);

        app.routes.post("/usuarios/{id}/edit", this::modificarUsuario);

        app.routes.post("/usuarios/{id}/delete", this::eliminarUsuario);
    }

    private void loginVisual(Context ctx) {
        ctx.attribute("error", null);
        ctx.render("templates/login.html");
    }

    private void login(Context ctx) {

        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        EntityManager em = ctx.attribute("em");
        Usuario u = usuarioServicio.autenticar(username, password, em);

        if (u != null) {
            ctx.sessionAttribute("usuarioId", u.getId());

            String recordar = ctx.formParam("recordar");
            if (recordar != null && recordar.equals("on")) {

                String cookieEncriptada = usuarioServicio.encriptarCookie(String.valueOf(u.getId()));

                ctx.cookie("recordar-usuario", cookieEncriptada, 604800);
            }
            ctx.redirect("/");

        } else {
            System.out.println("Login Incorrecto");
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("error",true);
            ctx.render("templates/login.html",modelo);
        }
    }

    private void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.removeCookie("recordar-usuario");
        ctx.redirect("/");
    }

    private void crearUsuarioVisual(Context ctx){
        Map<String, Object> modelo = new HashMap<>();

        modelo.put("roles",Rol.values());
        modelo.put("modo","crear");

        String error = ctx.sessionAttribute("error");
        modelo.put("error", error);

        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuarioActual = usuarioServicio.buscarPorId(uid,em);

        modelo.put("usuarioActual",usuarioActual);

        ctx.render("templates/form-usuario.html",modelo);

    }

    private void crearUsuario(Context ctx) {

        if (!authServicio.esAdmin(ctx)) {
            ctx.status(403).result("No autorizado");
            return;
        }
        EntityManager em = ctx.attribute("em");

        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String rol = ctx.formParam("rol");

        UploadedFile archivo = ctx.uploadedFile("foto");

        String fotoBase64 = null;
        String tipoImagen = null;

        if (archivo != null && archivo.size() > 0) {

            if (!archivo.contentType().startsWith("image/")) {
                ctx.redirect("/usuarios/crear?error=1");
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

        if(usuarioServicio.buscarPorUsername(username,em) == null) {
            assert em != null;
            if (usuarioServicio.crearUsuario(username, password, Rol.valueOf(rol), fotoBase64, tipoImagen, em) == null) {
                ctx.sessionAttribute("error", "El usuario no se pudo crear");
                ctx.redirect("/usuarios");
            }
            else ctx.redirect("/");
        }
        else {
            ctx.sessionAttribute("error", "El usuario no se pudo crear");
            ctx.redirect("/usuarios");
        }


    }

    private void modificarUsuario(Context ctx){
        EntityManager em = ctx.attribute("em");
        Usuario u = usuarioServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);

        if(u == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        String rolNuevo = ctx.formParam("rol");
        Rol rolFinal = u.getRol();

        Usuario usuarioActual = authServicio.obtenerUsuarioLoggeado(ctx);

        if (usuarioActual != null && !usuarioActual.getId().equals(u.getId())) {
            if (rolNuevo != null && !rolNuevo.isEmpty()) {
                rolFinal = Rol.valueOf(rolNuevo);
            }
        }

        UploadedFile archivo = ctx.uploadedFile("foto");
        String fotoBase64 = null;
        String tipoImagen = null;

        if (archivo != null && archivo.size() > 0) {

            if (!archivo.contentType().startsWith("image/")) {
                ctx.redirect("/usuarios/crear?error=1");
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

        usuarioServicio.modificarUsuario(u.getId(),
                ctx.formParam("username"),
                ctx.formParam("password"),
                rolFinal,
                fotoBase64,
                tipoImagen, em);



        ctx.redirect("/usuarios");
    }

    private void modificarUsuarioVisual(Context ctx){
        EntityManager em = ctx.attribute("em");
        Usuario u = usuarioServicio.buscarPorId(Long.parseLong(ctx.pathParam("id")), em);
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuarioActual = usuarioServicio.buscarPorId(uid,em);

        if(u == null){
            ctx.status(404);
            return;
        }

        if(!authServicio.esAdmin(ctx)){
            ctx.status(403);
            return;
        }

        ctx.attribute("modo", "editar");
        ctx.attribute("usuario", u);
        ctx.attribute("usuarioActual", usuarioActual);
        ctx.attribute("roles", Rol.values());

        ctx.render("templates/form-usuario.html");
    }

    private void eliminarUsuario(Context ctx) {
        EntityManager em = ctx.attribute("em");

        if (!authServicio.esAdmin(ctx)) {
            ctx.status(403);
            return;
        }

        long id = Long.parseLong(ctx.pathParam("id"));

        try {
            usuarioServicio.eliminarUsuario(id, em);
            ctx.redirect("/usuarios");
        } catch (RuntimeException e) {
            e.printStackTrace();
            ctx.status(404);
        }
    }

    private void listarUsuarios(Context ctx){
        EntityManager em = ctx.attribute("em");
        Long uid = ctx.sessionAttribute("usuarioId");
        Usuario usuario = usuarioServicio.buscarPorId(uid,em);

        List<Usuario> usuarios= usuarioServicio.listar(em);
        usuarios.remove(usuarioServicio.buscarPorId(0L,em));

        ctx.attribute("usuario",usuario);
        ctx.attribute("usuarios", usuarios);
        ctx.render("templates/lista-usuarios.html");
    }


}

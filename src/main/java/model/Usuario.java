package model;

import jakarta.persistence.*;
import util.Rol;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fotoBase64;
    private String tipoImagen;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    @OneToMany
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public Usuario(String username, String password, Rol rol, String fotoBase64, String tipoImagen) {
        this.username = username;
        this.password = password;
        this.fotoBase64 = fotoBase64;
        this.tipoImagen = tipoImagen;
        this.rol = rol;
    }

    public Usuario() {

    }

    public String getFotoDataUrl() {
        if (fotoBase64 == null || fotoBase64.isBlank()) {
            return "/img/default-user.png";
        }
        return "data:" + tipoImagen + ";base64," + fotoBase64;
    }

    public String getFotoBase64() {
        return fotoBase64;
    }

    public void setFotoBase64(String fotoBase64) {
        this.fotoBase64 = fotoBase64;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}

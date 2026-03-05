package model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Lugar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;

    private int maxCapacidad;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fotoBase64;
    private String tipoImagen;

    @OneToMany
    private List<Evento> eventos;

    public Lugar() {

    }

    public Lugar(String nombre, int maxCapacidad, String fotoBase64, String tipoImagen) {
        this.nombre = nombre;
        this.maxCapacidad = maxCapacidad;
        this.fotoBase64 = fotoBase64;
        this.tipoImagen = tipoImagen;
    }

    public String getFotoDataUrl() {
        if (fotoBase64 == null || fotoBase64.isBlank()) {
            return "/img/default-place.png";
        }
        return "data:" + tipoImagen + ";base64," + fotoBase64;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getMaxCapacidad() {
        return maxCapacidad;
    }

    public void setMaxCapacidad(int maxCapacidad) {
        this.maxCapacidad = maxCapacidad;
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
}

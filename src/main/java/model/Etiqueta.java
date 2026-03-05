package model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Etiqueta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nombre;

    @ManyToMany(mappedBy = "etiquetas")
    private List<Evento> eventos = new ArrayList<>();

    public Etiqueta(String nombre) {
        this.nombre = nombre;
    }

    public Etiqueta() {}

    public Long getId() {
        return id;
    }

    public static String juntarEtiquetas(List<Etiqueta> etiquetas){
        StringBuilder juntar = new StringBuilder();
        for(Etiqueta etiqueta : etiquetas){
            juntar.append(etiqueta.getNombre()).append(", ");
        }
        return juntar.toString();
    }

    public String getNombre() {
        return nombre;
    }

    @PrePersist
    @PreUpdate
    private void normalizar() {
        if (nombre != null) {
            nombre = nombre.trim().toLowerCase();
        }
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

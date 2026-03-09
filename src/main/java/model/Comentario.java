package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 250)
    private String texto;

    @ManyToOne
    private Usuario autor;

    @ManyToOne
    private Evento evento;

    private LocalDateTime fecha;

    public Comentario(String texto, Usuario autor, Evento evento, LocalDateTime fecha) {
        this.texto = texto;
        this.autor = autor;
        this.evento = evento;
        this.fecha = LocalDateTime.now();
    }

    public Comentario() {

    }

    public Long getId() {
        return id;
    }

    public Evento getEventoId() {
        return evento;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Usuario getAutor() {
        return autor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}

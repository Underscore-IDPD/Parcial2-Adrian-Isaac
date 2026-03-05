package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_asistentes")
public class Inscripcion {

    @EmbeddedId
    private InscripcionId id;

    @ManyToOne
    @MapsId("eventoId")
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fechaInscripcion;

    private boolean asistio = false;

    public Inscripcion() {}

    public Inscripcion(Evento evento, Usuario usuario) {
        this.evento = evento;
        this.usuario = usuario;
        this.fechaInscripcion = LocalDateTime.now();

        this.id = new InscripcionId(
                evento.getId(),
                usuario.getId()
        );
    }

    public InscripcionId getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public boolean getAsistio() {
        return asistio;
    }

    public void marcarAsistio() {
        asistio = true;
    }
}
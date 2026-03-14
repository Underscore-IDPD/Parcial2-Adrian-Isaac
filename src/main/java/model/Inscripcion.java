package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime fechaInscripcion;

    private boolean asistio = false;
    private LocalDateTime fechaAsistencia;

    public Inscripcion() {}

    public Inscripcion(Evento evento, Usuario usuario) {
        this.evento = evento;
        this.usuario = usuario;
        this.fechaInscripcion = LocalDateTime.now();

        this.id = new InscripcionId(
                evento.getId(),
                usuario.getId()
        );
        this.token = UUID.randomUUID().toString();
    }

    public String getToken(){
        return token;
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

    public LocalDateTime getFechaAsistencia() { return fechaAsistencia; }

    public boolean getAsistio() {
        return asistio;
    }

    public void marcarAsistio() {
        asistio = true;
        fechaAsistencia = LocalDateTime.now();
    }
}
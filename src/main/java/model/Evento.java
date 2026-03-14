package model;

import jakarta.persistence.*;
import util.Estado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Table(
        name = "articulo",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"titulo", "cuerpo"}),
                @UniqueConstraint(columnNames = {"autor_id", "cuerpo"})}
)

@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String concepto;

    @Column(length = 5000)
    private String descripcion;

    private int cupoMaximo;
    @ManyToOne
    private Usuario organizador;

    @OneToMany
    private List<Inscripcion> inscripciones = new ArrayList<>();

    private Estado estado;

    private int duracionMinutos;

    @ManyToOne
    private Lugar lugar;

    @ManyToMany
    @JoinTable(
            name = "evento_etiqueta",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id")
    )
    private List<Etiqueta> etiquetas = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios = new ArrayList<>();
    private LocalDateTime fechaEvento;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bannerBase64;
    private String tipoImagen;

    public Evento(String concepto, String descripcion, int cupoMaximo, int duracion, Usuario organizador, Lugar lugar, LocalDateTime fechaEvento, List<Etiqueta> etiquetas, String banner, String tipoImagen) {
        this.bannerBase64 = banner;
        this.tipoImagen = tipoImagen;
        this.concepto = concepto;
        this.descripcion = descripcion;
        this.cupoMaximo = cupoMaximo;
        this.duracionMinutos = duracion;
        this.organizador = organizador;
        this.lugar = lugar;
        this.fechaEvento = fechaEvento;
        this.etiquetas = etiquetas;
        this.estado = Estado.Pendiente;
        this.fechaCreacion = this.fechaModificacion = LocalDateTime.now();
    }

    public Evento(){

    }

    public String getDescripcion() {
        return descripcion;
    }

    public void modificar(String concepto, String descripcion, int cupoMaximo, int duracionMinutos, LocalDateTime fechaEvento, List<Etiqueta> etiquetas, Lugar lugar) {
        boolean modificado = false;
        if(concepto != null) {
            this.concepto = concepto;
            modificado = true;
        }

        if(duracionMinutos != this.duracionMinutos) {
            this.duracionMinutos = duracionMinutos;
            modificado = true;
        }

        if(cupoMaximo != this.cupoMaximo) {
            this.cupoMaximo = cupoMaximo;
            modificado = true;
        }

        if(descripcion != null){
            this.descripcion = descripcion;
            modificado = true;
        }

        if(!this.fechaEvento.equals(fechaEvento)) {
            modificado = true;
            this.fechaEvento = fechaEvento;
        }

        if(!this.etiquetas.equals(etiquetas)) {
            modificado = true;
            this.etiquetas = etiquetas;
        }

        if(!this.lugar.equals(lugar)) {
            modificado = true;
            this.lugar = lugar;
        }

        if(modificado) fechaModificacion = LocalDateTime.now();
    }

    public void actualizarEstado(Estado estado) {
        if(estado == Estado.Cancelado || estado == Estado.Concluido) return;
        this.estado = estado;
    }

    public Estado getEstado() {
        return estado;
    }

    public boolean termino(){
        return estado == Estado.Concluido;
    }

    public void cancelarEvento(String razon){
        this.estado = Estado.Cancelado;
        this.descripcion = this.descripcion + "|||" + razon;
        this.fechaModificacion = LocalDateTime.now();
    }

    public Lugar getLugar() {
        return lugar;
    }

    public List<Comentario> getComentarios() {
        return comentarios;
    }

    public void addComentario(Comentario comentario) {
        this.comentarios.add(comentario);
    }

    public List<Etiqueta> getEtiquetas() {
        return etiquetas;
    }

    public Long getId() {
        return id;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConcepto() {
        return concepto;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public int  getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public String getDuracionFormateada() {
        return String.format("%02d:%02d:00",
                duracionMinutos / 60,
                duracionMinutos % 60);
    }

    public String getFotoDataUrl() {
        if (bannerBase64 == null || bannerBase64.isBlank()) {
            return "/img/default-banner.png";
        }
        return "data:" + tipoImagen + ";base64," + bannerBase64;
    }

    public String getFotoBase64() {
        return bannerBase64;
    }

    public void setFotoBase64(String fotoBase64) {
        this.bannerBase64 = fotoBase64;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }
}

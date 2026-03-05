package service;

import data.repositories.EventoRepositorio;
import data.repositories.EtiquetaRepositorio;
import jakarta.persistence.*;
import model.*;
import util.Estado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventoServicio {
    private final EventoRepositorio eventoRepositorio;
    private final EtiquetaRepositorio etiquetaRepositorio;

    public EventoServicio(EventoRepositorio repositorio, EtiquetaRepositorio etiquetaRepositorio) {
        eventoRepositorio = repositorio;
        this.etiquetaRepositorio = etiquetaRepositorio;
    }

    public Evento crearEvento(String concepto, String descripcion, int cupoMaximo, int duracionMinutos, Usuario organizador, Lugar lugar, LocalDateTime fechaEvento, String etiquetas, String bannerBase64, String tipoImagen, EntityManager em) {

        if (eventoRepositorio.buscarPorConceptoYOrganizador(concepto, organizador.getId(), em) != null ||
                eventoRepositorio.buscarPorConceptoYDescripcion(concepto, descripcion, em) != null) {
            return null;
        }

        em.getTransaction().begin();
        try {

            List<Etiqueta> listaEtiquetas = obtenerOCrearEtiquetas(etiquetas, em);
            Evento e = new Evento(concepto, descripcion, cupoMaximo, duracionMinutos, organizador, lugar, fechaEvento, listaEtiquetas, bannerBase64, tipoImagen);

            boolean estado = eventoRepositorio.guardar(e, em);
            if (!estado) {
                em.getTransaction().rollback();
                throw new RuntimeException("No se pudo crear evento");
            }

            em.getTransaction().commit();
            eliminarEtiquetasHuerfanas(em);
            return e;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public List<Evento> listarEventos(int pagina, EntityManager em) {
        return eventoRepositorio.listarPaginado(pagina, em);
    }

    public Long totalEventos(EntityManager em) {
        return em.createQuery("SELECT COUNT(e) FROM Evento e", Long.class)
                .getSingleResult();
    }

    public Evento buscarPorId(long id, EntityManager em) {
        Evento ert = eventoRepositorio.buscarPorId(id, em);
        if (ert == null) {
            IO.println("No se encontro el evento");
            throw new RuntimeException("Evento no encontrado");
        }
        return ert;
    }

    public boolean validarUnico(Evento e, EntityManager em) {
        return eventoRepositorio.buscarPorConceptoYOrganizador(e.getConcepto(), e.getOrganizador().getId(), em) == null
                && eventoRepositorio.buscarPorConceptoYDescripcion(e.getConcepto(), e.getDescripcion(), em) == null;
    }

    public List<Comentario> listarComentarios(Long idEvento, EntityManager em) {

        List<Comentario> lista = new ArrayList<>(buscarPorId(idEvento, em).getComentarios());
        lista.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

        return lista;
    }

    public void eliminarEvento(long id, EntityManager em) {
        if(!eventoRepositorio.borrar(id,em))
            throw new RuntimeException("Evento no encontrado");
        else eliminarEtiquetasHuerfanas(em);
    }

    public void cancelarEvento(long id, String razon, EntityManager em) {
        Evento e = buscarPorId(id, em);
        if (e != null) {
            e.cancelarEvento(razon);
        }
    }

    public Evento modificarEvento(Long id, String concepto, String descripcion, int cupoMaximo, int duracionMinutos, LocalDateTime fechaEvento, String etiquetas, Lugar lugar, EntityManager em) {
        Evento e = buscarPorId(id,em);
        String conceptoviejo = e.getConcepto();
        String descripcionviejo = e.getDescripcion();

        if(e != null && e.getEstado() != Estado.Cancelado && e.getEstado() != Estado.Concluido && e.getEstado() != Estado.En_Transcurso) {
            e.modificar(concepto,descripcion,cupoMaximo,duracionMinutos,fechaEvento,obtenerOCrearEtiquetas(etiquetas,em),lugar);
            if(!validarUnico(e,em) && (!conceptoviejo.equals(concepto) || !descripcionviejo.equals(descripcion))){
                eliminarEtiquetasHuerfanas(em);
                return null;
            }
            em.getTransaction().begin();
            eventoRepositorio.actualizar(e,em);
            em.getTransaction().commit();
            eliminarEtiquetasHuerfanas(em);
            return e;
        }
        throw new RuntimeException("Evento no encontrado");
    }

    public List<Etiqueta> listarEtiquetas(EntityManager em) {
        return etiquetaRepositorio.listar(em);
    }

    public List<Evento> buscarPorEtiqueta(String nombre, EntityManager em) {
        return eventoRepositorio.buscarPorEtiqueta(nombre, em);
    }

    public List<Etiqueta> obtenerOCrearEtiquetas(String etiquetas, EntityManager em) {
        List<Etiqueta> etiquetasList = new ArrayList<>();

        if (etiquetas == null || etiquetas.trim().isEmpty()) {
            return new ArrayList<>();
        }

        for (String etiqueta : etiquetas.split(",")) {
            String limpia = etiqueta.trim().toLowerCase();
            if (limpia.isEmpty()) continue;
            etiquetasList.add(etiquetaRepositorio.obtenerOCrear(limpia, em));
        }

        return etiquetasList;
    }

    public void eliminarEtiquetasHuerfanas(EntityManager em) {
        em.getTransaction().begin();
        etiquetaRepositorio.eliminarEtiquetasHuerfanas(em);
        em.getTransaction().commit();
    }
}

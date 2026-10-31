package sigelab.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import sigelab.enums.EstadoReserva;

public class Reserva {

    private Integer idReserva;
    private int idUsuario;
    private int idEquipo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoReserva estado;
    private LocalDateTime fechaCreacion;
    private String observaciones;

    public Reserva() {
    }

    public Reserva(int idUsuario, int idEquipo, LocalDate fechaInicio, LocalDate fechaFin, EstadoReserva estado, LocalDateTime fechaCreacion, String observaciones) {
        this.idUsuario = idUsuario;
        this.idEquipo = idEquipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.observaciones = observaciones;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}

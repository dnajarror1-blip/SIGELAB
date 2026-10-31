package sigelab.model;

import java.time.LocalDate;
import sigelab.enums.EstadoPrestamo;

public class Prestamo {

    private Integer idPrestamo;
    private int idUsuarioSolicitante;
    private int idUsuarioRegistra;
    private LocalDate fechaPrestamo;
    private LocalDate fechaEsperadaDevolucion;
    private EstadoPrestamo estado;
    private String observaciones;

    public Prestamo() {
    }

    public Prestamo(int idUsuarioSolicitante, int idUsuarioRegistra, LocalDate fechaPrestamo, LocalDate fechaEsperadaDevolucion, EstadoPrestamo estado, String observaciones) {
        this.idUsuarioSolicitante = idUsuarioSolicitante;
        this.idUsuarioRegistra = idUsuarioRegistra;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Integer getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Integer idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public int getIdUsuarioSolicitante() {
        return idUsuarioSolicitante;
    }

    public void setIdUsuarioSolicitante(int idUsuarioSolicitante) {
        this.idUsuarioSolicitante = idUsuarioSolicitante;
    }

    public int getIdUsuarioRegistra() {
        return idUsuarioRegistra;
    }

    public void setIdUsuarioRegistra(int idUsuarioRegistra) {
        this.idUsuarioRegistra = idUsuarioRegistra;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaEsperadaDevolucion() {
        return fechaEsperadaDevolucion;
    }

    public void setFechaEsperadaDevolucion(LocalDate fechaEsperadaDevolucion) {
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}

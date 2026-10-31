package sigelab.model;

import java.time.LocalDate;
import sigelab.enums.CondicionDevolucion;

public class Devolucion {

    private Integer idDevolucion;
    private int idDetallePrestamo;
    private int idUsuario;
    private LocalDate fechaDevolucion;
    private CondicionDevolucion condicion;
    private String observaciones;

    public Devolucion() {
    }

    public Devolucion(int idDetallePrestamo, int idUsuario, LocalDate fechaDevolucion, CondicionDevolucion condicion, String observaciones) {
        this.idDetallePrestamo = idDetallePrestamo;
        this.idUsuario = idUsuario;
        this.fechaDevolucion = fechaDevolucion;
        this.condicion = condicion;
        this.observaciones = observaciones;
    }

    public Integer getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(Integer idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public int getIdDetallePrestamo() {
        return idDetallePrestamo;
    }

    public void setIdDetallePrestamo(int idDetallePrestamo) {
        this.idDetallePrestamo = idDetallePrestamo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public CondicionDevolucion getCondicion() {
        return condicion;
    }

    public void setCondicion(CondicionDevolucion condicion) {
        this.condicion = condicion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}

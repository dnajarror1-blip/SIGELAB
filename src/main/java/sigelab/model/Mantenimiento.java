package sigelab.model;

import java.time.LocalDate;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;

public class Mantenimiento {

    private Integer idMantenimiento;
    private int idEquipo;
    private LocalDate fechaIngreso;
    private TipoMantenimiento tipo;
    private String descripcion;
    private int idResponsable;
    private LocalDate fechaSalida;
    private ResultadoMantenimiento resultado;
    private EstadoMantenimiento estado;
    private String observaciones;

    public Mantenimiento() {
    }

    public Mantenimiento(int idEquipo, LocalDate fechaIngreso, TipoMantenimiento tipo, String descripcion, int idResponsable, LocalDate fechaSalida, ResultadoMantenimiento resultado, EstadoMantenimiento estado, String observaciones) {
        this.idEquipo = idEquipo;
        this.fechaIngreso = fechaIngreso;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.idResponsable = idResponsable;
        this.fechaSalida = fechaSalida;
        this.resultado = resultado;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Integer getIdMantenimiento() {
        return idMantenimiento;
    }

    public void setIdMantenimiento(Integer idMantenimiento) {
        this.idMantenimiento = idMantenimiento;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public TipoMantenimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMantenimiento tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getIdResponsable() {
        return idResponsable;
    }

    public void setIdResponsable(int idResponsable) {
        this.idResponsable = idResponsable;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public ResultadoMantenimiento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoMantenimiento resultado) {
        this.resultado = resultado;
    }

    public EstadoMantenimiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoMantenimiento estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}

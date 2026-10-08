package sigelab.model;

import sigelab.enums.EstadoEquipo;

public class Equipo {

    private Integer idEquipo;
    private String codigoInventario;
    private String nombre;
    private int idTipoEquipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private int idLaboratorio;
    private EstadoEquipo estado;
    private String observaciones;

    public Equipo() {

    }

    public Equipo(String codigoInventario, String nombre, int idTipoEquipo, String marca, String modelo, String numeroSerie, int idLaboratorio, EstadoEquipo estado, String observaciones) {
        this.codigoInventario = codigoInventario;
        this.nombre = nombre;
        this.idTipoEquipo = idTipoEquipo;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.idLaboratorio = idLaboratorio;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Integer getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Integer idEquipo) {
        this.idEquipo = idEquipo;
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public void setCodigoInventario(String codigoInventario) {
        this.codigoInventario = codigoInventario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(int idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

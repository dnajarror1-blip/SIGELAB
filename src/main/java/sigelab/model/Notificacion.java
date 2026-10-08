package sigelab.model;

import java.time.LocalDateTime;
import sigelab.enums.TipoNotificacion;

public class Notificacion {

    private Integer idNotificacion;
    private int idUsuario;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private LocalDateTime fechaHora;
    private boolean leida;
    private String entidad;
    private Integer idReferencia;

    public Notificacion() {
    }

    public Notificacion(int idUsuario, TipoNotificacion tipo, String titulo, String mensaje, LocalDateTime fechaHora, boolean leida, String entidad, Integer idReferencia) {
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fechaHora = fechaHora;
        this.leida = leida;
        this.entidad = entidad;
        this.idReferencia = idReferencia;
    }

    public Integer getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Integer idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Integer getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(Integer idReferencia) {
        this.idReferencia = idReferencia;
    }

}

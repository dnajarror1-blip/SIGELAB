package sigelab.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuditoriaFacial {

    private Integer idAuditoriaFacial;
    private int idUsuario;
    private LocalDateTime fechaHora;
    private String accion;
    private String resultado;
    private BigDecimal score;
    private BigDecimal threshold;
    private String detalle;
    private String dispositivo;

    public AuditoriaFacial() {
    }

    public AuditoriaFacial(int idUsuario, LocalDateTime fechaHora, String accion, String resultado, BigDecimal score, BigDecimal threshold, String detalle, String dispositivo) {
        this.idUsuario = idUsuario;
        this.fechaHora = fechaHora;
        this.accion = accion;
        this.resultado = resultado;
        this.score = score;
        this.threshold = threshold;
        this.detalle = detalle;
        this.dispositivo = dispositivo;
    }

    public Integer getIdAuditoriaFacial() {
        return idAuditoriaFacial;
    }

    public void setIdAuditoriaFacial(Integer idAuditoriaFacial) {
        this.idAuditoriaFacial = idAuditoriaFacial;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

}

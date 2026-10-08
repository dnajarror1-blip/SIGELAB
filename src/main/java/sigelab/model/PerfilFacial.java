package sigelab.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

public class PerfilFacial {

    private Integer idPerfilFacial;
    private int idUsuario;
    private byte[] embedding;
    private String modelo;
    private int dimension;
    private int cantidadMuestras;
    private BigDecimal qualityScore;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private boolean activo;

    public PerfilFacial() {
    }

    public PerfilFacial(int idUsuario, byte[] embedding, String modelo, int dimension, int cantidadMuestras, BigDecimal qualityScore, LocalDateTime fechaRegistro, LocalDateTime fechaActualizacion, boolean activo) {
        this.idUsuario = idUsuario;
        this.embedding = embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
        this.modelo = modelo;
        this.dimension = dimension;
        this.cantidadMuestras = cantidadMuestras;
        this.qualityScore = qualityScore;
        this.fechaRegistro = fechaRegistro;
        this.fechaActualizacion = fechaActualizacion;
        this.activo = activo;
    }

    public Integer getIdPerfilFacial() {
        return idPerfilFacial;
    }

    public void setIdPerfilFacial(Integer idPerfilFacial) {
        this.idPerfilFacial = idPerfilFacial;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public byte[] getEmbedding() {
        return embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
    }

    public void setEmbedding(byte[] embedding) {
        this.embedding = embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getCantidadMuestras() {
        return cantidadMuestras;
    }

    public void setCantidadMuestras(int cantidadMuestras) {
        this.cantidadMuestras = cantidadMuestras;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

}

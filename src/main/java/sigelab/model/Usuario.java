package sigelab.model;

import sigelab.enums.EstadoUsuario;

public class Usuario {

    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String username;
    private String passwordHash;
    private String correo;
    private int idRol;
    private EstadoUsuario estado;

    public Usuario() {
    }

    public Usuario(String nombre, String apellido, String username, String passwordHash, String correo, int idRol, EstadoUsuario estado) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.username = username;
        this.passwordHash = passwordHash;
        this.correo = correo;
        this.idRol = idRol;
        this.estado = estado;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

}

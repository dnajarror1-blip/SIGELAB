package sigelab.model;

public class DetallePrestamo {

    private Integer idDetallePrestamo;
    private int idPrestamo;
    private int idEquipo;

    public DetallePrestamo() {
    }

    public DetallePrestamo(int idPrestamo, int idEquipo) {
        this.idPrestamo = idPrestamo;
        this.idEquipo = idEquipo;
    }

    public Integer getIdDetallePrestamo() {
        return idDetallePrestamo;
    }

    public void setIdDetallePrestamo(Integer idDetallePrestamo) {
        this.idDetallePrestamo = idDetallePrestamo;
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(int idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

}

package sigelab.service;

import sigelab.enums.EstadoEquipo;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.integration.qr.QrService;
import sigelab.model.Equipo;
import sigelab.repository.EquipoRepository;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class QrEquipoService {
    private final QrService qr;
    private final EquipoLookup equipos;

    public QrEquipoService() {
        this(new QrService(), new EquipoRepository()::buscarPorCodigoInventario);
    }

    public QrEquipoService(QrService qr, EquipoLookup equipos) {
        this.qr = qr;
        this.equipos = equipos;
    }

    public Resultado identificar(File imagen) {
        String codigo = qr.leer(imagen).trim();
        return new Resultado(codigo, buscar(codigo));
    }

    public void generar(String codigo, File destino) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código de inventario es obligatorio");
        }
        Equipo equipo = buscar(codigo.trim());
        qr.generar(equipo.getCodigoInventario(), destino);
    }

    private Equipo buscar(String codigo) {
        try {
            return equipos.buscar(codigo).orElseThrow(() ->
                    new BusinessException("No existe un equipo con ese código de inventario"));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el equipo identificado por QR", e);
        }
    }

    @FunctionalInterface
    public interface EquipoLookup {
        Optional<Equipo> buscar(String codigo) throws SQLException;
    }

    public record Resultado(String codigoInventario, Equipo equipo) {
        public boolean disponibleParaUso() {
            return equipo.getEstado() == EstadoEquipo.DISPONIBLE;
        }

        public boolean bloqueadoPorPrestamo() {
            return equipo.getEstado() == EstadoEquipo.PRESTADO;
        }
    }
}

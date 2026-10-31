package sigelab.repository;

import sigelab.exception.DataAccessException;
import sigelab.model.DetallePrestamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import sigelab.config.DatabaseConfig;

public class DetallePrestamoRepository {

    public java.util.Optional<DetallePrestamo> buscarPorId(Connection conn, int idDetalle) throws SQLException {
        String sql = "SELECT id_detalle_prestamo, id_prestamo, id_equipo FROM dbo.DetallePrestamo WHERE id_detalle_prestamo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDetalle);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? java.util.Optional.of(mapear(rs)) : java.util.Optional.empty();
            }
        }
    }

    public List<DetallePrestamo> buscarPendientes(Connection conn, int idPrestamo) throws SQLException {
        String sql = """
                SELECT dp.id_detalle_prestamo, dp.id_prestamo, dp.id_equipo
                FROM dbo.DetallePrestamo dp WHERE dp.id_prestamo = ?
                  AND NOT EXISTS (SELECT 1 FROM dbo.Devolucion d
                                  WHERE d.id_detalle_prestamo = dp.id_detalle_prestamo)
                ORDER BY dp.id_equipo, dp.id_detalle_prestamo
                """;
        List<DetallePrestamo> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private DetallePrestamo mapear(ResultSet rs) throws SQLException {
        DetallePrestamo d = new DetallePrestamo(rs.getInt("id_prestamo"), rs.getInt("id_equipo"));
        d.setIdDetallePrestamo(rs.getInt("id_detalle_prestamo"));
        return d;
    }

    public void guardar(
            Connection connection,
            DetallePrestamo detalle
    ) {
        String sql = """
                INSERT INTO dbo.DetallePrestamo (
                    id_prestamo,
                    id_equipo
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, detalle.getIdPrestamo());
            statement.setInt(2, detalle.getIdEquipo());

            int filasInsertadas = statement.executeUpdate();

            if (filasInsertadas != 1) {
                throw new DataAccessException(
                        "No se pudo guardar el detalle del préstamo."
                );
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al guardar el detalle del préstamo.", e
            );
        }
    }
    public List<DetallePrestamo> buscarPorPrestamo(int idPrestamo) {
        String sql = """
            SELECT id_detalle_prestamo, id_prestamo, id_equipo
            FROM dbo.DetallePrestamo
            WHERE id_prestamo = ?
            ORDER BY id_detalle_prestamo
            """;

        List<DetallePrestamo> detalles = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrestamo);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetallePrestamo detalle = new DetallePrestamo();

                    detalle.setIdDetallePrestamo(
                            rs.getInt("id_detalle_prestamo")
                    );
                    detalle.setIdPrestamo(rs.getInt("id_prestamo"));
                    detalle.setIdEquipo(rs.getInt("id_equipo"));

                    detalles.add(detalle);
                }
            }

            return detalles;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar los equipos del préstamo.", e
            );
        }
    }
}

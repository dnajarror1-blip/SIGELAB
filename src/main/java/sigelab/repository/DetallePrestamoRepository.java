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
package sigelab.repository;

import sigelab.exception.DataAccessException;
import sigelab.model.DetallePrestamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
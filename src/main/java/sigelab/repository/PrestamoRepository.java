package sigelab.repository;

import sigelab.exception.DataAccessException;
import sigelab.model.Prestamo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PrestamoRepository {

    public int guardar(Connection connection, Prestamo prestamo) {
        String sql = """
                INSERT INTO dbo.Prestamo (
                    id_usuario_solicitante,
                    id_usuario_registra,
                    fecha_prestamo,
                    fecha_esperada_devolucion,
                    estado,
                    observaciones
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, prestamo.getIdUsuarioSolicitante());
            statement.setInt(2, prestamo.getIdUsuarioRegistra());
            statement.setDate(3, Date.valueOf(prestamo.getFechaPrestamo()));
            statement.setDate(
                    4,
                    Date.valueOf(prestamo.getFechaEsperadaDevolucion())
            );
            statement.setString(5, prestamo.getEstado().name());
            statement.setNString(6, prestamo.getObservaciones());

            int filasInsertadas = statement.executeUpdate();

            if (filasInsertadas != 1) {
                throw new DataAccessException(
                        "No se pudo guardar el préstamo."
                );
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new DataAccessException(
                    "No se pudo obtener el identificador del préstamo."
            );

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al guardar el préstamo.", e
            );
        }
    }
}
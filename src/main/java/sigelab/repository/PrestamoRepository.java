package sigelab.repository;

import sigelab.exception.DataAccessException;
import sigelab.model.Prestamo;
import sigelab.model.Rol;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoPrestamo;
import java.util.ArrayList;
import java.util.List;

public class PrestamoRepository {

    /** Serializa devoluciones del mismo encabezado, incluidas las parciales. */
    public Optional<Prestamo> buscarParaDevolucion(Connection conn, int idPrestamo) throws SQLException {
        String sql = """
                SELECT id_prestamo, id_usuario_solicitante, id_usuario_registra,
                       fecha_prestamo, fecha_esperada_devolucion, estado, observaciones
                FROM dbo.Prestamo WITH (UPDLOCK, HOLDLOCK) WHERE id_prestamo = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Prestamo p = new Prestamo(rs.getInt("id_usuario_solicitante"),
                        rs.getInt("id_usuario_registra"), rs.getDate("fecha_prestamo").toLocalDate(),
                        rs.getDate("fecha_esperada_devolucion").toLocalDate(),
                        EstadoPrestamo.valueOf(rs.getString("estado")), rs.getString("observaciones"));
                p.setIdPrestamo(rs.getInt("id_prestamo"));
                return Optional.of(p);
            }
        }
    }

    public void finalizarSinPendientes(Connection conn, int idPrestamo) throws SQLException {
        String sql = """
                UPDATE p SET estado = 'FINALIZADO' FROM dbo.Prestamo p
                WHERE p.id_prestamo = ? AND p.estado = 'ACTIVO'
                  AND NOT EXISTS (
                    SELECT 1 FROM dbo.DetallePrestamo dp WHERE dp.id_prestamo = p.id_prestamo
                      AND NOT EXISTS (SELECT 1 FROM dbo.Devolucion d
                                      WHERE d.id_detalle_prestamo = dp.id_detalle_prestamo))
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            if (stmt.executeUpdate() != 1) throw new SQLException("No se pudo finalizar el préstamo.");
        }
    }

    /** Consulta mínima de los participantes usando la conexión del préstamo. */
    public Optional<Rol> buscarRolDeUsuarioActivo(Connection conn, int idUsuario) {
        String sql = """
                SELECT r.id_rol, r.nombre
                FROM dbo.Usuario u
                JOIN dbo.Rol r ON r.id_rol = u.id_rol
                WHERE u.id_usuario = ? AND u.estado = 'ACTIVO'
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Rol rol = new Rol(rs.getString("nombre"));
                rol.setIdRol(rs.getInt("id_rol"));
                return Optional.of(rol);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar al participante del préstamo.", e);
        }
    }

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
    public Optional<Prestamo> buscarPorId(int idPrestamo) {
        String sql = """
            SELECT id_prestamo,
                   id_usuario_solicitante,
                   id_usuario_registra,
                   fecha_prestamo,
                   fecha_esperada_devolucion,
                   estado,
                   observaciones
            FROM dbo.Prestamo
            WHERE id_prestamo = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrestamo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Prestamo prestamo = new Prestamo();
                prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                prestamo.setIdUsuarioSolicitante(
                        rs.getInt("id_usuario_solicitante")
                );
                prestamo.setIdUsuarioRegistra(
                        rs.getInt("id_usuario_registra")
                );
                prestamo.setFechaPrestamo(
                        rs.getDate("fecha_prestamo").toLocalDate()
                );
                prestamo.setFechaEsperadaDevolucion(
                        rs.getDate("fecha_esperada_devolucion").toLocalDate()
                );
                prestamo.setEstado(
                        EstadoPrestamo.valueOf(rs.getString("estado"))
                );
                prestamo.setObservaciones(rs.getString("observaciones"));

                return Optional.of(prestamo);
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar el préstamo.", e
            );
        }
    }
    public List<Prestamo> buscarActivos() {
        String sql = """
            SELECT id_prestamo,
                   id_usuario_solicitante,
                   id_usuario_registra,
                   fecha_prestamo,
                   fecha_esperada_devolucion,
                   estado,
                   observaciones
            FROM dbo.Prestamo
            WHERE estado = ?
            ORDER BY id_prestamo
            """;

        List<Prestamo> prestamos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, EstadoPrestamo.ACTIVO.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setIdUsuarioSolicitante(
                            rs.getInt("id_usuario_solicitante")
                    );
                    prestamo.setIdUsuarioRegistra(
                            rs.getInt("id_usuario_registra")
                    );
                    prestamo.setFechaPrestamo(
                            rs.getDate("fecha_prestamo").toLocalDate()
                    );
                    prestamo.setFechaEsperadaDevolucion(
                            rs.getDate("fecha_esperada_devolucion").toLocalDate()
                    );
                    prestamo.setEstado(
                            EstadoPrestamo.valueOf(rs.getString("estado"))
                    );
                    prestamo.setObservaciones(rs.getString("observaciones"));

                    prestamos.add(prestamo);
                }
            }

            return prestamos;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar los préstamos activos.", e
            );
        }
    }
    public List<Prestamo> buscarActivosPorSolicitante(int idSolicitante) {
        String sql = """
            SELECT id_prestamo,
                   id_usuario_solicitante,
                   id_usuario_registra,
                   fecha_prestamo,
                   fecha_esperada_devolucion,
                   estado,
                   observaciones
            FROM dbo.Prestamo
            WHERE estado = ?
              AND id_usuario_solicitante = ?
            ORDER BY id_prestamo
            """;

        List<Prestamo> prestamos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, EstadoPrestamo.ACTIVO.name());
            stmt.setInt(2, idSolicitante);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();

                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setIdUsuarioSolicitante(
                            rs.getInt("id_usuario_solicitante")
                    );
                    prestamo.setIdUsuarioRegistra(
                            rs.getInt("id_usuario_registra")
                    );
                    prestamo.setFechaPrestamo(
                            rs.getDate("fecha_prestamo").toLocalDate()
                    );
                    prestamo.setFechaEsperadaDevolucion(
                            rs.getDate("fecha_esperada_devolucion").toLocalDate()
                    );
                    prestamo.setEstado(
                            EstadoPrestamo.valueOf(rs.getString("estado"))
                    );
                    prestamo.setObservaciones(rs.getString("observaciones"));

                    prestamos.add(prestamo);
                }
            }

            return prestamos;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar los préstamos del solicitante.", e
            );
        }
    }
}

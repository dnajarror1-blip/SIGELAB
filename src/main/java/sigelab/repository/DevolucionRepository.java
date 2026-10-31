package sigelab.repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import sigelab.model.Devolucion;

public class DevolucionRepository {
    public record PrestamoPendiente(int idPrestamo, String solicitante,
                                   LocalDate fechaPrestamo, LocalDate fechaEsperada) { }

    public List<PrestamoPendiente> listarPrestamosPendientes(Connection conn) throws SQLException {
        String sql = """
                SELECT p.id_prestamo, u.nombre, u.apellido, p.fecha_prestamo, p.fecha_esperada_devolucion
                FROM dbo.Prestamo p JOIN dbo.Usuario u ON u.id_usuario = p.id_usuario_solicitante
                WHERE p.estado = 'ACTIVO' AND EXISTS (
                    SELECT 1 FROM dbo.DetallePrestamo dp WHERE dp.id_prestamo = p.id_prestamo
                      AND NOT EXISTS (SELECT 1 FROM dbo.Devolucion d
                                      WHERE d.id_detalle_prestamo = dp.id_detalle_prestamo))
                ORDER BY p.fecha_esperada_devolucion, p.id_prestamo
                """;
        List<PrestamoPendiente> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(new PrestamoPendiente(rs.getInt("id_prestamo"),
                    rs.getString("nombre") + " " + rs.getString("apellido"),
                    rs.getDate("fecha_prestamo").toLocalDate(),
                    rs.getDate("fecha_esperada_devolucion").toLocalDate()));
        }
        return lista;
    }

    public boolean existeParaDetalle(Connection conn, int idDetalle) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM dbo.Devolucion WHERE id_detalle_prestamo = ?")) {
            stmt.setInt(1, idDetalle);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    public int guardar(Connection conn, Devolucion devolucion) throws SQLException {
        String sql = """
                INSERT INTO dbo.Devolucion
                    (id_detalle_prestamo, id_usuario, fecha_devolucion, condicion, observaciones)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, devolucion.getIdDetallePrestamo());
            stmt.setInt(2, devolucion.getIdUsuario());
            stmt.setDate(3, Date.valueOf(devolucion.getFechaDevolucion()));
            stmt.setString(4, devolucion.getCondicion().name());
            stmt.setNString(5, devolucion.getObservaciones());
            if (stmt.executeUpdate() != 1) throw new SQLException("No se pudo registrar la devolución.");
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new SQLException("No se obtuvo el identificador de devolución.");
        }
    }
}

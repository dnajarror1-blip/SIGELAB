package sigelab.repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoReserva;
import sigelab.model.Reserva;

public class ReservaRepository {
    public record Solicitante(int idUsuario, String nombre) { }
    public record EquipoReservable(int idEquipo, String codigo, String nombre, EstadoEquipo estado) { }
    public record Resumen(Reserva reserva, String solicitante, String codigoEquipo, String nombreEquipo) { }

    public boolean existeSolicitanteActivo(Connection c, int idUsuario) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM dbo.Usuario WHERE id_usuario = ? AND estado = 'ACTIVO'")) {
            s.setInt(1,idUsuario);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    public List<Solicitante> listarSolicitantesActivos(Connection c) throws SQLException {
        List<Solicitante> lista = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id_usuario, nombre, apellido, username FROM dbo.Usuario WHERE estado = 'ACTIVO' ORDER BY nombre, apellido");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) lista.add(new Solicitante(rs.getInt("id_usuario"),
                    rs.getString("nombre") + " " + rs.getString("apellido") + " (" + rs.getString("username") + ")"));
        }
        return lista;
    }

    public List<EquipoReservable> listarEquipos(Connection c) throws SQLException {
        List<EquipoReservable> lista = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("SELECT id_equipo, codigo_inventario, nombre, estado FROM dbo.Equipo ORDER BY codigo_inventario");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) lista.add(new EquipoReservable(rs.getInt("id_equipo"),rs.getString("codigo_inventario"),
                    rs.getString("nombre"),EstadoEquipo.valueOf(rs.getString("estado"))));
        }
        return lista;
    }

    /** Intervalos DATE inclusivos. Canceladas y completadas no bloquean nuevas reservas. */
    public boolean existeSolapamiento(Connection c, int idEquipo, LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = """
                SELECT 1 FROM dbo.Reserva WHERE id_equipo = ? AND estado = 'ACTIVA'
                    AND fecha_inicio <= ? AND fecha_fin >= ?
                """;
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1,idEquipo); s.setDate(2,java.sql.Date.valueOf(fin)); s.setDate(3,java.sql.Date.valueOf(inicio));
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    public int guardar(Connection c, Reserva r) throws SQLException {
        String sql = """
                INSERT INTO dbo.Reserva (id_usuario, id_equipo, fecha_inicio, fecha_fin, estado, fecha_creacion, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement s = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1,r.getIdUsuario()); s.setInt(2,r.getIdEquipo());
            s.setDate(3,java.sql.Date.valueOf(r.getFechaInicio())); s.setDate(4,java.sql.Date.valueOf(r.getFechaFin()));
            s.setString(5,r.getEstado().name()); s.setTimestamp(6,Timestamp.valueOf(r.getFechaCreacion()));
            s.setNString(7,r.getObservaciones());
            if (s.executeUpdate()!=1) throw new SQLException("No se pudo registrar la reserva.");
            try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
            throw new SQLException("No se obtuvo el identificador de la reserva.");
        }
    }

    public Optional<Reserva> buscarPorId(Connection c, int idReserva) throws SQLException {
        return buscar(c,idReserva,false);
    }

    public Optional<Reserva> buscarParaCancelar(Connection c, int idReserva) throws SQLException {
        return buscar(c,idReserva,true);
    }

    private Optional<Reserva> buscar(Connection c, int idReserva, boolean bloquear) throws SQLException {
        String sql = "SELECT * FROM dbo.Reserva " + (bloquear ? "WITH (UPDLOCK, HOLDLOCK) " : "") + "WHERE id_reserva = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1,idReserva);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    public List<Resumen> listar(Connection c) throws SQLException {
        String sql = """
                SELECT r.*, u.nombre AS nombre_usuario, u.apellido AS apellido_usuario,
                       e.codigo_inventario, e.nombre AS nombre_equipo
                FROM dbo.Reserva r JOIN dbo.Usuario u ON u.id_usuario = r.id_usuario
                JOIN dbo.Equipo e ON e.id_equipo = r.id_equipo
                ORDER BY r.fecha_inicio DESC, r.id_reserva DESC
                """;
        List<Resumen> lista = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet rs = s.executeQuery()) {
            while (rs.next()) lista.add(new Resumen(mapear(rs),rs.getString("nombre_usuario") + " "
                    + rs.getString("apellido_usuario"),rs.getString("codigo_inventario"),rs.getString("nombre_equipo")));
        }
        return lista;
    }

    public void cancelar(Connection c, int idReserva) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE dbo.Reserva SET estado = 'CANCELADA' WHERE id_reserva = ? AND estado = 'ACTIVA'")) {
            s.setInt(1,idReserva);
            if (s.executeUpdate()!=1) throw new SQLException("La reserva ya no permite cancelación.");
        }
    }

    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva(rs.getInt("id_usuario"),rs.getInt("id_equipo"),
                rs.getDate("fecha_inicio").toLocalDate(),rs.getDate("fecha_fin").toLocalDate(),
                EstadoReserva.valueOf(rs.getString("estado")),rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                rs.getString("observaciones"));
        r.setIdReserva(rs.getInt("id_reserva"));
        return r;
    }
}

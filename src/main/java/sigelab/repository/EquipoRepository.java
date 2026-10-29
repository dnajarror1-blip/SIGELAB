package sigelab.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;

public class EquipoRepository {

    public Optional<Equipo> buscarPorId(int idEquipo) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return buscarPorId(idEquipo, conn);
        }
    }

    public Optional<Equipo> buscarPorId(int idEquipo, Connection conn) throws SQLException {
        String sql = "SELECT id_equipo, codigo_inventario, nombre, id_tipo_equipo, marca, modelo, "
                   + "numero_serie, id_laboratorio, estado, observaciones FROM dbo.Equipo WHERE id_equipo = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEquipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearEquipo(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void actualizarEstado(int idEquipo, EstadoEquipo nuevoEstado, Connection conn) throws SQLException {
        String sql = "UPDATE dbo.Equipo SET estado = ? WHERE id_equipo = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado.name());
            stmt.setInt(2, idEquipo);
            stmt.executeUpdate();
        }
    }

    public List<Equipo> listarTodos() throws SQLException {
        String sql = "SELECT id_equipo, codigo_inventario, nombre, id_tipo_equipo, marca, modelo, "
                   + "numero_serie, id_laboratorio, estado, observaciones FROM dbo.Equipo ORDER BY id_equipo ASC";
        List<Equipo> lista = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearEquipo(rs));
            }
        }
        return lista;
    }

    private Equipo mapearEquipo(ResultSet rs) throws SQLException {
        Equipo equipo = new Equipo();
        equipo.setIdEquipo(rs.getInt("id_equipo"));
        equipo.setCodigoInventario(rs.getString("codigo_inventario"));
        equipo.setNombre(rs.getString("nombre"));
        equipo.setIdTipoEquipo(rs.getInt("id_tipo_equipo"));
        equipo.setMarca(rs.getString("marca"));
        equipo.setModelo(rs.getString("modelo"));
        equipo.setNumeroSerie(rs.getString("numero_serie"));
        equipo.setIdLaboratorio(rs.getInt("id_laboratorio"));

        String estadoStr = rs.getString("estado");
        if (estadoStr != null) {
            equipo.setEstado(EstadoEquipo.valueOf(estadoStr));
        }

        equipo.setObservaciones(rs.getString("observaciones"));
        return equipo;
    }

    public Optional<Equipo> buscarPorCodigoInventario(String codigoInventario) throws SQLException {
        if (codigoInventario == null || codigoInventario.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT id_equipo, codigo_inventario, nombre, id_tipo_equipo,
                       marca, modelo, numero_serie, id_laboratorio,
                       estado, observaciones
                FROM dbo.Equipo
                WHERE codigo_inventario = ?
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoInventario.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearEquipo(rs));
                }
            }
        }
        return Optional.empty();
    }

}

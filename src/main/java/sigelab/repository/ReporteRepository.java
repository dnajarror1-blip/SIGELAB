
package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoEquipo;
import sigelab.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReporteRepository {

    public List<Map<String, Object>> listarEquiposPorEstado(EstadoEquipo estado) {
        String sql = """
                SELECT id_equipo, codigo_inventario, nombre, marca,
                       modelo, numero_serie, estado
                FROM dbo.Equipo
                WHERE estado = ?
                ORDER BY nombre ASC
                """;

        List<Map<String, Object>> resultados = new ArrayList<>();

        try (Connection conexion = DatabaseConfig.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, estado.name());

            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();

                    fila.put("id_equipo", rs.getInt("id_equipo"));
                    fila.put("codigo_inventario", rs.getString("codigo_inventario"));
                    fila.put("nombre", rs.getString("nombre"));
                    fila.put("marca", rs.getString("marca"));
                    fila.put("modelo", rs.getString("modelo"));
                    fila.put("numero_serie", rs.getString("numero_serie"));
                    fila.put("estado", rs.getString("estado"));

                    resultados.add(fila);
                }
            }

            return resultados;

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Error al consultar el reporte de equipos por estado.", e);
        }
    }
}

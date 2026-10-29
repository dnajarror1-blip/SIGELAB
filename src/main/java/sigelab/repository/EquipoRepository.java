package sigelab.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;

public class EquipoRepository {

    // Busca un equipo por ID usando una conexión propia.
    public Optional<Equipo> buscarPorId(int idEquipo) throws SQLException {

        try (Connection conn = DatabaseConfig.getConnection()) {
            return buscarPorId(idEquipo, conn);
        }
    }


    // Busca un equipo por ID dentro de una transacción existente.
    public Optional<Equipo> buscarPorId(
            int idEquipo,
            Connection conn
    ) throws SQLException {

        String sql = """
                SELECT id_equipo,
                       codigo_inventario,
                       nombre,
                       id_tipo_equipo,
                       marca,
                       modelo,
                       numero_serie,
                       id_laboratorio,
                       estado,
                       observaciones
                FROM dbo.Equipo
                WHERE id_equipo = ?
                """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setInt(1, idEquipo);

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(
                            mapearEquipo(rs)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // Busca por código único de inventario.
    public Optional<Equipo> buscarPorCodigoInventario(
            String codigoInventario
    ) throws SQLException {

        String sql = """
                SELECT id_equipo,
                       codigo_inventario,
                       nombre,
                       id_tipo_equipo,
                       marca,
                       modelo,
                       numero_serie,
                       id_laboratorio,
                       estado,
                       observaciones
                FROM dbo.Equipo
                WHERE codigo_inventario = ?
                """;

        try (Connection conn =
                     DatabaseConfig.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(
                    1,
                    codigoInventario
            );

            try (ResultSet rs =
                         stmt.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(
                            mapearEquipo(rs)
                    );
                }
            }
        }

        return Optional.empty();
    }


    // Guarda un equipo nuevo.
    public Equipo guardar(
            Equipo equipo
    ) throws SQLException {

        String sql = """
                INSERT INTO dbo.Equipo
                (codigo_inventario,
                 nombre,
                 id_tipo_equipo,
                 marca,
                 modelo,
                 numero_serie,
                 id_laboratorio,
                 estado,
                 observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn =
                     DatabaseConfig.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            stmt.setString(
                    1,
                    equipo.getCodigoInventario()
            );

            stmt.setString(
                    2,
                    equipo.getNombre()
            );

            stmt.setInt(
                    3,
                    equipo.getIdTipoEquipo()
            );

            stmt.setString(
                    4,
                    equipo.getMarca()
            );

            stmt.setString(
                    5,
                    equipo.getModelo()
            );

            stmt.setString(
                    6,
                    equipo.getNumeroSerie()
            );

            stmt.setInt(
                    7,
                    equipo.getIdLaboratorio()
            );

            stmt.setString(
                    8,
                    equipo.getEstado().name()
            );

            stmt.setString(
                    9,
                    equipo.getObservaciones()
            );


            int filas =
                    stmt.executeUpdate();


            if (filas == 0) {
                throw new SQLException(
                        "No fue posible registrar el equipo."
                );
            }


            try (ResultSet keys =
                         stmt.getGeneratedKeys()) {

                if (keys.next()) {
                    equipo.setIdEquipo(
                            keys.getInt(1)
                    );
                }
            }


            return equipo;
        }
    }


    // Nombre oficial acordado por el equipo.
    public List<Equipo> listar()
            throws SQLException {

        return listarTodos();
    }


    // Se conserva para no romper código existente.
    public List<Equipo> listarTodos()
            throws SQLException {

        String sql = """
                SELECT id_equipo,
                       codigo_inventario,
                       nombre,
                       id_tipo_equipo,
                       marca,
                       modelo,
                       numero_serie,
                       id_laboratorio,
                       estado,
                       observaciones
                FROM dbo.Equipo
                ORDER BY id_equipo ASC
                """;

        return ejecutarListado(sql);
    }


    // Equipos que pueden ser utilizados por préstamo/reserva.
    public List<Equipo> listarDisponibles()
            throws SQLException {

        String sql = """
                SELECT id_equipo,
                       codigo_inventario,
                       nombre,
                       id_tipo_equipo,
                       marca,
                       modelo,
                       numero_serie,
                       id_laboratorio,
                       estado,
                       observaciones
                FROM dbo.Equipo
                WHERE estado = 'DISPONIBLE'
                ORDER BY codigo_inventario
                """;

        return ejecutarListado(sql);
    }


    // Actualiza datos generales del equipo.
    public void actualizar(
            Equipo equipo
    ) throws SQLException {

        String sql = """
                UPDATE dbo.Equipo
                SET codigo_inventario = ?,
                    nombre = ?,
                    id_tipo_equipo = ?,
                    marca = ?,
                    modelo = ?,
                    numero_serie = ?,
                    id_laboratorio = ?,
                    estado = ?,
                    observaciones = ?
                WHERE id_equipo = ?
                """;

        try (Connection conn =
                     DatabaseConfig.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(
                    1,
                    equipo.getCodigoInventario()
            );

            stmt.setString(
                    2,
                    equipo.getNombre()
            );

            stmt.setInt(
                    3,
                    equipo.getIdTipoEquipo()
            );

            stmt.setString(
                    4,
                    equipo.getMarca()
            );

            stmt.setString(
                    5,
                    equipo.getModelo()
            );

            stmt.setString(
                    6,
                    equipo.getNumeroSerie()
            );

            stmt.setInt(
                    7,
                    equipo.getIdLaboratorio()
            );

            stmt.setString(
                    8,
                    equipo.getEstado().name()
            );

            stmt.setString(
                    9,
                    equipo.getObservaciones()
            );

            stmt.setInt(
                    10,
                    equipo.getIdEquipo()
            );


            int filas =
                    stmt.executeUpdate();


            if (filas == 0) {
                throw new SQLException(
                        "El equipo no existe."
                );
            }
        }
    }


    // Versión sencilla para llamadas fuera de una transacción.
    public void actualizarEstado(
            int idEquipo,
            EstadoEquipo nuevoEstado
    ) throws SQLException {

        try (Connection conn =
                     DatabaseConfig.getConnection()) {

            actualizarEstado(
                    idEquipo,
                    nuevoEstado,
                    conn
            );
        }
    }


    // Versión transaccional utilizada por otros módulos.
    public void actualizarEstado(
            int idEquipo,
            EstadoEquipo nuevoEstado,
            Connection conn
    ) throws SQLException {

        String sql = """
                UPDATE dbo.Equipo
                SET estado = ?
                WHERE id_equipo = ?
                """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(
                    1,
                    nuevoEstado.name()
            );

            stmt.setInt(
                    2,
                    idEquipo
            );

            int filas =
                    stmt.executeUpdate();


            if (filas == 0) {
                throw new SQLException(
                        "El equipo no existe."
                );
            }
        }
    }


    // Elimina un equipo cuando la BD permite hacerlo.
    public void eliminar(
            int idEquipo
    ) throws SQLException {

        String sql = """
                DELETE FROM dbo.Equipo
                WHERE id_equipo = ?
                """;

        try (Connection conn =
                     DatabaseConfig.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idEquipo
            );


            int filas =
                    stmt.executeUpdate();


            if (filas == 0) {
                throw new SQLException(
                        "El equipo no existe."
                );
            }
        }
    }


    // Método auxiliar para consultas que devuelven varios equipos.
    private List<Equipo> ejecutarListado(
            String sql
    ) throws SQLException {

        List<Equipo> lista =
                new ArrayList<>();


        try (Connection conn =
                     DatabaseConfig.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs =
                     stmt.executeQuery()) {

            while (rs.next()) {

                lista.add(
                        mapearEquipo(rs)
                );
            }
        }


        return lista;
    }


    // Convierte una fila SQL en un objeto Equipo.
    private Equipo mapearEquipo(
            ResultSet rs
    ) throws SQLException {

        Equipo equipo =
                new Equipo();


        equipo.setIdEquipo(
                rs.getInt(
                        "id_equipo"
                )
        );


        equipo.setCodigoInventario(
                rs.getString(
                        "codigo_inventario"
                )
        );


        equipo.setNombre(
                rs.getString(
                        "nombre"
                )
        );


        equipo.setIdTipoEquipo(
                rs.getInt(
                        "id_tipo_equipo"
                )
        );


        equipo.setMarca(
                rs.getString(
                        "marca"
                )
        );


        equipo.setModelo(
                rs.getString(
                        "modelo"
                )
        );


        equipo.setNumeroSerie(
                rs.getString(
                        "numero_serie"
                )
        );


        equipo.setIdLaboratorio(
                rs.getInt(
                        "id_laboratorio"
                )
        );


        String estadoStr =
                rs.getString(
                        "estado"
                );


        if (estadoStr != null) {

            equipo.setEstado(
                    EstadoEquipo.valueOf(
                            estadoStr
                    )
            );
        }


        equipo.setObservaciones(
                rs.getString(
                        "observaciones"
                )
        );


        return equipo;
    }
}
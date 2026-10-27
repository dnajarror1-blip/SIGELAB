package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.enums.EstadoLaboratorio;
import sigelab.model.Laboratorio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LaboratorioRepository {

    // Guarda un nuevo laboratorio.
    public Laboratorio guardar(Laboratorio laboratorio) {

        final String sql = """
                INSERT INTO dbo.Laboratorio
                (codigo, nombre, ubicacion, capacidad, estado)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, laboratorio.getCodigo());
            statement.setString(2, laboratorio.getNombre());
            statement.setString(3, laboratorio.getUbicacion());
            statement.setInt(4, laboratorio.getCapacidad());
            statement.setString(5, laboratorio.getEstado().name());

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "No fue posible registrar el laboratorio."
                );
            }

            // Recupera el ID generado por SQL Server.
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    laboratorio.setIdLaboratorio(
                            generatedKeys.getInt(1)
                    );
                }
            }

            return laboratorio;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al guardar el laboratorio.",
                    e
            );
        }
    }


    // Devuelve todos los laboratorios.
    public List<Laboratorio> listar() {

        final String sql = """
                SELECT id_laboratorio,
                       codigo,
                       nombre,
                       ubicacion,
                       capacidad,
                       estado
                FROM dbo.Laboratorio
                ORDER BY codigo
                """;

        List<Laboratorio> laboratorios =
                new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                laboratorios.add(
                        mapear(resultSet)
                );
            }

            return laboratorios;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al consultar los laboratorios.",
                    e
            );
        }
    }


    // Devuelve solamente laboratorios activos.
    public List<Laboratorio> listarActivos() {

        final String sql = """
                SELECT id_laboratorio,
                       codigo,
                       nombre,
                       ubicacion,
                       capacidad,
                       estado
                FROM dbo.Laboratorio
                WHERE estado = 'ACTIVO'
                ORDER BY codigo
                """;

        List<Laboratorio> laboratorios =
                new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                laboratorios.add(
                        mapear(resultSet)
                );
            }

            return laboratorios;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al consultar los laboratorios activos.",
                    e
            );
        }
    }


    // Busca un laboratorio por ID.
    public Optional<Laboratorio> buscarPorId(
            int idLaboratorio
    ) {

        final String sql = """
                SELECT id_laboratorio,
                       codigo,
                       nombre,
                       ubicacion,
                       capacidad,
                       estado
                FROM dbo.Laboratorio
                WHERE id_laboratorio = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    idLaboratorio
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapear(resultSet)
                    );
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al buscar el laboratorio.",
                    e
            );
        }
    }


    // Busca un laboratorio por su código.
    public Optional<Laboratorio> buscarPorCodigo(
            String codigo
    ) {

        final String sql = """
                SELECT id_laboratorio,
                       codigo,
                       nombre,
                       ubicacion,
                       capacidad,
                       estado
                FROM dbo.Laboratorio
                WHERE codigo = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    codigo
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapear(resultSet)
                    );
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al buscar el laboratorio por código.",
                    e
            );
        }
    }


    // Actualiza un laboratorio existente.
    public void actualizar(
            Laboratorio laboratorio
    ) {

        final String sql = """
                UPDATE dbo.Laboratorio
                SET codigo = ?,
                    nombre = ?,
                    ubicacion = ?,
                    capacidad = ?,
                    estado = ?
                WHERE id_laboratorio = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    laboratorio.getCodigo()
            );

            statement.setString(
                    2,
                    laboratorio.getNombre()
            );

            statement.setString(
                    3,
                    laboratorio.getUbicacion()
            );

            statement.setInt(
                    4,
                    laboratorio.getCapacidad()
            );

            statement.setString(
                    5,
                    laboratorio.getEstado().name()
            );

            statement.setInt(
                    6,
                    laboratorio.getIdLaboratorio()
            );

            int filasAfectadas =
                    statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "El laboratorio que intenta actualizar no existe."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al actualizar el laboratorio.",
                    e
            );
        }
    }


    // Elimina un laboratorio por ID.
    public void eliminar(
            int idLaboratorio
    ) {

        final String sql = """
                DELETE FROM dbo.Laboratorio
                WHERE id_laboratorio = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    idLaboratorio
            );

            int filasAfectadas =
                    statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "El laboratorio que intenta eliminar no existe."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No fue posible eliminar el laboratorio. "
                            + "Verifique que no tenga equipos asociados.",
                    e
            );
        }
    }


    // Convierte una fila de SQL en un objeto Laboratorio.
    private Laboratorio mapear(
            ResultSet resultSet
    ) throws SQLException {

        Laboratorio laboratorio =
                new Laboratorio();

        laboratorio.setIdLaboratorio(
                resultSet.getInt(
                        "id_laboratorio"
                )
        );

        laboratorio.setCodigo(
                resultSet.getString(
                        "codigo"
                )
        );

        laboratorio.setNombre(
                resultSet.getString(
                        "nombre"
                )
        );

        laboratorio.setUbicacion(
                resultSet.getString(
                        "ubicacion"
                )
        );

        laboratorio.setCapacidad(
                resultSet.getInt(
                        "capacidad"
                )
        );

        laboratorio.setEstado(
                EstadoLaboratorio.valueOf(
                        resultSet.getString(
                                "estado"
                        )
                )
        );

        return laboratorio;
    }
}
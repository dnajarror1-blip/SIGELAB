package sigelab.repository;

import sigelab.config.DatabaseConfig;
import sigelab.model.TipoEquipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TipoEquipoRepository {

    // Guarda un nuevo tipo de equipo en la base de datos.
    public TipoEquipo guardar(TipoEquipo tipoEquipo) {

        final String sql = """
                INSERT INTO dbo.TipoEquipo (nombre, descripcion)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, tipoEquipo.getNombre());
            statement.setString(2, tipoEquipo.getDescripcion());

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "No fue posible registrar el tipo de equipo."
                );
            }

            // Recupera el ID generado automáticamente por SQL Server.
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tipoEquipo.setIdTipoEquipo(
                            generatedKeys.getInt(1)
                    );
                }
            }

            return tipoEquipo;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al guardar el tipo de equipo.",
                    e
            );
        }
    }


    // Devuelve todos los tipos de equipo registrados.
    public List<TipoEquipo> listar() {

        final String sql = """
                SELECT id_tipo_equipo, nombre, descripcion
                FROM dbo.TipoEquipo
                ORDER BY nombre
                """;

        List<TipoEquipo> tipos = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tipos.add(mapear(resultSet));
            }

            return tipos;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al consultar los tipos de equipo.",
                    e
            );
        }
    }


    // Busca un tipo de equipo por su ID.
    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo) {

        final String sql = """
                SELECT id_tipo_equipo, nombre, descripcion
                FROM dbo.TipoEquipo
                WHERE id_tipo_equipo = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idTipoEquipo);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapear(resultSet)
                    );
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al buscar el tipo de equipo.",
                    e
            );
        }
    }


    // Busca un tipo de equipo por su nombre.
    public Optional<TipoEquipo> buscarPorNombre(String nombre) {

        final String sql = """
                SELECT id_tipo_equipo, nombre, descripcion
                FROM dbo.TipoEquipo
                WHERE nombre = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nombre);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapear(resultSet)
                    );
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al buscar el tipo de equipo por nombre.",
                    e
            );
        }
    }


    // Actualiza los datos de un tipo de equipo.
    public void actualizar(TipoEquipo tipoEquipo) {

        final String sql = """
                UPDATE dbo.TipoEquipo
                SET nombre = ?,
                    descripcion = ?
                WHERE id_tipo_equipo = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tipoEquipo.getNombre());
            statement.setString(2, tipoEquipo.getDescripcion());
            statement.setInt(
                    3,
                    tipoEquipo.getIdTipoEquipo()
            );

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "El tipo de equipo que intenta actualizar no existe."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al actualizar el tipo de equipo.",
                    e
            );
        }
    }


    // Elimina un tipo de equipo por su ID.
    public void eliminar(int idTipoEquipo) {

        final String sql = """
                DELETE FROM dbo.TipoEquipo
                WHERE id_tipo_equipo = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idTipoEquipo);

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                throw new IllegalStateException(
                        "El tipo de equipo que intenta eliminar no existe."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al eliminar el tipo de equipo.",
                    e
            );
        }
    }


    // Convierte una fila de la base de datos en un objeto TipoEquipo.
    private TipoEquipo mapear(ResultSet resultSet) throws SQLException {

        TipoEquipo tipoEquipo = new TipoEquipo();

        tipoEquipo.setIdTipoEquipo(
                resultSet.getInt("id_tipo_equipo")
        );

        tipoEquipo.setNombre(
                resultSet.getString("nombre")
        );

        tipoEquipo.setDescripcion(
                resultSet.getString("descripcion")
        );

        return tipoEquipo;
    }
}

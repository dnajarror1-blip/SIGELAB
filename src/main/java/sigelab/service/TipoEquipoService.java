package sigelab.service;

import sigelab.model.TipoEquipo;
import sigelab.repository.TipoEquipoRepository;

import java.util.List;
import java.util.Optional;

public class TipoEquipoService {

    private final TipoEquipoRepository repository;

    public TipoEquipoService() {
        this.repository = new TipoEquipoRepository();
    }

    // Registra un nuevo tipo de equipo.
    public TipoEquipo crear(String nombre, String descripcion) {

        String nombreLimpio = limpiar(nombre);
        String descripcionLimpia = limpiarOpcional(descripcion);

        validarNombre(nombreLimpio);

        if (repository.buscarPorNombre(nombreLimpio).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un tipo de equipo con ese nombre."
            );
        }

        TipoEquipo tipoEquipo = new TipoEquipo(
                nombreLimpio,
                descripcionLimpia
        );

        return repository.guardar(tipoEquipo);
    }

    // Devuelve todos los tipos de equipo.
    public List<TipoEquipo> listar() {
        return repository.listar();
    }

    // Busca un tipo de equipo por ID.
    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo) {

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "El ID del tipo de equipo no es válido."
            );
        }

        return repository.buscarPorId(idTipoEquipo);
    }

    // Actualiza un tipo de equipo existente.
    public void actualizar(
            int idTipoEquipo,
            String nombre,
            String descripcion
    ) {

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "El ID del tipo de equipo no es válido."
            );
        }

        String nombreLimpio = limpiar(nombre);
        String descripcionLimpia = limpiarOpcional(descripcion);

        validarNombre(nombreLimpio);

        TipoEquipo actual = repository.buscarPorId(idTipoEquipo)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El tipo de equipo no existe."
                        )
                );

        repository.buscarPorNombre(nombreLimpio)
                .filter(tipo ->
                        !tipo.getIdTipoEquipo().equals(idTipoEquipo)
                )
                .ifPresent(tipo -> {
                    throw new IllegalArgumentException(
                            "Ya existe un tipo de equipo con ese nombre."
                    );
                });

        actual.setNombre(nombreLimpio);
        actual.setDescripcion(descripcionLimpia);

        repository.actualizar(actual);
    }

    // Elimina un tipo de equipo.
    public void eliminar(int idTipoEquipo) {

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "El ID del tipo de equipo no es válido."
            );
        }

        if (repository.buscarPorId(idTipoEquipo).isEmpty()) {
            throw new IllegalArgumentException(
                    "El tipo de equipo no existe."
            );
        }

        repository.eliminar(idTipoEquipo);
    }

    // Valida que el nombre sea obligatorio.
    private void validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del tipo de equipo es obligatorio."
            );
        }

        if (nombre.length() > 60) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 60 caracteres."
            );
        }
    }

    // Limpia espacios al inicio y final.
    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    // La descripción es opcional.
    private String limpiarOpcional(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        if (limpio.length() > 200) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar los 200 caracteres."
            );
        }

        return limpio;
    }
}
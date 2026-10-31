package sigelab.service;

import sigelab.enums.EstadoLaboratorio;
import sigelab.model.Laboratorio;
import sigelab.repository.LaboratorioRepository;

import java.util.List;
import java.util.Optional;

public class LaboratorioService {

    private final LaboratorioRepository repository;

    public LaboratorioService() {
        this.repository =
                new LaboratorioRepository();
    }


    // Registra un nuevo laboratorio.
    public Laboratorio crear(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        String codigoLimpio =
                limpiar(codigo);

        String nombreLimpio =
                limpiar(nombre);

        String ubicacionLimpia =
                limpiar(ubicacion);


        validarDatos(
                codigoLimpio,
                nombreLimpio,
                ubicacionLimpia,
                capacidad,
                estado
        );


        // RN-03: el código debe ser único.
        if (repository
                .buscarPorCodigo(codigoLimpio)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ya existe un laboratorio con ese código."
            );
        }


        Laboratorio laboratorio =
                new Laboratorio();

        laboratorio.setCodigo(
                codigoLimpio
        );

        laboratorio.setNombre(
                nombreLimpio
        );

        laboratorio.setUbicacion(
                ubicacionLimpia
        );

        laboratorio.setCapacidad(
                capacidad
        );

        laboratorio.setEstado(
                estado
        );


        return repository.guardar(
                laboratorio
        );
    }


    // Devuelve todos los laboratorios.
    public List<Laboratorio> listar() {
        return repository.listar();
    }


    // Devuelve los laboratorios activos.
    public List<Laboratorio> listarActivos() {
        return repository.listarActivos();
    }


    // Busca un laboratorio por ID.
    public Optional<Laboratorio> buscarPorId(
            int idLaboratorio
    ) {

        validarId(
                idLaboratorio
        );

        return repository.buscarPorId(
                idLaboratorio
        );
    }


    // Busca un laboratorio por código.
    public Optional<Laboratorio> buscarPorCodigo(
            String codigo
    ) {

        String codigoLimpio =
                limpiar(codigo);

        if (codigoLimpio == null
                || codigoLimpio.isBlank()) {

            throw new IllegalArgumentException(
                    "El código es obligatorio."
            );
        }

        return repository.buscarPorCodigo(
                codigoLimpio
        );
    }


    // Actualiza un laboratorio existente.
    public void actualizar(
            int idLaboratorio,
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        validarId(
                idLaboratorio
        );


        String codigoLimpio =
                limpiar(codigo);

        String nombreLimpio =
                limpiar(nombre);

        String ubicacionLimpia =
                limpiar(ubicacion);


        validarDatos(
                codigoLimpio,
                nombreLimpio,
                ubicacionLimpia,
                capacidad,
                estado
        );


        Laboratorio actual =
                repository.buscarPorId(
                        idLaboratorio
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "El laboratorio no existe."
                        )
                );


        // Evita que otro laboratorio tenga el mismo código.
        repository
                .buscarPorCodigo(codigoLimpio)
                .filter(laboratorio ->
                        !laboratorio
                                .getIdLaboratorio()
                                .equals(idLaboratorio)
                )
                .ifPresent(laboratorio -> {
                    throw new IllegalArgumentException(
                            "Ya existe un laboratorio con ese código."
                    );
                });


        actual.setCodigo(
                codigoLimpio
        );

        actual.setNombre(
                nombreLimpio
        );

        actual.setUbicacion(
                ubicacionLimpia
        );

        actual.setCapacidad(
                capacidad
        );

        actual.setEstado(
                estado
        );


        repository.actualizar(
                actual
        );
    }


    // Elimina un laboratorio.
    public void eliminar(
            int idLaboratorio
    ) {

        validarId(
                idLaboratorio
        );


        if (repository
                .buscarPorId(idLaboratorio)
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El laboratorio no existe."
            );
        }


        repository.eliminar(
                idLaboratorio
        );
    }


    // Valida los datos principales.
    private void validarDatos(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        if (codigo == null
                || codigo.isBlank()) {

            throw new IllegalArgumentException(
                    "El código del laboratorio es obligatorio."
            );
        }


        if (codigo.length() > 20) {

            throw new IllegalArgumentException(
                    "El código no puede superar los 20 caracteres."
            );
        }


        if (nombre == null
                || nombre.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del laboratorio es obligatorio."
            );
        }


        if (nombre.length() > 80) {

            throw new IllegalArgumentException(
                    "El nombre no puede superar los 80 caracteres."
            );
        }


        if (ubicacion == null
                || ubicacion.isBlank()) {

            throw new IllegalArgumentException(
                    "La ubicación del laboratorio es obligatoria."
            );
        }


        if (ubicacion.length() > 120) {

            throw new IllegalArgumentException(
                    "La ubicación no puede superar los 120 caracteres."
            );
        }


        if (capacidad <= 0) {

            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero."
            );
        }


        if (estado == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un estado."
            );
        }
    }


    // Valida el ID.
    private void validarId(
            int idLaboratorio
    ) {

        if (idLaboratorio <= 0) {

            throw new IllegalArgumentException(
                    "El ID del laboratorio no es válido."
            );
        }
    }


    // Quita espacios innecesarios.
    private String limpiar(
            String valor
    ) {

        return valor == null
                ? null
                : valor.trim();
    }
}

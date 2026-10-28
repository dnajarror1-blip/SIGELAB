package sigelab.service;

import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;
import sigelab.repository.EquipoRepository;
import sigelab.repository.LaboratorioRepository;
import sigelab.repository.TipoEquipoRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final LaboratorioRepository laboratorioRepository;

    public EquipoService() {
        this.equipoRepository = new EquipoRepository();
        this.tipoEquipoRepository = new TipoEquipoRepository();
        this.laboratorioRepository = new LaboratorioRepository();
    }


    // Registra un equipo nuevo.
    public Equipo crear(
            String codigoInventario,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String numeroSerie,
            int idLaboratorio,
            String observaciones
    ) {

        String codigoLimpio = limpiar(codigoInventario);
        String nombreLimpio = limpiar(nombre);
        String marcaLimpia = limpiar(marca);
        String modeloLimpio = limpiar(modelo);
        String serieLimpia = limpiar(numeroSerie);
        String observacionesLimpias = limpiarOpcional(observaciones);

        validarDatos(
                codigoLimpio,
                nombreLimpio,
                idTipoEquipo,
                marcaLimpia,
                modeloLimpio,
                serieLimpia,
                idLaboratorio,
                observacionesLimpias
        );

        validarRelaciones(
                idTipoEquipo,
                idLaboratorio
        );

        try {

            // RN-04: código de inventario único.
            if (equipoRepository
                    .buscarPorCodigoInventario(codigoLimpio)
                    .isPresent()) {

                throw new IllegalArgumentException(
                        "Ya existe un equipo con ese código de inventario."
                );
            }

            // Todo equipo nuevo inicia DISPONIBLE.
            Equipo equipo = new Equipo(
                    codigoLimpio,
                    nombreLimpio,
                    idTipoEquipo,
                    marcaLimpia,
                    modeloLimpio,
                    serieLimpia,
                    idLaboratorio,
                    EstadoEquipo.DISPONIBLE,
                    observacionesLimpias
            );

            return equipoRepository.guardar(equipo);

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible registrar el equipo.",
                    e
            );
        }
    }


    // Devuelve todos los equipos.
    public List<Equipo> listar() {

        try {
            return equipoRepository.listar();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible consultar los equipos.",
                    e
            );
        }
    }


    // Devuelve solamente equipos disponibles.
    public List<Equipo> listarDisponibles() {

        try {
            return equipoRepository.listarDisponibles();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible consultar los equipos disponibles.",
                    e
            );
        }
    }


    // Busca un equipo por ID.
    public Optional<Equipo> buscarPorId(
            int idEquipo
    ) {

        validarId(idEquipo);

        try {
            return equipoRepository.buscarPorId(idEquipo);

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible buscar el equipo.",
                    e
            );
        }
    }


    // Busca un equipo por código de inventario.
    public Optional<Equipo> buscarPorCodigoInventario(
            String codigoInventario
    ) {

        String codigoLimpio =
                limpiar(codigoInventario);

        if (codigoLimpio == null
                || codigoLimpio.isBlank()) {

            throw new IllegalArgumentException(
                    "El código de inventario es obligatorio."
            );
        }

        try {

            return equipoRepository
                    .buscarPorCodigoInventario(
                            codigoLimpio
                    );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible buscar el equipo.",
                    e
            );
        }
    }


    // Actualiza los datos del equipo sin cambiar su estado.
    public void actualizar(
            int idEquipo,
            String codigoInventario,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String numeroSerie,
            int idLaboratorio,
            String observaciones
    ) {

        validarId(idEquipo);

        String codigoLimpio = limpiar(codigoInventario);
        String nombreLimpio = limpiar(nombre);
        String marcaLimpia = limpiar(marca);
        String modeloLimpio = limpiar(modelo);
        String serieLimpia = limpiar(numeroSerie);
        String observacionesLimpias = limpiarOpcional(observaciones);

        validarDatos(
                codigoLimpio,
                nombreLimpio,
                idTipoEquipo,
                marcaLimpia,
                modeloLimpio,
                serieLimpia,
                idLaboratorio,
                observacionesLimpias
        );

        validarRelaciones(
                idTipoEquipo,
                idLaboratorio
        );

        try {

            Equipo actual =
                    equipoRepository
                            .buscarPorId(idEquipo)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El equipo no existe."
                                    )
                            );


            // Evita usar el código de otro equipo.
            equipoRepository
                    .buscarPorCodigoInventario(
                            codigoLimpio
                    )
                    .filter(equipo ->
                            !equipo.getIdEquipo()
                                    .equals(idEquipo)
                    )
                    .ifPresent(equipo -> {

                        throw new IllegalArgumentException(
                                "Ya existe un equipo con ese código de inventario."
                        );
                    });


            actual.setCodigoInventario(
                    codigoLimpio
            );

            actual.setNombre(
                    nombreLimpio
            );

            actual.setIdTipoEquipo(
                    idTipoEquipo
            );

            actual.setMarca(
                    marcaLimpia
            );

            actual.setModelo(
                    modeloLimpio
            );

            actual.setNumeroSerie(
                    serieLimpia
            );

            actual.setIdLaboratorio(
                    idLaboratorio
            );

            actual.setObservaciones(
                    observacionesLimpias
            );

            // No modificamos el estado desde el CRUD.
            equipoRepository.actualizar(
                    actual
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible actualizar el equipo.",
                    e
            );
        }
    }


    // Cambia el estado únicamente si la transición es válida.
    public void actualizarEstado(
            int idEquipo,
            EstadoEquipo nuevoEstado
    ) {

        validarId(idEquipo);

        if (nuevoEstado == null) {

            throw new IllegalArgumentException(
                    "El nuevo estado es obligatorio."
            );
        }

        try {

            Equipo equipo =
                    equipoRepository
                            .buscarPorId(idEquipo)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El equipo no existe."
                                    )
                            );


            EstadoEquipo estadoActual =
                    equipo.getEstado();


            if (estadoActual == nuevoEstado) {
                return;
            }


            if (!esTransicionPermitida(
                    estadoActual,
                    nuevoEstado
            )) {

                throw new IllegalStateException(
                        "No se permite cambiar el equipo de "
                                + estadoActual
                                + " a "
                                + nuevoEstado
                                + "."
                );
            }


            equipoRepository.actualizarEstado(
                    idEquipo,
                    nuevoEstado
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible actualizar el estado del equipo.",
                    e
            );
        }
    }


    // Elimina solamente equipos disponibles y sin historial.
    public void eliminar(
            int idEquipo
    ) {

        validarId(idEquipo);

        try {

            Equipo equipo =
                    equipoRepository
                            .buscarPorId(idEquipo)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El equipo no existe."
                                    )
                            );


            if (equipo.getEstado()
                    != EstadoEquipo.DISPONIBLE) {

                throw new IllegalStateException(
                        "Solo puede eliminarse un equipo DISPONIBLE."
                );
            }


            equipoRepository.eliminar(
                    idEquipo
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No fue posible eliminar el equipo. "
                            + "Verifique que no tenga historial asociado.",
                    e
            );
        }
    }


    // Máquina de estados del equipo.
    private boolean esTransicionPermitida(
            EstadoEquipo actual,
            EstadoEquipo nuevo
    ) {

        return switch (actual) {

            case DISPONIBLE ->
                    nuevo == EstadoEquipo.PRESTADO
                            || nuevo == EstadoEquipo.MANTENIMIENTO;

            case PRESTADO ->
                    nuevo == EstadoEquipo.DISPONIBLE
                            || nuevo == EstadoEquipo.DANADO;

            case DANADO ->
                    nuevo == EstadoEquipo.MANTENIMIENTO;

            case MANTENIMIENTO ->
                    nuevo == EstadoEquipo.DISPONIBLE
                            || nuevo == EstadoEquipo.BAJA;

            case BAJA ->
                    false;
        };
    }


    // Comprueba las relaciones con catálogos existentes.
    private void validarRelaciones(
            int idTipoEquipo,
            int idLaboratorio
    ) {

        if (tipoEquipoRepository
                .buscarPorId(idTipoEquipo)
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El tipo de equipo seleccionado no existe."
            );
        }


        if (laboratorioRepository
                .buscarPorId(idLaboratorio)
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El laboratorio seleccionado no existe."
            );
        }
    }


    // Valida campos obligatorios y longitudes.
    private void validarDatos(
            String codigoInventario,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String numeroSerie,
            int idLaboratorio,
            String observaciones
    ) {

        validarTexto(
                codigoInventario,
                "El código de inventario es obligatorio.",
                30
        );

        validarTexto(
                nombre,
                "El nombre del equipo es obligatorio.",
                100
        );


        if (idTipoEquipo <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un tipo de equipo."
            );
        }


        validarTexto(
                marca,
                "La marca es obligatoria.",
                60
        );

        validarTexto(
                modelo,
                "El modelo es obligatorio.",
                60
        );

        validarTexto(
                numeroSerie,
                "El número de serie es obligatorio.",
                80
        );


        if (idLaboratorio <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un laboratorio."
            );
        }


        if (observaciones != null
                && observaciones.length() > 250) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar los 250 caracteres."
            );
        }
    }


    private void validarTexto(
            String valor,
            String mensaje,
            int longitudMaxima
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensaje
            );
        }


        if (valor.length() > longitudMaxima) {

            throw new IllegalArgumentException(
                    "El campo supera el máximo de "
                            + longitudMaxima
                            + " caracteres."
            );
        }
    }


    private void validarId(
            int idEquipo
    ) {

        if (idEquipo <= 0) {

            throw new IllegalArgumentException(
                    "El ID del equipo no es válido."
            );
        }
    }


    // Quita espacios al inicio y final.
    private String limpiar(
            String valor
    ) {

        return valor == null
                ? null
                : valor.trim();
    }


    // Las observaciones son opcionales.
    private String limpiarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.trim();
    }
}
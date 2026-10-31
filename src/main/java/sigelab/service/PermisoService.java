package sigelab.service;

import sigelab.model.Rol;
import sigelab.repository.RolRepository;

import java.util.EnumSet;
import java.util.Set;

public class PermisoService {

    private final RolRepository rolRepository;

    public PermisoService() {
        this(new RolRepository());
    }

    public PermisoService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    /** Revalida identidad activa y permisos dentro de la conexión del servicio llamante. */
    public void validarAcceso(java.sql.Connection conn, int idUsuario, Modulo modulo)
            throws java.sql.SQLException {
        if (idUsuario <= 0) throw new sigelab.exception.BusinessException("Debe iniciar sesión.");
        Rol rol = rolRepository.buscarDeUsuarioActivo(conn, idUsuario)
                .orElseThrow(() -> new sigelab.exception.BusinessException(
                        "El usuario no existe o está inactivo."));
        if (!modulosDe(rol).contains(modulo)) {
            throw new sigelab.exception.BusinessException("No tiene permiso para acceder a " + modulo + ".");
        }
    }

    // Dice si un rol puede entrar a una opción del menú
    public boolean puedeAcceder(int idRol, Modulo modulo) {
        return modulosDe(idRol).contains(modulo);
    }

    // Devuelve todas las opciones que le tocan a un rol
    // (el menú se arma con esta lista)
    public Set<Modulo> modulosDe(int idRol) {
        return rolRepository.buscarPorId(idRol).map(this::modulosDe)
                .orElseGet(() -> EnumSet.noneOf(Modulo.class));
    }

    public Set<Modulo> modulosDe(Rol rol) {
        String nombre = rol == null ? null : rol.getNombre();

        // El admin puede entrar a todo
        if ("ADMIN".equals(nombre)) {
            return EnumSet.allOf(Modulo.class);
        }

        // El encargado maneja el trabajo diario, pero no usuarios,
        // enrollment ni auditoría
        if ("ENCARGADO".equals(nombre)) {
            return EnumSet.of(
                    Modulo.LABORATORIOS_EQUIPOS,
                    Modulo.RESERVAS_PRESTAMOS,
                    Modulo.MANTENIMIENTO,
                    Modulo.REPORTES_QR,
                    Modulo.NOTIFICACIONES);
        }

        if ("ESTUDIANTE".equals(nombre)) {
            return EnumSet.of(Modulo.CONSULTA_LABORATORIOS, Modulo.CONSULTA_EQUIPOS);
        }

        // Cualquier otro rol que no conocemos no ve nada
        return EnumSet.noneOf(Modulo.class);
    }
}

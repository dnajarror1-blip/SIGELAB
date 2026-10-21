package sigelab.service;

import java.util.EnumSet;
import java.util.Set;

public class PermisoService {

    // Los ids vienen del seed de la base: 1 = ADMIN, 2 = ENCARGADO
    private static final int ID_ADMIN = 1;
    private static final int ID_ENCARGADO = 2;

    // Dice si un rol puede entrar a una opción del menú
    public boolean puedeAcceder(int idRol, Modulo modulo) {
        return modulosDe(idRol).contains(modulo);
    }

    // Devuelve todas las opciones que le tocan a un rol
    // (el menú se arma con esta lista)
    public Set<Modulo> modulosDe(int idRol) {

        // El admin puede entrar a todo
        if (idRol == ID_ADMIN) {
            return EnumSet.allOf(Modulo.class);
        }

        // El encargado maneja el trabajo diario, pero no usuarios,
        // enrollment ni auditoría
        if (idRol == ID_ENCARGADO) {
            return EnumSet.of(
                    Modulo.LABORATORIOS_EQUIPOS,
                    Modulo.RESERVAS_PRESTAMOS,
                    Modulo.MANTENIMIENTO,
                    Modulo.REPORTES_QR,
                    Modulo.NOTIFICACIONES);
        }

        // Cualquier otro rol que no conocemos no ve nada
        return EnumSet.noneOf(Modulo.class);
    }
}
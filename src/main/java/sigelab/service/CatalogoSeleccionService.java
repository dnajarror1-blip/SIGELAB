package sigelab.service;

import sigelab.enums.EstadoUsuario;
import sigelab.model.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Consultas para selectores: reutiliza servicios, sin SQL ni operaciones de escritura. */
public class CatalogoSeleccionService {
    public record Solicitante(int idUsuario, String nombre) {
        @Override public String toString() { return nombre; }
    }
    public record EquipoOpcion(Equipo equipo, String tipo, String laboratorio) {
        @Override public String toString() { return equipo.getCodigoInventario() + " · " + equipo.getNombre(); }
        public String busqueda() { return toString() + " " + Objects.toString(equipo.getMarca(), ""); }
    }
    private final EquipoService equipos;
    private final UsuarioService usuarios;
    private final TipoEquipoService tipos;
    private final LaboratorioService laboratorios;

    public CatalogoSeleccionService() {
        this(new EquipoService(), new UsuarioService(), new TipoEquipoService(), new LaboratorioService());
    }
    public CatalogoSeleccionService(EquipoService equipos, UsuarioService usuarios,
                                   TipoEquipoService tipos, LaboratorioService laboratorios) {
        this.equipos = equipos; this.usuarios = usuarios; this.tipos = tipos; this.laboratorios = laboratorios;
    }
    public List<Solicitante> listarSolicitantesActivos() {
        Map<Integer, Rol> roles = usuarios.listarRoles().stream().collect(Collectors.toMap(Rol::getIdRol, Function.identity()));
        return usuarios.listarUsuarios().stream()
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO && roles.containsKey(u.getIdRol()))
                .map(u -> new Solicitante(u.getIdUsuario(), u.getNombre() + " " + u.getApellido() + " · "
                        + roles.get(u.getIdRol()).getNombre().toLowerCase(Locale.ROOT) + " (" + u.getUsername() + ")"))
                .sorted(Comparator.comparing(Solicitante::nombre, String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public Map<Integer, String> nombresUsuarios() {
        return usuarios.listarUsuarios().stream().collect(Collectors.toMap(Usuario::getIdUsuario,
                u -> u.getNombre() + " " + u.getApellido()));
    }
    public List<EquipoOpcion> listarEquipos(boolean soloDisponibles) {
        Map<Integer, String> nombresTipo = tipos.listar().stream().collect(Collectors.toMap(TipoEquipo::getIdTipoEquipo, TipoEquipo::getNombre));
        Map<Integer, String> nombresLab = laboratorios.listar().stream().collect(Collectors.toMap(Laboratorio::getIdLaboratorio, Laboratorio::getNombre));
        return (soloDisponibles ? equipos.listarDisponibles() : equipos.listar()).stream()
                .map(e -> new EquipoOpcion(e, nombresTipo.getOrDefault(e.getIdTipoEquipo(), "Sin tipo"),
                        nombresLab.getOrDefault(e.getIdLaboratorio(), "Sin laboratorio")))
                .sorted(Comparator.comparing(e -> e.equipo().getCodigoInventario(), String.CASE_INSENSITIVE_ORDER)).toList();
    }
}

package sigelab.controller;

import sigelab.exception.BusinessException;
import sigelab.model.Usuario;
import sigelab.service.Modulo;
import sigelab.service.PermisoService;
import sigelab.service.QrEquipoService;

import java.io.File;

public class QrController {
    private final Usuario usuario;
    private final QrEquipoService service;
    private final PermisoService permisos;

    public QrController(Usuario usuario) {
        this(usuario, new QrEquipoService(), new PermisoService());
    }

    public QrController(Usuario usuario, QrEquipoService service, PermisoService permisos) {
        this.usuario = usuario;
        this.service = service;
        this.permisos = permisos;
    }

    public QrEquipoService.Resultado identificar(File imagen) {
        verificarPermiso();
        return service.identificar(imagen);
    }

    public void generar(String codigo, File destino) {
        verificarPermiso();
        service.generar(codigo, destino);
    }

    private void verificarPermiso() {
        if (usuario == null || !permisos.puedeAcceder(usuario.getIdRol(), Modulo.REPORTES_QR)) {
            throw new BusinessException("No tienes permiso para acceder a QR");
        }
    }
}

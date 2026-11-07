package sigelab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import sigelab.enums.EstadoEquipo;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.integration.qr.QrService;
import sigelab.model.Equipo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class QrEquipoServiceTest {
    @TempDir
    Path carpeta;

    @ParameterizedTest
    @EnumSource(EstadoEquipo.class)
    void identificaElEquipoRealSinCambiarSuEstado(EstadoEquipo estado) {
        Equipo equipo = new Equipo();
        equipo.setCodigoInventario("INV-CP-09-10");
        equipo.setNombre("Equipo de prueba");
        equipo.setEstado(estado);
        QrEquipoService service = new QrEquipoService(new QrService(), codigo -> {
            assertEquals(equipo.getCodigoInventario(), codigo);
            return Optional.of(equipo);
        });
        var archivo = carpeta.resolve("equipo.png").toFile();

        service.generar(equipo.getCodigoInventario(), archivo);
        var resultado = service.identificar(archivo);

        assertSame(equipo, resultado.equipo());
        assertEquals(equipo.getCodigoInventario(), resultado.codigoInventario());
        assertEquals(estado == EstadoEquipo.DISPONIBLE, resultado.disponibleParaUso());
        assertEquals(estado == EstadoEquipo.PRESTADO, resultado.bloqueadoPorPrestamo());
        assertEquals(estado, equipo.getEstado());
    }

    @Test
    void noGeneraQrDeUnEquipoInexistente() {
        QrEquipoService service = new QrEquipoService(new QrService(), codigo -> Optional.empty());
        Path archivo = carpeta.resolve("inexistente.png");
        assertThrows(BusinessException.class, () -> service.generar("NO-EXISTE", archivo.toFile()));
        assertFalse(Files.exists(archivo));
    }

    @Test
    void qrDeCodigoDesconocidoSeRechaza() {
        var archivo = carpeta.resolve("desconocido.png").toFile();
        new QrService().generar("DESCONOCIDO", archivo);
        QrEquipoService service = new QrEquipoService(new QrService(), codigo -> Optional.empty());
        assertThrows(BusinessException.class, () -> service.identificar(archivo));
    }

    @Test
    void falloSqlConservaLaCausa() {
        SQLException fallo = new SQLException("Error simulado");
        QrEquipoService service = new QrEquipoService(new QrService(), codigo -> { throw fallo; });
        DataAccessException error = assertThrows(DataAccessException.class,
                () -> service.generar("INV-001", carpeta.resolve("equipo.png").toFile()));
        assertSame(fallo, error.getCause());
    }
}

package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.exception.BusinessException;
import sigelab.model.*;
import sigelab.repository.*;
import java.nio.file.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class EquipoCp03Test {
    @Test void cp03CodigoExistenteSeRechazaSinInsertarNiExponerSql() {
        EquipoService servicio = new EquipoService(new EquipoRepository() {
            public Optional<Equipo> buscarPorCodigoInventario(String codigo) {
                assertEquals("X", codigo); return Optional.of(new Equipo());
            }
            public Equipo guardar(Equipo equipo) { return fail("No debe insertar el duplicado"); }
        }, new TipoEquipoRepository() {
            public Optional<TipoEquipo> buscarPorId(int id) { return Optional.of(new TipoEquipo()); }
        }, new LaboratorioRepository() {
            public Optional<Laboratorio> buscarPorId(int id) { return Optional.of(new Laboratorio()); }
        });
        var error = assertThrows(BusinessException.class,
                () -> servicio.crear("X", "Demo", 42, "Marca", "Modelo", "Serie", 77, ""));
        assertEquals("Ya existe un equipo con ese código de inventario.", error.getMessage());
        assertNull(error.getCause());
    }
    @Test void cp03EsquemaConservaUniqueDelCodigo() throws Exception {
        String sql = Files.readString(Path.of("database/sigelab.sql"));
        assertTrue(sql.matches("(?s).*codigo_inventario\\s+NVARCHAR\\(30\\)\\s+NOT NULL\\s+CONSTRAINT UQ_Equipo_codigo_inventario UNIQUE.*"));
    }
}

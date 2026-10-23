
package sigelab.service;

import org.junit.jupiter.api.Test;
import sigelab.model.Auditoria;
import sigelab.repository.AuditoriaRepository;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTransaccionalTest {

    @Test
    void debePropagarErrorCuandoFallaAuditoriaTransaccional() {

        // Repositorio simulado: no accede a SQL Server.
        AuditoriaRepository repositorioSimulado =
                new AuditoriaRepository() {

                    @Override
                    public void guardar(Auditoria auditoria, Connection conn)
                            throws SQLException {

                        throw new SQLException("Error simulado de auditoria");
                    }
                };

        AuditService servicio = new AuditService(repositorioSimulado);

        // Conexion ficticia; no abre ninguna conexion real.
        Connection conexionSimulada = (Connection) Proxy.newProxyInstance(
                AuditServiceTransaccionalTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    throw new AssertionError(
                            "La conexion simulada no debe utilizarse directamente"
                    );
                }
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> servicio.registrar(
                        1,
                        "INICIAR_MANTENIMIENTO",
                        "EQUIPO",
                        9,
                        "Prueba de auditoria transaccional",
                        conexionSimulada
                )
        );

        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void debeRechazarConexionTransaccionalNula() {

        AuditService servicio = new AuditService();

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrar(
                        1,
                        "FINALIZAR_MANTENIMIENTO",
                        "EQUIPO",
                        9,
                        "Prueba con conexion nula",
                        null
                )
        );
    }
}

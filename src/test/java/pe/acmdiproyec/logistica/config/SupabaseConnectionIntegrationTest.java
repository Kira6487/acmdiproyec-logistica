package pe.acmdiproyec.logistica.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SupabaseConnectionIntegrationTest {

    private static final String DB_URL = "SUPABASE_DB_URL";
    private static final String DB_USER = "SUPABASE_DB_USER";
    private static final String DB_PASSWORD = "SUPABASE_DB_PASSWORD";

    @Test
    void debeConectarYLeerElEsquemaPublicoCuandoExistenVariables() {
        String url = System.getenv(DB_URL);
        String usuario = System.getenv(DB_USER);
        String password = System.getenv(DB_PASSWORD);

        assumeTrue(Stream.of(url, usuario, password)
                        .allMatch(valor -> valor != null && !valor.isBlank()),
                "La integración con Supabase es opcional y requiere las tres variables SUPABASE_DB_*.");

        try (Connection conexion = DriverManager.getConnection(url, usuario, password)) {
            conexion.setReadOnly(true);
            validarConsultaBasica(conexion);
            validarTablasEsperadas(conexion);
            validarLecturaDeProductos(conexion);
        } catch (SQLException exception) {
            throw new AssertionError(
                    "No fue posible completar la validación de solo lectura con Supabase. SQLState="
                            + exception.getSQLState());
        }
    }

    private void validarConsultaBasica(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("select 1")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getInt(1)).isEqualTo(1);
        }
    }

    private void validarTablasEsperadas(Connection conexion) throws SQLException {
        String consulta = """
                select count(*)
                  from information_schema.tables
                 where table_schema = 'public'
                   and table_name in (
                       'roles', 'usuarios', 'categorias', 'unidades_medida', 'productos',
                       'motivos_movimiento', 'inventario', 'movimientos',
                       'movimiento_detalle', 'auditoria'
                   )
                """;

        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(consulta)) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getInt(1)).isEqualTo(10);
        }
    }

    private void validarLecturaDeProductos(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("select count(*) from public.productos")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getLong(1)).isGreaterThanOrEqualTo(0);
        }
    }
}

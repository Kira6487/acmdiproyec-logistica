package pe.acmdiproyec.logistica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogisticaApplicationTest {

    @Test
    void debeUsarElPaqueteBaseEsperado() {
        assertThat(LogisticaApplication.class.getPackageName())
                .isEqualTo("pe.acmdiproyec.logistica");
    }
}

package com.example.banco.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Coherencia de la fecha de nacimiento con CURP y RFC")
class DocumentosIdentidadTest {

    private static final LocalDate FECHA = LocalDate.of(1995, 8, 21);

    @Test
    @DisplayName("La fecha de la CURP (posiciones 5-10) coincide con la de nacimiento")
    void curpCoherente() {
        assertThat(DocumentosIdentidad.fechaCoincideConCurp("LOHF950821MGTPRR08", FECHA)).isTrue();
        assertThat(DocumentosIdentidad.fechaCoincideConCurp("LOHF950822MGTPRR08", FECHA)).isFalse();
        assertThat(DocumentosIdentidad.fechaCoincideConCurp("CORTA", FECHA)).isFalse();
        assertThat(DocumentosIdentidad.fechaCoincideConCurp(null, FECHA)).isFalse();
    }

    @Test
    @DisplayName("El RFC de 13 caracteres debe tener la fecha de nacimiento; el de 12 no se compara")
    void rfcCoherente() {
        assertThat(DocumentosIdentidad.fechaCoincideConRfc("LOHF950821QK5", FECHA)).isTrue();
        assertThat(DocumentosIdentidad.fechaCoincideConRfc("LOHF950822QK5", FECHA)).isFalse();
        assertThat(DocumentosIdentidad.fechaCoincideConRfc("LOH010101AB1", FECHA)).isTrue();
        assertThat(DocumentosIdentidad.fechaCoincideConRfc(null, FECHA)).isFalse();
    }

    @Test
    @DisplayName("Formato AAMMDD con ceros a la izquierda")
    void formatoAammdd() {
        assertThat(DocumentosIdentidad.fechaAammdd(LocalDate.of(2001, 2, 3))).isEqualTo("010203");
    }
}

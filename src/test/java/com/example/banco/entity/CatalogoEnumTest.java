package com.example.banco.entity;

import com.example.banco.entity.converter.EstadoCivilConverter;
import com.example.banco.entity.converter.EstatusCuentaConverter;
import com.example.banco.entity.converter.SexoConverter;
import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.entity.enums.Sexo;
import com.example.banco.exception.ValorCatalogoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Catálogos como enums (SMALLINT en la base de datos)")
class CatalogoEnumTest {

    @Test
    @DisplayName("Acepta la clave sin importar mayúsculas ni espacios, o el id")
    void conversionDesdeTexto() {
        assertThat(Sexo.desdeTexto("h")).isEqualTo(Sexo.H);
        assertThat(Sexo.desdeTexto(" M ")).isEqualTo(Sexo.M);
        assertThat(Sexo.desdeTexto("3")).isEqualTo(Sexo.X);
        assertThat(EstadoCivil.desdeTexto("union_libre")).isEqualTo(EstadoCivil.UNION_LIBRE);
        assertThat(EstatusCuenta.desdeTexto("ACTIVA")).isEqualTo(EstatusCuenta.ACTIVA);
        assertThat(Sexo.desdeTexto(null)).isNull();
    }

    @Test
    @DisplayName("Un valor inexistente indica el campo y los valores permitidos")
    void valorInvalido() {
        assertThatThrownBy(() -> Sexo.desdeTexto("Z"))
                .isInstanceOf(ValorCatalogoInvalidoException.class)
                .hasMessageContaining("sexo")
                .hasMessageContaining("H, M, X");
    }

    @Test
    @DisplayName("Los converters guardan el id SMALLINT y lo leen de vuelta")
    void converters() {
        SexoConverter sexo = new SexoConverter();
        assertThat(sexo.convertToDatabaseColumn(Sexo.M)).isEqualTo((short) 2);
        assertThat(sexo.convertToEntityAttribute((short) 3)).isEqualTo(Sexo.X);
        assertThat(sexo.convertToDatabaseColumn(null)).isNull();

        EstadoCivilConverter estadoCivil = new EstadoCivilConverter();
        assertThat(estadoCivil.convertToDatabaseColumn(EstadoCivil.SEPARADO)).isEqualTo((short) 6);
        assertThat(estadoCivil.convertToEntityAttribute((short) 1)).isEqualTo(EstadoCivil.SOLTERO);

        EstatusCuentaConverter estatus = new EstatusCuentaConverter();
        assertThat(estatus.convertToDatabaseColumn(EstatusCuenta.INACTIVA)).isEqualTo((short) 2);
        assertThat(estatus.convertToEntityAttribute((short) 4)).isEqualTo(EstatusCuenta.CANCELADA);
    }

    @Test
    @DisplayName("Un id que no existe en el catálogo es un error de integridad")
    void idInexistente() {
        assertThatThrownBy(() -> new SexoConverter().convertToEntityAttribute((short) 9))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Una cuenta nueva inicia ACTIVA y al inactivarse pasa a INACTIVA")
    void ciclodeVidaDeCuenta() {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        Cuenta cuenta = Cuenta.abrir(cliente, new java.math.BigDecimal("1000.00"), "MXN");
        assertThat(cuenta.getEstatus()).isEqualTo(EstatusCuenta.ACTIVA);
        cuenta.inactivar();
        assertThat(cuenta.getEstatus()).isEqualTo(EstatusCuenta.INACTIVA);
    }

    @Test
    @DisplayName("Cambiar la contraseña aumenta la versión de credenciales; toString no la muestra")
    void usuario() {
        Cliente cliente = new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
        cliente.setCorreo("carlos@prueba.mx");
        Usuario usuario = Usuario.crear(cliente, "$2a$04$hash");
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getCorreo()).isEqualTo("carlos@prueba.mx");
        usuario.cambiarPassword("$2a$04$otro");
        assertThat(usuario.getVersionCredenciales()).isEqualTo(1);
        assertThat(usuario.toString()).doesNotContain("$2a$");
    }
}

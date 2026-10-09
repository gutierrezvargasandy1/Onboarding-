package com.example.banco.service;

import com.example.banco.config.CuentaProperties;
import com.example.banco.dto.response.CuentaResponse;
import com.example.banco.dto.response.SaldoResponse;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Cuenta;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.exception.ClienteInactivoException;
import com.example.banco.exception.CuentaNoEncontradaException;
import com.example.banco.exception.ErrorValidacionException;
import com.example.banco.mapper.CuentaMapper;
import com.example.banco.repository.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CuentaService: apertura y consultas de cuentas")
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    private CuentaService servicio;

    @BeforeEach
    void crear() {
        servicio = new CuentaService(cuentaRepository, new CuentaProperties(new BigDecimal("1000"), "MXN"), new CuentaMapper());
    }

    private static Cliente cliente() {
        return new Cliente("GOMC900515HQTMNR08", "GOMC900515AB7");
    }

    @Test
    @DisplayName("Abre la cuenta inicial ACTIVA, asociada al cliente y con el saldo definido por el sistema")
    void abreCuentaInicial() {
        Cliente cliente = cliente();
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cuenta cuenta = servicio.abrirCuentaInicial(cliente);

        ArgumentCaptor<Cuenta> guardada = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstatus()).isEqualTo(EstatusCuenta.ACTIVA);
        assertThat(guardada.getValue().getSaldo()).isEqualByComparingTo("1000.00");
        assertThat(guardada.getValue().getSaldo().scale()).isEqualTo(2);
        assertThat(guardada.getValue().getMoneda()).isEqualTo("MXN");
        assertThat(guardada.getValue().getCliente()).isSameAs(cliente);
        assertThat(cliente.getCuentas()).containsExactly(cuenta);
    }

    @Test
    @DisplayName("Un cliente inactivo no puede tener cuentas activas")
    void clienteInactivo() {
        Cliente cliente = cliente();
        cliente.darDeBaja();
        assertThatThrownBy(() -> servicio.abrirCuentaInicial(cliente)).isInstanceOf(ClienteInactivoException.class);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("El saldo inicial negativo nunca se usa")
    void saldoNegativo() {
        CuentaService conSaldoNegativo = new CuentaService(cuentaRepository,
                new CuentaProperties(new BigDecimal("-1"), "MXN"), new CuentaMapper());
        assertThatThrownBy(() -> conSaldoNegativo.abrirCuentaInicial(cliente()))
                .isInstanceOf(ErrorValidacionException.class).hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("Consulta una cuenta y su saldo por número")
    void consulta() {
        Cuenta cuenta = Cuenta.abrir(cliente(), new BigDecimal("1000.00"), "MXN");
        when(cuentaRepository.findByNumeroCuenta("1000000016")).thenReturn(Optional.of(cuenta));

        CuentaResponse respuesta = servicio.obtener("1000000016");
        SaldoResponse saldo = servicio.consultarSaldo(" 1000000016 ");

        assertThat(respuesta.estatus()).isEqualTo(EstatusCuenta.ACTIVA);
        assertThat(saldo.saldo()).isEqualByComparingTo("1000.00");
        assertThat(saldo.moneda()).isEqualTo("MXN");
        assertThat(saldo.fechaConsulta()).isNotNull();
    }

    @Test
    @DisplayName("Formato distinto de 10 dígitos: error de validación")
    void formatoInvalido() {
        assertThatThrownBy(() -> servicio.obtener("12345")).isInstanceOf(ErrorValidacionException.class);
        assertThatThrownBy(() -> servicio.obtener("ABCDEFGHIJ")).isInstanceOf(ErrorValidacionException.class);
        assertThatThrownBy(() -> servicio.obtener(null)).isInstanceOf(ErrorValidacionException.class);
    }

    @Test
    @DisplayName("Dígito verificador incorrecto: cuenta no encontrada sin consultar la base de datos")
    void digitoVerificadorIncorrecto() {
        assertThatThrownBy(() -> servicio.obtener("1000000010")).isInstanceOf(CuentaNoEncontradaException.class);
        verify(cuentaRepository, never()).findByNumeroCuenta(anyString());
    }

    @Test
    @DisplayName("Número válido que no existe: cuenta no encontrada")
    void noExiste() {
        when(cuentaRepository.findByNumeroCuenta("1234567897")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.consultarSaldo("1234567897")).isInstanceOf(CuentaNoEncontradaException.class);
    }
}

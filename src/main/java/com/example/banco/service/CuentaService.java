package com.example.banco.service;

import com.example.banco.config.CuentaProperties;
import com.example.banco.dto.response.CuentaResponse;
import com.example.banco.dto.response.PaginaResponse;
import com.example.banco.dto.response.SaldoResponse;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Cuenta;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.exception.ClienteInactivoException;
import com.example.banco.exception.CuentaNoEncontradaException;
import com.example.banco.exception.ErrorValidacionException;
import com.example.banco.mapper.CuentaMapper;
import com.example.banco.repository.CuentaRepository;
import com.example.banco.util.NumeroCuenta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final CuentaProperties cuentaProperties;
    private final CuentaMapper cuentaMapper;

    /**
     * Abre la cuenta del cliente recién registrado: número único generado por la base
     * de datos, estatus ACTIVA y saldo inicial definido por el sistema (nunca negativo).
     * Solo se ejecuta dentro de la transacción del registro (MANDATORY).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Cuenta abrirCuentaInicial(Cliente cliente) {
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException(cliente.getId());
        }
        BigDecimal saldoInicial = cuentaProperties.saldoInicial().setScale(2, RoundingMode.HALF_UP);
        if (saldoInicial.signum() < 0) {
            throw new ErrorValidacionException("El saldo inicial no puede ser negativo");
        }
        Cuenta cuenta = cuentaRepository.save(Cuenta.abrir(cliente, saldoInicial, cuentaProperties.moneda()));
        cliente.agregarCuenta(cuenta);
        return cuenta;
    }

    @Transactional(readOnly = true)
    public CuentaResponse obtener(String numeroCuenta) {
        return cuentaMapper.aRespuesta(buscar(numeroCuenta));
    }

    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        return cuentaMapper.aSaldo(buscar(numeroCuenta));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CuentaResponse> listarActivas(Pageable pagina) {
        return PaginaResponse.de(cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA, pagina).map(cuentaMapper::aRespuesta));
    }

    /**
     * Valida el formato (10 dígitos) antes de consultar. Si el dígito verificador no
     * cuadra, la cuenta no puede existir y se responde 404 sin tocar la base de datos.
     */
    private Cuenta buscar(String numeroCuenta) {
        String numero = numeroCuenta == null ? null : numeroCuenta.strip();
        if (!NumeroCuenta.tieneFormato(numero)) {
            throw ErrorValidacionException.campo("numeroCuenta", "debe tener exactamente 10 dígitos");
        }
        if (!NumeroCuenta.esValido(numero)) {
            throw new CuentaNoEncontradaException(numero);
        }
        return cuentaRepository.findByNumeroCuenta(numero).orElseThrow(() -> new CuentaNoEncontradaException(numero));
    }
}

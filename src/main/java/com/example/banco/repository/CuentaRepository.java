package com.example.banco.repository;

import com.example.banco.entity.Cuenta;
import com.example.banco.entity.enums.EstatusCuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    /** Búsqueda por número (índice único uq_cuentas_numero). */
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    /** Cuentas por estatus, p. ej. ACTIVA (índice parcial idx_cuentas_activas). */
    Page<Cuenta> findByEstatus(EstatusCuenta estatus, Pageable pageable);
}

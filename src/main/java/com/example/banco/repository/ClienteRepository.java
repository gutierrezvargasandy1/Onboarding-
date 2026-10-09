package com.example.banco.repository;

import com.example.banco.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Consultas de clientes. El {@link EntityGraph} trae domicilio y estado en el
 * mismo SELECT (JOIN) y evita el problema N+1; las cuentas se cargan por lotes
 * (hibernate.default_batch_fetch_size).
 */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Override
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Optional<Cliente> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Page<Cliente> findAll(Pageable pageable);

    /** Clientes activos (usa el índice parcial idx_clientes_activos). */
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Page<Cliente> findByActivoTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Optional<Cliente> findByCurp(String curp);

    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Optional<Cliente> findByRfc(String rfc);

    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    Optional<Cliente> findByCorreo(String correo);

    /** Cliente dueño de una cuenta (uq_cuentas_numero resuelve la subconsulta por índice). */
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    @Query("select c from Cliente c where c.id = "
            + "(select cu.clienteId from Cuenta cu where cu.numeroCuenta = :numeroCuenta)")
    Optional<Cliente> buscarPorNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    /** Registrados en [desde, hasta): el límite superior es exclusivo (usa idx_clientes_fecha_registro). */
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    @Query("select c from Cliente c where c.fechaRegistro >= :desde and c.fechaRegistro < :hasta")
    Page<Cliente> buscarRegistradosEntre(@Param("desde") OffsetDateTime desde,
                                         @Param("hasta") OffsetDateTime hasta,
                                         Pageable pageable);

    /**
     * Carga para modificar o dar de baja: cliente, domicilio y estado en un solo SELECT.
     * La consulta no lleva bloqueo: si lo llevara, Hibernate aplicaría el mismo modo
     * de bloqueo al domicilio y al estado (que no tienen versión) y la carga fallaría.
     * El incremento forzado de versión se pide después, solo sobre el cliente
     * ({@code ClienteService.actualizar}).
     */
    @EntityGraph(attributePaths = {"domicilio", "domicilio.estado"})
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> buscarParaModificar(@Param("id") Integer id);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCurpAndRfc(String curp, String rfc);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);
}

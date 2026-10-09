package com.example.banco.integracion;

import com.example.banco.entity.Cliente;
import com.example.banco.repository.ClienteRepository;
import com.example.banco.soporte.DatosPrueba;
import com.example.banco.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Concurrencia · peticiones simultáneas")
class ConcurrenciaIT extends PruebaIntegracion {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    @DisplayName("Dos registros simultáneos del mismo cliente: exactamente uno se crea y el otro recibe 409")
    void registrosSimultaneos() throws Exception {
        Map<String, Object> cliente = DatosPrueba.cliente();
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Integer> registro = () -> {
            salida.await();
            return registrar(cliente).andReturn().getResponse().getStatus();
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> resultados = new ArrayList<>(List.of(hilos.submit(registro), hilos.submit(registro)));
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                estados.add(resultado.get());
            }
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(contarFilas("clientes")).isEqualTo(1);
        assertThat(contarFilas("cuentas")).isEqualTo(1);
        assertThat(contarFilas("usuarios")).isEqualTo(1);
    }

    @Test
    @DisplayName("Bloqueo optimista (@Version): una modificación basada en datos viejos se rechaza")
    void bloqueoOptimista() throws Exception {
        ClienteRegistrado registrado = registrarCliente();
        TransactionTemplate exterior = new TransactionTemplate(transacciones);
        TransactionTemplate interior = new TransactionTemplate(transacciones);
        interior.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> exterior.executeWithoutResult(estado -> {
            Cliente versionVieja = clienteRepository.findById(registrado.id()).orElseThrow();
            // Otra transacción modifica y confirma primero
            interior.executeWithoutResult(otro -> {
                Cliente actual = clienteRepository.findById(registrado.id()).orElseThrow();
                actual.setOcupacion("Cambio concurrente");
            });
            versionVieja.setOcupacion("Cambio con datos viejos");
            clienteRepository.flush();
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(jdbc.queryForObject("SELECT ocupacion FROM onboarding.clientes WHERE id = ?", String.class, registrado.id()))
                .isEqualTo("Cambio concurrente");
    }
}

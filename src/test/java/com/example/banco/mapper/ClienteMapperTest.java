package com.example.banco.mapper;

import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.dto.response.ClienteResponse;
import com.example.banco.entity.CatEstado;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Cuenta;
import com.example.banco.soporte.DatosPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mapeo entre DTOs y entidades")
class ClienteMapperTest {

    private final ClienteMapper mapper = new ClienteMapper(new CuentaMapper());
    private final CatEstado queretaro = new CatEstado((short) 22, "Querétaro");

    @Test
    @DisplayName("Copia todos los datos de la solicitud, incluido el domicilio con su estado")
    void nuevoCliente() {
        Map<String, Object> datos = DatosPrueba.cliente();
        ClienteRequest solicitud = DatosPrueba.solicitud(datos);

        Cliente cliente = mapper.nuevoCliente(solicitud, queretaro);

        assertThat(cliente.getCurp()).isEqualTo(solicitud.curp());
        assertThat(cliente.getRfc()).isEqualTo(solicitud.rfc());
        assertThat(cliente.getNombreCompleto()).isEqualTo("Carlos Alberto Gómez Mendoza");
        assertThat(cliente.getIngresoMensual()).isEqualByComparingTo("28000.00");
        assertThat(cliente.isActivo()).isTrue();
        assertThat(cliente.getDomicilio().getCliente()).isSameAs(cliente);
        assertThat(cliente.getDomicilio().getEstado()).isSameAs(queretaro);
        assertThat(cliente.getDomicilio().getPais()).isEqualTo("MX");
    }

    @Test
    @DisplayName("La respuesta incluye domicilio, nombre del estado y cuentas, sin la contraseña")
    void respuesta() {
        Cliente cliente = mapper.nuevoCliente(DatosPrueba.solicitud(), queretaro);
        cliente.agregarCuenta(Cuenta.abrir(cliente, new BigDecimal("1000.00"), "MXN"));

        ClienteResponse respuesta = mapper.aRespuesta(cliente);

        assertThat(respuesta.domicilio().estadoId()).isEqualTo((short) 22);
        assertThat(respuesta.domicilio().estado()).isEqualTo("Querétaro");
        assertThat(respuesta.cuentas()).hasSize(1);
        assertThat(respuesta.toString()).doesNotContain(DatosPrueba.PASSWORD);
    }

    @Test
    @DisplayName("Al actualizar reutiliza el mismo domicilio (relación 1:1)")
    void reutilizaDomicilio() {
        Cliente cliente = mapper.nuevoCliente(DatosPrueba.solicitud(), queretaro);
        var domicilioOriginal = cliente.getDomicilio();

        mapper.copiarDatos(DatosPrueba.solicitud(), new CatEstado((short) 11, "Guanajuato"), cliente);

        assertThat(cliente.getDomicilio()).isSameAs(domicilioOriginal);
        assertThat(cliente.getDomicilio().getEstado().getNombre()).isEqualTo("Guanajuato");
    }
}

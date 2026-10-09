package com.example.banco.service;

import com.example.banco.dto.request.ClienteActualizacionRequest;
import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.dto.response.ClienteResponse;
import com.example.banco.entity.CatEstado;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Cuenta;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.exception.CampoNoModificableException;
import com.example.banco.exception.ClienteInactivoException;
import com.example.banco.exception.ClienteNoEncontradoException;
import com.example.banco.exception.ClienteYaRegistradoException;
import com.example.banco.exception.CorreoDuplicadoException;
import com.example.banco.exception.CurpDuplicadaException;
import com.example.banco.exception.ErrorCampo;
import com.example.banco.exception.ErrorValidacionException;
import com.example.banco.exception.RfcDuplicadoException;
import com.example.banco.mapper.ClienteMapper;
import com.example.banco.mapper.CuentaMapper;
import com.example.banco.repository.ClienteRepository;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.soporte.DatosPrueba;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService: reglas de negocio")
class ClienteServiceTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final CatEstado QUERETARO = new CatEstado((short) 22, "Querétaro");

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CatalogoService catalogoService;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private EntityManager entityManager;

    private ClienteService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ClienteService(clienteRepository, usuarioRepository, catalogoService, cuentaService,
                usuarioService, new ClienteMapper(new CuentaMapper()), entityManager);
        lenient().when(catalogoService.obtenerEstado(any())).thenReturn(QUERETARO);
    }

    private static Cliente clienteExistente(ClienteRequest solicitud) {
        Cliente cliente = new ClienteMapper(new CuentaMapper()).nuevoCliente(solicitud, QUERETARO);
        Cuenta cuenta = Cuenta.abrir(cliente, new BigDecimal("1000.00"), "MXN");
        cliente.agregarCuenta(cuenta);
        return cliente;
    }

    private static ClienteActualizacionRequest actualizacion(Map<String, Object> datos) {
        return JSON.convertValue(datos, ClienteActualizacionRequest.class);
    }

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("Guarda el cliente y en la misma operación abre su cuenta y crea su usuario, en ese orden")
        void registraClienteCuentaYUsuario() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(cuentaService.abrirCuentaInicial(any(Cliente.class))).thenAnswer(invocacion -> {
                Cliente cliente = invocacion.getArgument(0);
                Cuenta cuenta = Cuenta.abrir(cliente, new BigDecimal("1000.00"), "MXN");
                cliente.agregarCuenta(cuenta);
                return cuenta;
            });

            ClienteResponse respuesta = servicio.registrar(solicitud);

            InOrder orden = inOrder(clienteRepository, cuentaService, usuarioService);
            orden.verify(clienteRepository).save(any(Cliente.class));
            orden.verify(cuentaService).abrirCuentaInicial(any(Cliente.class));
            orden.verify(usuarioService).crearUsuario(any(Cliente.class), eq(DatosPrueba.PASSWORD));
            assertThat(respuesta.curp()).isEqualTo(solicitud.curp());
            assertThat(respuesta.activo()).isTrue();
            assertThat(respuesta.domicilio().estado()).isEqualTo("Querétaro");
            assertThat(respuesta.cuentas()).singleElement()
                    .satisfies(cuenta -> assertThat(cuenta.estatus()).isEqualTo(EstatusCuenta.ACTIVA));
        }

        @Test
        @DisplayName("Misma CURP y mismo RFC: cliente ya registrado")
        void clienteYaRegistrado() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(clienteRepository.existsByCurp(solicitud.curp())).thenReturn(true);
            when(clienteRepository.existsByRfc(solicitud.rfc())).thenReturn(true);
            when(clienteRepository.existsByCurpAndRfc(solicitud.curp(), solicitud.rfc())).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(ClienteYaRegistradoException.class);
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("CURP de otra persona: CURP duplicada")
        void curpDuplicada() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(clienteRepository.existsByCurp(solicitud.curp())).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(CurpDuplicadaException.class);
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("RFC de otra persona: RFC duplicado")
        void rfcDuplicado() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(clienteRepository.existsByRfc(solicitud.rfc())).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(RfcDuplicadoException.class);
        }

        @Test
        @DisplayName("Correo de otro cliente o de otro usuario: correo duplicado")
        void correoDuplicado() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(clienteRepository.existsByCorreo(solicitud.correo())).thenReturn(true);
            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(CorreoDuplicadoException.class);

            when(clienteRepository.existsByCorreo(solicitud.correo())).thenReturn(false);
            when(usuarioRepository.existsByCorreo(solicitud.correo())).thenReturn(true);
            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(CorreoDuplicadoException.class);
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("La fecha de nacimiento debe coincidir con la de la CURP y el RFC")
        void coherenciaDeIdentidad() {
            Map<String, Object> datos = DatosPrueba.cliente();
            datos.put("fechaNacimiento", "1990-05-16");
            ClienteRequest solicitud = DatosPrueba.solicitud(datos);

            assertThatThrownBy(() -> servicio.registrar(solicitud))
                    .isInstanceOfSatisfying(ErrorValidacionException.class, ex ->
                            assertThat(ex.getErrores()).extracting(ErrorCampo::campo).containsExactly("curp", "rfc"));
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una nacionalidad fuera del catálogo detiene el registro")
        void nacionalidadInvalida() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            org.mockito.Mockito.doThrow(ErrorValidacionException.campo("nacionalidad", "no existe"))
                    .when(catalogoService).validarPais(anyString(), eq("nacionalidad"));

            assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(ErrorValidacionException.class);
            verify(clienteRepository, never()).save(any());
            verifyNoInteractions(cuentaService, usuarioService);
        }
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("Actualiza datos personales, de contacto, domicilio y laborales")
        void actualiza() {
            Map<String, Object> datos = DatosPrueba.cliente();
            Cliente cliente = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));
            Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
            cambios.put("telefonoMovil", "4429998877");
            cambios.put("ocupacion", "Gerente");
            DatosPrueba.castMapa(cambios.get("domicilio")).put("calle", "Calle Nueva");

            ClienteResponse respuesta = servicio.actualizar(1, actualizacion(cambios));

            assertThat(respuesta.telefonoMovil()).isEqualTo("4429998877");
            assertThat(respuesta.ocupacion()).isEqualTo("Gerente");
            assertThat(respuesta.domicilio().calle()).isEqualTo("Calle Nueva");
            verify(clienteRepository).flush();
            verify(usuarioService, never()).sincronizarCorreo(any());
        }

        @Test
        @DisplayName("Fuerza el incremento de versión solo del cliente, aunque cambie únicamente el domicilio")
        void incrementoForzadoDeVersion() {
            Map<String, Object> datos = DatosPrueba.cliente();
            Cliente cliente = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));
            Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
            DatosPrueba.castMapa(cambios.get("domicilio")).put("calle", "Calle Nueva");

            servicio.actualizar(1, actualizacion(cambios));

            verify(entityManager).lock(cliente, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            verify(entityManager, never()).lock(eq(cliente.getDomicilio()), any(LockModeType.class));
        }

        @Test
        @DisplayName("Si cambia el correo, el usuario de acceso se sincroniza")
        void sincronizaCorreo() {
            Map<String, Object> datos = DatosPrueba.cliente();
            Cliente cliente = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));
            Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
            cambios.put("correo", "nuevo@prueba.mx");

            servicio.actualizar(1, actualizacion(cambios));

            verify(usuarioService).sincronizarCorreo(cliente);
            assertThat(cliente.getCorreo()).isEqualTo("nuevo@prueba.mx");
        }

        @Test
        @DisplayName("No permite modificar la CURP, el RFC ni el número de cuenta")
        void camposNoModificables() {
            Map<String, Object> datos = DatosPrueba.cliente();
            Cliente cliente = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));

            Map<String, Object> conCurp = DatosPrueba.actualizacion(datos);
            conCurp.put("curp", "LOHF950821MGTPRR08");
            assertThatThrownBy(() -> servicio.actualizar(1, actualizacion(conCurp)))
                    .isInstanceOf(CampoNoModificableException.class).hasMessageContaining("CURP");

            Map<String, Object> conRfc = DatosPrueba.actualizacion(datos);
            conRfc.put("rfc", "LOHF950821QK5");
            assertThatThrownBy(() -> servicio.actualizar(1, actualizacion(conRfc)))
                    .isInstanceOf(CampoNoModificableException.class).hasMessageContaining("RFC");

            Map<String, Object> conCuenta = DatosPrueba.actualizacion(datos);
            conCuenta.put("numeroCuenta", "1234567897");
            assertThatThrownBy(() -> servicio.actualizar(1, actualizacion(conCuenta)))
                    .isInstanceOf(CampoNoModificableException.class).hasMessageContaining("número de cuenta");
            verify(clienteRepository, never()).flush();
        }

        @Test
        @DisplayName("Enviar la misma CURP y el mismo RFC no es un cambio y se permite")
        void mismosValoresInmutables() {
            Map<String, Object> datos = DatosPrueba.cliente();
            Cliente cliente = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));
            Map<String, Object> cambios = DatosPrueba.actualizacion(datos);
            cambios.put("curp", datos.get("curp").toString().toLowerCase());
            cambios.put("rfc", datos.get("rfc"));

            assertThat(servicio.actualizar(1, actualizacion(cambios)).curp()).isEqualTo(datos.get("curp"));
        }

        @Test
        @DisplayName("Cliente inexistente, dado de baja o con correo ajeno")
        void casosDeError() {
            Map<String, Object> datos = DatosPrueba.cliente();
            ClienteActualizacionRequest cambios = actualizacion(DatosPrueba.actualizacion(datos));

            when(clienteRepository.buscarParaModificar(9)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.actualizar(9, cambios)).isInstanceOf(ClienteNoEncontradoException.class);

            Cliente inactivo = clienteExistente(DatosPrueba.solicitud(datos));
            inactivo.darDeBaja();
            when(clienteRepository.buscarParaModificar(2)).thenReturn(Optional.of(inactivo));
            assertThatThrownBy(() -> servicio.actualizar(2, cambios)).isInstanceOf(ClienteInactivoException.class);
            verify(entityManager, never()).lock(eq(inactivo), any(LockModeType.class));

            Cliente activo = clienteExistente(DatosPrueba.solicitud(datos));
            when(clienteRepository.buscarParaModificar(3)).thenReturn(Optional.of(activo));
            Map<String, Object> otroCorreo = DatosPrueba.actualizacion(datos);
            otroCorreo.put("correo", "ajeno@prueba.mx");
            when(clienteRepository.existsByCorreoAndIdNot("ajeno@prueba.mx", 3)).thenReturn(true);
            assertThatThrownBy(() -> servicio.actualizar(3, actualizacion(otroCorreo)))
                    .isInstanceOf(CorreoDuplicadoException.class);
        }
    }

    @Nested
    @DisplayName("Baja lógica")
    class BajaLogica {

        @Test
        @DisplayName("Desactiva al cliente, inactiva sus cuentas e inactiva su usuario")
        void daDeBaja() {
            Cliente cliente = clienteExistente(DatosPrueba.solicitud());
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));

            servicio.darDeBaja(1);

            assertThat(cliente.isActivo()).isFalse();
            assertThat(cliente.getCuentas()).allSatisfy(cuenta ->
                    assertThat(cuenta.getEstatus()).isEqualTo(EstatusCuenta.INACTIVA));
            verify(usuarioService).inactivarPorBajaDeCliente(1);
            verify(clienteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Repetir la baja no hace nada (idempotente)")
        void idempotente() {
            Cliente cliente = clienteExistente(DatosPrueba.solicitud());
            cliente.darDeBaja();
            when(clienteRepository.buscarParaModificar(1)).thenReturn(Optional.of(cliente));

            servicio.darDeBaja(1);

            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Cliente inexistente: cliente no encontrado")
        void inexistente() {
            when(clienteRepository.buscarParaModificar(5)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.darDeBaja(5)).isInstanceOf(ClienteNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("El rango de fechas se convierte a instantes de la Ciudad de México, con fin exclusivo")
        void rangoDeFechas() {
            Pageable pagina = PageRequest.of(0, 20);
            when(clienteRepository.buscarRegistradosEntre(any(), any(), eq(pagina))).thenReturn(Page.empty(pagina));

            servicio.listarRegistradosEntre(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6), pagina);

            ArgumentCaptor<OffsetDateTime> desde = ArgumentCaptor.forClass(OffsetDateTime.class);
            ArgumentCaptor<OffsetDateTime> hasta = ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(clienteRepository).buscarRegistradosEntre(desde.capture(), hasta.capture(), eq(pagina));
            assertThat(desde.getValue()).isEqualTo(OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-6)));
            assertThat(hasta.getValue()).isEqualTo(OffsetDateTime.of(2026, 10, 7, 0, 0, 0, 0, ZoneOffset.ofHours(-6)));
        }

        @Test
        @DisplayName("'desde' posterior a 'hasta' es un error de validación")
        void rangoInvertido() {
            assertThatThrownBy(() -> servicio.listarRegistradosEntre(LocalDate.of(2026, 10, 6),
                    LocalDate.of(2026, 10, 1), PageRequest.of(0, 20)))
                    .isInstanceOf(ErrorValidacionException.class);
        }

        @Test
        @DisplayName("Busca por CURP normalizada a mayúsculas y valida su formato")
        void porCurp() {
            ClienteRequest solicitud = DatosPrueba.solicitud();
            when(clienteRepository.findByCurp(solicitud.curp())).thenReturn(Optional.of(clienteExistente(solicitud)));

            assertThat(servicio.obtenerPorCurp(" " + solicitud.curp().toLowerCase()).curp()).isEqualTo(solicitud.curp());
            assertThatThrownBy(() -> servicio.obtenerPorCurp("NO-ES-CURP")).isInstanceOf(ErrorValidacionException.class);
            when(clienteRepository.findByCurp("LOHF950821MGTPRR08")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.obtenerPorCurp("LOHF950821MGTPRR08")).isInstanceOf(ClienteNoEncontradoException.class);
        }

        @Test
        @DisplayName("Busca por RFC, correo y número de cuenta validando el formato de cada uno")
        void otrosCriterios() {
            assertThatThrownBy(() -> servicio.obtenerPorRfc("ABC")).isInstanceOf(ErrorValidacionException.class);
            assertThatThrownBy(() -> servicio.obtenerPorCorreo("sin-arroba")).isInstanceOf(ErrorValidacionException.class);
            assertThatThrownBy(() -> servicio.obtenerPorNumeroCuenta("123")).isInstanceOf(ErrorValidacionException.class);
            when(clienteRepository.buscarPorNumeroCuenta("1000000016")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.obtenerPorNumeroCuenta("1000000016")).isInstanceOf(ClienteNoEncontradoException.class);
        }
    }
}

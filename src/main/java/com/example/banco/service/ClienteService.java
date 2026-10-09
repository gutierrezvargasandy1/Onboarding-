package com.example.banco.service;

import com.example.banco.dto.request.ClienteActualizacionRequest;
import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.dto.response.ClienteResponse;
import com.example.banco.dto.response.PaginaResponse;
import com.example.banco.entity.CatEstado;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Cuenta;
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
import com.example.banco.repository.ClienteRepository;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.util.Constantes;
import com.example.banco.util.DocumentosIdentidad;
import com.example.banco.util.NumeroCuenta;
import com.example.banco.util.Texto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de clientes: alta (cliente + domicilio + cuenta + usuario en una
 * sola transacción), consultas, actualización y baja lógica.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final Pattern CURP = Pattern.compile(Constantes.REGEX_CURP);
    private static final Pattern RFC = Pattern.compile(Constantes.REGEX_RFC);
    private static final Pattern CORREO = Pattern.compile(Constantes.REGEX_CORREO);

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoService catalogoService;
    private final CuentaService cuentaService;
    private final UsuarioService usuarioService;
    private final ClienteMapper clienteMapper;
    private final EntityManager entityManager;

    // ================================================================ ALTA

    /**
     * Registra al cliente y, en la misma transacción, abre su cuenta y crea su usuario.
     * Si cualquier paso falla, no se guarda nada (atomicidad).
     */
    @Transactional
    public ClienteResponse registrar(ClienteRequest solicitud) {
        validarUnicidad(solicitud.curp(), solicitud.rfc(), solicitud.correo());
        validarCoherenciaIdentidad(solicitud.curp(), solicitud.rfc(), solicitud.fechaNacimiento());
        catalogoService.validarPais(solicitud.nacionalidad(), "nacionalidad");
        CatEstado estado = catalogoService.obtenerEstado(solicitud.domicilio().estadoId());

        Cliente cliente = clienteMapper.nuevoCliente(solicitud, estado);
        clienteRepository.save(cliente);

        Cuenta cuenta = cuentaService.abrirCuentaInicial(cliente);
        usuarioService.crearUsuario(cliente, solicitud.password());

        log.info("Cliente {} registrado con la cuenta {}", cliente.getId(), NumeroCuenta.enmascarar(cuenta.getNumeroCuenta()));
        return clienteMapper.aRespuesta(cliente);
    }

    // =========================================================== CONSULTAS

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Integer id) {
        return clienteMapper.aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listar(Pageable pagina) {
        return PaginaResponse.de(clienteRepository.findAll(pagina).map(clienteMapper::aRespuesta));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listarActivos(Pageable pagina) {
        return PaginaResponse.de(clienteRepository.findByActivoTrue(pagina).map(clienteMapper::aRespuesta));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCurp(String curp) {
        String valor = Texto.mayusculas(curp);
        validarFormato(valor, CURP, "curp", "formato de CURP inválido");
        return clienteRepository.findByCurp(valor).map(clienteMapper::aRespuesta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con la CURP " + valor));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorRfc(String rfc) {
        String valor = Texto.mayusculas(rfc);
        validarFormato(valor, RFC, "rfc", "formato de RFC inválido");
        return clienteRepository.findByRfc(valor).map(clienteMapper::aRespuesta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con el RFC " + valor));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCorreo(String correo) {
        String valor = Texto.minusculas(correo);
        validarFormato(valor, CORREO, "correo", "formato de correo electrónico inválido");
        return clienteRepository.findByCorreo(valor).map(clienteMapper::aRespuesta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con el correo " + valor));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        String valor = Texto.limpiar(numeroCuenta);
        if (!NumeroCuenta.tieneFormato(valor)) {
            throw ErrorValidacionException.campo("numeroCuenta", "debe tener exactamente 10 dígitos");
        }
        return clienteRepository.buscarPorNumeroCuenta(valor).map(clienteMapper::aRespuesta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con la cuenta " + valor));
    }

    /** Clientes registrados entre dos fechas (incluidas), en hora de la Ciudad de México. */
    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listarRegistradosEntre(LocalDate desde, LocalDate hasta, Pageable pagina) {
        if (desde.isAfter(hasta)) {
            throw ErrorValidacionException.campo("desde", "no puede ser posterior a 'hasta'");
        }
        OffsetDateTime inicio = desde.atStartOfDay(Constantes.ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime finExclusivo = hasta.plusDays(1).atStartOfDay(Constantes.ZONA_HORARIA).toOffsetDateTime();
        return PaginaResponse.de(clienteRepository.buscarRegistradosEntre(inicio, finExclusivo, pagina)
                .map(clienteMapper::aRespuesta));
    }

    // ======================================================= ACTUALIZACIÓN

    /**
     * Actualiza datos personales, de contacto, domicilio e información laboral.
     * CURP, RFC y número de cuenta no se pueden modificar.
     */
    @Transactional
    public ClienteResponse actualizar(Integer id, ClienteActualizacionRequest solicitud) {
        Cliente cliente = clienteRepository.buscarParaModificar(id).orElseThrow(() -> ClienteNoEncontradoException.porId(id));
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException(id);
        }
        // El domicilio no tiene versión propia: se fuerza el incremento de la versión del
        // cliente al confirmar, así dos actualizaciones simultáneas se detectan aunque solo
        // cambie el domicilio (la segunda recibe 409 CONFLICTO_CONCURRENCIA).
        entityManager.lock(cliente, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        rechazarCambiosEnCamposInmutables(cliente, solicitud);
        if (!cliente.getCorreo().equals(solicitud.correo())
                && (clienteRepository.existsByCorreoAndIdNot(solicitud.correo(), id)
                || usuarioRepository.existsByCorreoAndClienteIdNot(solicitud.correo(), id))) {
            throw new CorreoDuplicadoException();
        }
        validarCoherenciaIdentidad(cliente.getCurp(), cliente.getRfc(), solicitud.fechaNacimiento());
        catalogoService.validarPais(solicitud.nacionalidad(), "nacionalidad");
        CatEstado estado = catalogoService.obtenerEstado(solicitud.domicilio().estadoId());

        boolean cambioCorreo = !cliente.getCorreo().equals(solicitud.correo());
        clienteMapper.copiarDatos(solicitud, estado, cliente);
        clienteRepository.flush();
        if (cambioCorreo) {
            usuarioService.sincronizarCorreo(cliente);
        }
        log.info("Cliente {} actualizado", id);
        return clienteMapper.aRespuesta(cliente);
    }

    // ========================================================= BAJA LÓGICA

    /**
     * Desactiva al cliente sin borrar nada: sus cuentas activas pasan a INACTIVA y su
     * usuario queda inactivo. Es idempotente: repetirla no cambia nada.
     */
    @Transactional
    public void darDeBaja(Integer id) {
        Cliente cliente = clienteRepository.buscarParaModificar(id).orElseThrow(() -> ClienteNoEncontradoException.porId(id));
        if (!cliente.isActivo()) {
            return;
        }
        cliente.darDeBaja();
        cliente.getCuentas().forEach(Cuenta::inactivar);
        usuarioService.inactivarPorBajaDeCliente(id);
        log.info("Cliente {} dado de baja (baja lógica)", id);
    }

    // ============================================================ apoyo

    private Cliente buscar(Integer id) {
        return clienteRepository.findById(id).orElseThrow(() -> ClienteNoEncontradoException.porId(id));
    }

    /** Misma CURP y RFC = ya registrado; si solo coincide uno, se indica cuál. */
    private void validarUnicidad(String curp, String rfc, String correo) {
        boolean curpExiste = clienteRepository.existsByCurp(curp);
        boolean rfcExiste = clienteRepository.existsByRfc(rfc);
        if (curpExiste && rfcExiste && clienteRepository.existsByCurpAndRfc(curp, rfc)) {
            throw new ClienteYaRegistradoException();
        }
        if (curpExiste) {
            throw new CurpDuplicadaException();
        }
        if (rfcExiste) {
            throw new RfcDuplicadoException();
        }
        if (clienteRepository.existsByCorreo(correo) || usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException();
        }
    }

    /** La fecha de nacimiento debe coincidir con la que llevan la CURP y el RFC (13 caracteres). */
    private static void validarCoherenciaIdentidad(String curp, String rfc, LocalDate fechaNacimiento) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (!DocumentosIdentidad.fechaCoincideConCurp(curp, fechaNacimiento)) {
            errores.add(new ErrorCampo("curp", "la fecha de la CURP no coincide con la fecha de nacimiento"));
        }
        if (!DocumentosIdentidad.fechaCoincideConRfc(rfc, fechaNacimiento)) {
            errores.add(new ErrorCampo("rfc", "la fecha del RFC no coincide con la fecha de nacimiento"));
        }
        if (!errores.isEmpty()) {
            throw new ErrorValidacionException("La fecha de nacimiento no coincide con los documentos de identidad", errores);
        }
    }

    private static void rechazarCambiosEnCamposInmutables(Cliente cliente, ClienteActualizacionRequest solicitud) {
        if (solicitud.curp() != null && !solicitud.curp().equals(cliente.getCurp())) {
            throw new CampoNoModificableException("curp", "La CURP");
        }
        if (solicitud.rfc() != null && !solicitud.rfc().equals(cliente.getRfc())) {
            throw new CampoNoModificableException("rfc", "El RFC");
        }
        if (solicitud.numeroCuenta() != null && cliente.getCuentas().stream()
                .noneMatch(cuenta -> Objects.equals(cuenta.getNumeroCuenta(), solicitud.numeroCuenta()))) {
            throw new CampoNoModificableException("numeroCuenta", "El número de cuenta");
        }
    }

    private static void validarFormato(String valor, Pattern formato, String campo, String mensaje) {
        if (valor == null || !formato.matcher(valor).matches()) {
            throw ErrorValidacionException.campo(campo, mensaje);
        }
    }
}

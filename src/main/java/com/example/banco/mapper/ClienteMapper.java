package com.example.banco.mapper;

import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.dto.request.DatosCliente;
import com.example.banco.dto.request.DomicilioRequest;
import com.example.banco.dto.response.ClienteResponse;
import com.example.banco.dto.response.DomicilioResponse;
import com.example.banco.entity.CatEstado;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Domicilio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Conversión entre DTOs y entidades. Los DTOs llegan ya normalizados y validados;
 * aquí solo se copian datos (sin reglas de negocio).
 */
@Component
@RequiredArgsConstructor
public class ClienteMapper {

    private final CuentaMapper cuentaMapper;

    public Cliente nuevoCliente(ClienteRequest solicitud, CatEstado estado) {
        Cliente cliente = new Cliente(solicitud.curp(), solicitud.rfc());
        copiarDatos(solicitud, estado, cliente);
        return cliente;
    }

    /** Copia los datos modificables (alta y actualización comparten este método). */
    public void copiarDatos(DatosCliente datos, CatEstado estado, Cliente cliente) {
        cliente.setNombre(datos.nombre());
        cliente.setSegundoNombre(datos.segundoNombre());
        cliente.setApellidoPaterno(datos.apellidoPaterno());
        cliente.setApellidoMaterno(datos.apellidoMaterno());
        cliente.setFechaNacimiento(datos.fechaNacimiento());
        cliente.setSexo(datos.sexo());
        cliente.setNacionalidad(datos.nacionalidad());
        cliente.setEstadoCivil(datos.estadoCivil());
        cliente.setCorreo(datos.correo());
        cliente.setTelefonoMovil(datos.telefonoMovil());
        cliente.setTelefonoAlterno(datos.telefonoAlterno());
        cliente.setOcupacion(datos.ocupacion());
        cliente.setEmpresa(datos.empresa());
        cliente.setIngresoMensual(datos.ingresoMensual());

        DomicilioRequest origen = datos.domicilio();
        Domicilio domicilio = cliente.getDomicilio() != null ? cliente.getDomicilio() : new Domicilio();
        domicilio.setCalle(origen.calle());
        domicilio.setNumeroExterior(origen.numeroExterior());
        domicilio.setNumeroInterior(origen.numeroInterior());
        domicilio.setColonia(origen.colonia());
        domicilio.setMunicipio(origen.municipio());
        domicilio.setEstado(estado);
        domicilio.setCodigoPostal(origen.codigoPostal());
        domicilio.setPais(origen.pais());
        if (cliente.getDomicilio() == null) {
            cliente.asignarDomicilio(domicilio);
        }
    }

    public ClienteResponse aRespuesta(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getSegundoNombre(),
                cliente.getApellidoPaterno(),
                cliente.getApellidoMaterno(),
                cliente.getNombreCompleto(),
                cliente.getFechaNacimiento(),
                cliente.getCurp(),
                cliente.getRfc(),
                cliente.getSexo(),
                cliente.getNacionalidad(),
                cliente.getEstadoCivil(),
                cliente.getCorreo(),
                cliente.getTelefonoMovil(),
                cliente.getTelefonoAlterno(),
                aRespuesta(cliente.getDomicilio()),
                cliente.getOcupacion(),
                cliente.getEmpresa(),
                cliente.getIngresoMensual(),
                cliente.isActivo(),
                cliente.getFechaRegistro(),
                cliente.getFechaActualizacion(),
                cliente.getFechaBaja(),
                cliente.getCuentas().stream().map(cuentaMapper::aRespuesta).toList());
    }

    private DomicilioResponse aRespuesta(Domicilio domicilio) {
        if (domicilio == null) {
            return null;
        }
        CatEstado estado = domicilio.getEstado();
        return new DomicilioResponse(domicilio.getCalle(), domicilio.getNumeroExterior(),
                domicilio.getNumeroInterior(), domicilio.getColonia(), domicilio.getMunicipio(),
                estado.getId(), estado.getNombre(), domicilio.getCodigoPostal(), domicilio.getPais());
    }
}

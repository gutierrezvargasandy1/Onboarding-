package com.example.banco.web;

import com.example.banco.exception.ErrorValidacionException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/** Lista blanca de campos por los que se puede ordenar una consulta paginada. */
public final class Ordenamiento {

    public static final Set<String> CLIENTES = Set.of(
            "id", "nombre", "apellidoPaterno", "apellidoMaterno", "fechaNacimiento", "fechaRegistro", "curp", "rfc", "correo");
    public static final Set<String> CUENTAS = Set.of("id", "numeroCuenta", "saldo", "fechaApertura");

    private Ordenamiento() {
    }

    public static Pageable validar(Pageable pagina, Set<String> permitidos) {
        for (Sort.Order orden : pagina.getSort()) {
            if (!permitidos.contains(orden.getProperty())) {
                throw ErrorValidacionException.campo("sort", "no se puede ordenar por '" + orden.getProperty()
                        + "'; opciones: " + String.join(", ", new TreeSet<>(permitidos)));
            }
        }
        return pagina;
    }
}

package com.example.banco.validation.validator;

import com.example.banco.util.Constantes;
import com.example.banco.validation.annotation.MayorDeEdad;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/**
 * Valida la fecha de nacimiento con el día actual de la Ciudad de México (la misma
 * regla que el trigger de la base de datos, sin depender de la zona horaria del
 * servidor): no futura, no anterior a 1900 y con 18 años cumplidos.
 */
public class MayorDeEdadValidator implements ConstraintValidator<MayorDeEdad, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext contexto) {
        if (fechaNacimiento == null) {
            return true;
        }
        LocalDate hoy = LocalDate.now(Constantes.ZONA_HORARIA);
        if (fechaNacimiento.isAfter(hoy)) {
            return rechazar(contexto, "no puede ser una fecha futura");
        }
        if (fechaNacimiento.isBefore(Constantes.FECHA_NACIMIENTO_MINIMA)) {
            return rechazar(contexto, "no puede ser anterior a 1900-01-01");
        }
        return Period.between(fechaNacimiento, hoy).getYears() >= Constantes.EDAD_MINIMA;
    }

    private static boolean rechazar(ConstraintValidatorContext contexto, String mensaje) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
        return false;
    }
}

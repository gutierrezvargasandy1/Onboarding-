package com.example.banco.validation.annotation;

import com.example.banco.util.Constantes;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CURP: 18 caracteres con el formato oficial de RENAPO. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Size(min = 18, max = 18, message = "debe tener 18 caracteres")
@Pattern(regexp = Constantes.REGEX_CURP, message = "formato de CURP inválido")
public @interface Curp {

    String message() default "CURP inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

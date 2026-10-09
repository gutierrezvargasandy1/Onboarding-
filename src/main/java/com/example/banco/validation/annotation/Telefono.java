package com.example.banco.validation.annotation;

import com.example.banco.util.Constantes;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Teléfono de exactamente 10 dígitos. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = Constantes.REGEX_TELEFONO, message = "debe contener exactamente 10 dígitos")
public @interface Telefono {

    String message() default "teléfono inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

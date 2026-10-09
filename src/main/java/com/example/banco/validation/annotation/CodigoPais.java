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

/** Código de país ISO 3166-1 alfa-2 (dos letras, p. ej. MX). */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = Constantes.REGEX_CODIGO_PAIS, message = "debe ser un código ISO de 2 letras, p. ej. MX")
public @interface CodigoPais {

    String message() default "código de país inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

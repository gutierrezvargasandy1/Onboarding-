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

/** Código postal de exactamente 5 dígitos. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = Constantes.REGEX_CODIGO_POSTAL, message = "debe contener exactamente 5 dígitos")
public @interface CodigoPostal {

    String message() default "código postal inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

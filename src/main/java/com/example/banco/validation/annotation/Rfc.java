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

/** RFC: 12 o 13 caracteres con el formato del SAT. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Size(min = 12, max = 13, message = "debe tener 12 o 13 caracteres")
@Pattern(regexp = Constantes.REGEX_RFC, message = "formato de RFC inválido")
public @interface Rfc {

    String message() default "RFC inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

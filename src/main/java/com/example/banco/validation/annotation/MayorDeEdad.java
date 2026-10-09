package com.example.banco.validation.annotation;

import com.example.banco.validation.validator.MayorDeEdadValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Fecha de nacimiento válida: no futura, desde 1900 y con 18 años cumplidos (hora de la Ciudad de México). */
@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {

    String message() default "el cliente debe ser mayor de edad (18 años o más)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

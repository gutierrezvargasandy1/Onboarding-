package com.example.banco.validation.annotation;

import com.example.banco.validation.validator.PasswordSeguraValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Contraseña que cumple la política (8+ caracteres, mayúscula, minúscula, número y especial). */
@Documented
@Constraint(validatedBy = PasswordSeguraValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSegura {

    String message() default "la contraseña no cumple la política de seguridad";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

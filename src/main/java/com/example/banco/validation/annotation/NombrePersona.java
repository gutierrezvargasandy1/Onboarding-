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

/** Nombre o apellido: solo letras y espacios, de 2 a 50 caracteres. Null se acepta (usar @NotBlank si es obligatorio). */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Size(min = 2, max = 50, message = "debe tener entre 2 y 50 caracteres")
@Pattern(regexp = Constantes.REGEX_NOMBRE, message = "solo admite letras y espacios")
public @interface NombrePersona {

    String message() default "nombre inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

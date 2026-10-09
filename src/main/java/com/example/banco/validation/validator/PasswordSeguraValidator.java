package com.example.banco.validation.validator;

import com.example.banco.util.PoliticaPassword;
import com.example.banco.validation.annotation.PasswordSegura;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/** Aplica {@link PoliticaPassword} y explica exactamente qué le falta a la contraseña. */
public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext contexto) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        List<String> faltantes = PoliticaPassword.incumplimientos(password);
        if (faltantes.isEmpty()) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        // Sin interpolación de expresiones: el texto proviene de constantes, nunca del usuario
        contexto.buildConstraintViolationWithTemplate("la contraseña requiere: " + String.join(", ", faltantes))
                .addConstraintViolation();
        return false;
    }
}

package com.example.banco.controller;

import com.example.banco.dto.response.GenericResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ValidadorCampos {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(Errors errors) {
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos" + listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }
}
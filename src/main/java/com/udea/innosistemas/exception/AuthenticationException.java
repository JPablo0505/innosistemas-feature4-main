package com.udea.innosistemas.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

//clase para manejar excepciones específicas relacionadas con la autenticación de usuarios.
//Proporciona mensajes de error claros cuando ocurren fallos de autenticación.  
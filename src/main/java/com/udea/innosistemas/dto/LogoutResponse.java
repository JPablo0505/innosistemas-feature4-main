package com.udea.innosistemas.dto;

/**
 * DTO para respuesta de logout.
 * Indica si el logout fue exitoso y proporciona un mensaje.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
public class LogoutResponse {

    private boolean success;
    private String message;

    public LogoutResponse() {
    }

    public LogoutResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

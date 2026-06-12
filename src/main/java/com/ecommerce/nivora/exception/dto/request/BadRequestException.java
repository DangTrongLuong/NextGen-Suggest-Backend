package com.ecommerce.nivora.exception.dto.request;


public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

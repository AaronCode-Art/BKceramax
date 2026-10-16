package com.ceramax.api.exception;

public class CacheUnavailableException extends RuntimeException {
    public CacheUnavailableException() {
        super("La caché está temporalmente no disponible; intente nuevamente");
    }
}

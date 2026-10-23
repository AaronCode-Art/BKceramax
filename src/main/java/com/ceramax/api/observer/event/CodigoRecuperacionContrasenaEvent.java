package com.ceramax.api.observer.event;

public record CodigoRecuperacionContrasenaEvent(
    String destinatario,
    String nombre,
    String codigo
) {}

package com.ceramax.api.dto.request;

import java.util.regex.Pattern;

public final class ContrasenaClienteValidator {

    private static final Pattern SECUENCIA_NUMERICA = Pattern.compile(
        "012|123|234|345|456|567|678|789|987|876|765|654|543|432|321|210"
    );

    private ContrasenaClienteValidator() {}

    public static boolean esValida(String contrasena) {
        if (contrasena == null || contrasena.length() < 6 || contrasena.length() > 100) {
            return false;
        }
        long digitos = contrasena.chars().filter(caracter -> caracter >= '0' && caracter <= '9').count();
        boolean mayuscula = contrasena.chars().anyMatch(caracter -> caracter >= 'A' && caracter <= 'Z');
        boolean simbolo = contrasena.codePoints().anyMatch(caracter ->
            !Character.isLetterOrDigit(caracter)
        );
        return digitos >= 6 && mayuscula && simbolo && !SECUENCIA_NUMERICA.matcher(contrasena).find();
    }
}

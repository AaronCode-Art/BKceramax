package com.ceramax.api.factory;

import com.ceramax.api.dto.response.ComprobanteDatos;
import com.ceramax.api.exception.BusinessException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ComprobanteFactory {

    public ComprobanteDatos crear(String tipo, String ruc, String razonSocial) {
        if (!"BOLETA".equals(tipo) && !"FACTURA".equals(tipo)) {
            throw new BusinessException("Tipo de comprobante inválido");
        }
        if ("FACTURA".equals(tipo)) {
            String rucNormalizado = ruc == null ? "" : ruc.trim();
            String razonSocialNormalizada = razonSocial == null ? "" : razonSocial.trim();
            if (!rucNormalizado.matches("[0-9]{11}") || razonSocialNormalizada.isEmpty()) {
                throw new BusinessException("Para FACTURA se requiere RUC válido y razón social");
            }
            return new ComprobanteDatos(tipo, rucNormalizado, razonSocialNormalizada);
        }
        return new ComprobanteDatos(tipo, null, null);
    }

    public String normalizarMetodoPago(String metodoPago) {
        String metodo = metodoPago == null ? "" : metodoPago.trim().toUpperCase(Locale.ROOT);
        if (!metodo.matches("TARJETA|YAPE|PLIN|TRANSFERENCIA|EFECTIVO")) {
            throw new BusinessException("Método de pago inválido");
        }
        return metodo;
    }
}

package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.RestablecerContrasenaClienteRequest;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.observer.event.CodigoRecuperacionContrasenaEvent;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecuperacionContrasenaClienteService {

    private static final int MAX_INTENTOS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MENSAJE_SOLICITUD =
        "Si los datos corresponden a una cuenta con correo registrado, enviaremos un código.";

    private final ClienteRepository clienteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public RecuperacionContrasenaClienteService(
        ClienteRepository clienteRepository,
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder,
        ApplicationEventPublisher eventPublisher
    ) {
        this.clienteRepository = clienteRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String solicitarCodigo(String identificador) {
        Optional<Cliente> resultado = buscarCliente(identificador);
        if (resultado.isEmpty()) {
            return MENSAJE_SOLICITUD;
        }

        Cliente cliente = resultado.get();
        if (!Boolean.TRUE.equals(cliente.getActivo())
            || cliente.getEmail() == null
            || cliente.getEmail().isBlank()) {
            return MENSAJE_SOLICITUD;
        }

        Integer solicitudesRecientes = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM recuperacion_contrasena_cliente
                WHERE cliente_id = ?
                  AND created_at > now() - interval '1 minute'
                """,
            Integer.class,
            cliente.getId()
        );

        if (solicitudesRecientes != null && solicitudesRecientes > 0) {
            return MENSAJE_SOLICITUD;
        }

        String codigo = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        OffsetDateTime creadoEn = OffsetDateTime.now();

        jdbcTemplate.update(
            """
                UPDATE recuperacion_contrasena_cliente
                SET consumed_at = now()
                WHERE cliente_id = ? AND consumed_at IS NULL
                """,
            cliente.getId()
        );

        jdbcTemplate.update(
            """
                INSERT INTO recuperacion_contrasena_cliente
                    (id, cliente_id, codigo_hash, expires_at, created_at, intentos)
                VALUES (?, ?, ?, ?, ?, 0)
                """,
            UUID.randomUUID(),
            cliente.getId(),
            hashCodigo(codigo),
            creadoEn.plusMinutes(10),
            creadoEn
        );

        eventPublisher.publishEvent(new CodigoRecuperacionContrasenaEvent(
            cliente.getEmail(),
            escaparHtml(cliente.getNombres()),
            codigo
        ));

        return MENSAJE_SOLICITUD;
    }

    @Transactional
    public boolean restablecer(RestablecerContrasenaClienteRequest request) {
        Optional<Cliente> cuenta = buscarCliente(request.identificador())
            .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()));

        if (cuenta.isEmpty()) {
            return false;
        }

        Cliente cliente = cuenta.get();

        var codigosActivos = jdbcTemplate.query(
            """
                SELECT id, codigo_hash
                FROM recuperacion_contrasena_cliente
                WHERE cliente_id = ?
                  AND consumed_at IS NULL
                  AND expires_at > now()
                  AND intentos < ?
                ORDER BY created_at DESC
                LIMIT 1
                FOR UPDATE
                """,
            (rs, rowNum) -> new CodigoActivo(
                rs.getObject("id", UUID.class),
                rs.getString("codigo_hash")
            ),
            cliente.getId(),
            MAX_INTENTOS
        );

        if (codigosActivos.isEmpty()) {
            return false;
        }

        CodigoActivo codigoActivo = codigosActivos.get(0);

        if (!MessageDigest.isEqual(
            codigoActivo.hash().getBytes(StandardCharsets.US_ASCII),
            hashCodigo(request.codigo()).getBytes(StandardCharsets.US_ASCII)
        )) {
            jdbcTemplate.update(
                """
                    UPDATE recuperacion_contrasena_cliente
                    SET intentos = intentos + 1
                    WHERE id = ? AND consumed_at IS NULL
                    """,
                codigoActivo.id()
            );
            return false;
        }

        int consumido = jdbcTemplate.update(
            """
                UPDATE recuperacion_contrasena_cliente
                SET consumed_at = now()
                WHERE id = ?
                  AND consumed_at IS NULL
                  AND expires_at > now()
                  AND intentos < ?
                """,
            codigoActivo.id(),
            MAX_INTENTOS
        );

        if (consumido != 1) {
            return false;
        }

        cliente.setPasswordHash(passwordEncoder.encode(request.nuevaContrasena()));
        clienteRepository.save(cliente);
        return true;
    }

    private Optional<Cliente> buscarCliente(String identificador) {
        String valor = identificador.trim();

        if (valor.contains("@")) {
            return clienteRepository.findByEmailIgnoreCase(valor.toLowerCase(Locale.ROOT));
        }

        if (valor.matches("[0-9]{8}")) {
            return clienteRepository.findByTipoDocumentoAndNumeroDocumento("DNI", valor);
        }

        return Optional.empty();
    }

    private static String hashCodigo(String codigo) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(codigo.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private record CodigoActivo(UUID id, String hash) {}
}

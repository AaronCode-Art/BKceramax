package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.ClienteUpdateRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.acceso.ClienteMapper;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream().map(clienteMapper::toResponse).toList();
    }

    public ClienteResponse obtenerPorId(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return clienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerMiPerfil() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Perfil de cliente no encontrado");
        }
        Cliente cliente = clienteRepository.findByEmail(authentication.getName())
            .filter(Cliente::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado"));
        return clienteMapper.toResponse(cliente);
    }

    public ClienteResponse registrar(ClienteRequest request) {
        String email = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
        String tipoDocumento = request.tipoDocumento().trim();
        String numeroDocumento = request.numeroDocumento().trim();
        if (email != null && clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El correo electrónico ya existe.");
        }
        if (clienteRepository.existsByNumeroDocumentoAndTipoDocumento(numeroDocumento, tipoDocumento)) {
            throw new ConflictException("El número de documento ya existe para ese tipo de documento.");
        }

        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setEmail(email);
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(numeroDocumento);
        if (request.password() != null && !request.password().isBlank()) {
            cliente.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        cliente.setActivo(true);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    public ClienteResponse actualizar(UUID id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String tipoDocumento = request.tipoDocumento().trim();
        String numeroDocumento = request.numeroDocumento().trim();
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("El correo electrónico ya existe.");
        }
        if (clienteRepository.existsByNumeroDocumentoAndTipoDocumentoAndIdNot(
            numeroDocumento, tipoDocumento, id
        )) {
            throw new ConflictException("El número de documento ya existe para ese tipo de documento.");
        }

        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(numeroDocumento);
        cliente.setNombres(request.nombres().trim());
        cliente.setApellidos(request.apellidos().trim());
        cliente.setEmail(email);
        cliente.setTelefono(request.telefono().trim());
        cliente.setDepartamento(request.departamento().trim());
        cliente.setProvincia(request.provincia().trim());
        cliente.setDistrito(request.distrito().trim());
        cliente.setDireccion(request.direccion().trim());
        cliente.setCodigoPostal(request.codigoPostal().trim());
        cliente.setReferencia(request.referencia().trim());
        cliente.setIdUbigeo(request.idUbigeo().trim());
        if (request.activo() != null) {
            cliente.setActivo(request.activo());
        }
        if (request.password() != null && !request.password().isBlank()) {
            cliente.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
    }
}

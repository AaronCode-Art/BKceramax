package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.UsuarioRequest;
import com.ceramax.api.dto.request.UsuarioUpdateRequest;
import com.ceramax.api.dto.response.UsuarioResponse;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.acceso.UsuarioMapper;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.repository.acceso.RolRepository;
import com.ceramax.api.repository.acceso.SucursalRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        RolRepository rolRepository,
        SucursalRepository sucursalRepository,
        UsuarioMapper usuarioMapper,
        PasswordEncoder passwordEncoder,
        EntityManager entityManager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.sucursalRepository = sucursalRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(usuarioMapper::toResponse).toList();
    }

    public UsuarioResponse obtenerPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse crear(UsuarioRequest request) {
        String codigo = request.codigo().trim();
        if (usuarioRepository.existsByCodigo(codigo)) {
            throw new ConflictException("El código de personal ya existe.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String tipoDocumento = request.tipoDocumento().trim();
        String numeroDocumento = request.numeroDocumento().trim();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El correo electrónico ya existe.");
        }
        if (usuarioRepository.existsByNumeroDocumentoAndTipoDocumento(numeroDocumento, tipoDocumento)) {
            throw new ConflictException("El número de documento ya existe para ese tipo de documento.");
        }

        Rol rol = rolRepository.findById(request.rolId())
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        Sucursal sucursal = request.sucursalId() == null ? null : sucursalRepository.findById(request.sucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setCodigo(codigo);
        usuario.setEmail(email);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNumeroDocumento(numeroDocumento);
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));

        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);
        entityManager.refresh(usuarioGuardado);
        return usuarioMapper.toResponse(usuarioGuardado);
    }

    public UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String codigo = request.codigo().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String tipoDocumento = request.tipoDocumento().trim();
        String numeroDocumento = request.numeroDocumento().trim();
        if (usuarioRepository.existsByCodigo(codigo) && !codigo.equals(usuario.getCodigo())) {
            throw new ConflictException("El código de personal ya existe.");
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("El correo electrónico ya existe.");
        }
        if (usuarioRepository.existsByNumeroDocumentoAndTipoDocumentoAndIdNot(
            numeroDocumento, tipoDocumento, id
        )) {
            throw new ConflictException("El número de documento ya existe para ese tipo de documento.");
        }

        Rol rol = rolRepository.findById(request.rolId())
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        Sucursal sucursal = request.sucursalId() == null ? null : sucursalRepository.findById(request.sucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        usuario.setCodigo(codigo);
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNumeroDocumento(numeroDocumento);
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setEmail(email);
        usuario.setTelefono(request.telefono() == null || request.telefono().isBlank() ? null : request.telefono().trim());
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        Usuario usuarioActualizado = usuarioRepository.saveAndFlush(usuario);
        entityManager.refresh(usuarioActualizado);
        return usuarioMapper.toResponse(usuarioActualizado);
    }
}

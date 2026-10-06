package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.RolRequest;
import com.ceramax.api.dto.response.RolResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.acceso.RolMapper;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.repository.acceso.RolRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    public RolService(RolRepository rolRepository, RolMapper rolMapper) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
    }

    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(rolMapper::toResponse).toList();
    }

    public RolResponse obtenerPorId(UUID id) {
        Rol rol = rolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        return rolMapper.toResponse(rol);
    }

    public RolResponse crear(RolRequest request) {
        if (rolRepository.findByCodigo(request.codigo()).isPresent()) {
            throw new BusinessException("El código del rol ya existe");
        }
        Rol rol = rolMapper.toEntity(request);
        return rolMapper.toResponse(rolRepository.save(rol));
    }

    public RolResponse actualizar(UUID id, RolRequest request) {
        Rol rol = rolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        rolMapper.updateEntityFromRequest(request, rol);
        return rolMapper.toResponse(rolRepository.save(rol));
    }
}

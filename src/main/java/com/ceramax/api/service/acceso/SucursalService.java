package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.SucursalRequest;
import com.ceramax.api.dto.response.SucursalResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.acceso.SucursalMapper;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.repository.acceso.SucursalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final SucursalMapper sucursalMapper;

    public SucursalService(SucursalRepository sucursalRepository, SucursalMapper sucursalMapper) {
        this.sucursalRepository = sucursalRepository;
        this.sucursalMapper = sucursalMapper;
    }

    public List<SucursalResponse> listar() {
        return sucursalRepository.findAll().stream().map(sucursalMapper::toResponse).toList();
    }

    public SucursalResponse obtenerPorId(UUID id) {
        Sucursal sucursal = sucursalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        return sucursalMapper.toResponse(sucursal);
    }

    public SucursalResponse crear(SucursalRequest request) {
        if (sucursalRepository.findByCodigo(request.nombre()).isPresent()) {
            throw new BusinessException("La sucursal ya existe");
        }
        Sucursal sucursal = sucursalMapper.toEntity(request);
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    public SucursalResponse actualizar(UUID id, SucursalRequest request) {
        Sucursal sucursal = sucursalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        sucursalMapper.updateEntityFromRequest(request, sucursal);
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }
}

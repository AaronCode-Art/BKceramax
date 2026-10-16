package com.ceramax.api.service.inventario;

import com.ceramax.api.dto.request.AlmacenRequest;
import com.ceramax.api.dto.response.AlmacenResponse;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.inventario.AlmacenMapper;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.inventario.Almacen;
import com.ceramax.api.repository.acceso.SucursalRepository;
import com.ceramax.api.repository.inventario.AlmacenRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlmacenService {

    private final AlmacenRepository almacenRepository;
    private final SucursalRepository sucursalRepository;
    private final EntityManager entityManager;
    private final AlmacenMapper almacenMapper;

    public AlmacenService(
        AlmacenRepository almacenRepository,
        SucursalRepository sucursalRepository,
        EntityManager entityManager,
        AlmacenMapper almacenMapper
    ) {
        this.almacenRepository = almacenRepository;
        this.sucursalRepository = sucursalRepository;
        this.entityManager = entityManager;
        this.almacenMapper = almacenMapper;
    }

    @Transactional(readOnly = true)
    public List<AlmacenResponse> listarActivos() {
        return almacenRepository.findByActivoTrueOrderByNombreAsc().stream().map(almacenMapper::toResponse).toList();
    }

    public AlmacenResponse crear(AlmacenRequest request) {
        Almacen almacen = new Almacen();
        applyRequest(request, almacen);
        almacenRepository.saveAndFlush(almacen);
        entityManager.refresh(almacen);
        return almacenMapper.toResponse(almacen);
    }

    public AlmacenResponse actualizar(UUID id, AlmacenRequest request) {
        Almacen almacen = almacenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado"));
        applyRequest(request, almacen);
        almacenRepository.saveAndFlush(almacen);
        entityManager.refresh(almacen);
        return almacenMapper.toResponse(almacen);
    }

    public void desactivar(UUID id) {
        Almacen almacen = almacenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado"));
        almacen.setActivo(false);
        almacenRepository.saveAndFlush(almacen);
        entityManager.refresh(almacen);
    }

    private void applyRequest(AlmacenRequest request, Almacen almacen) {
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
            .filter(Sucursal::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal activa no encontrada"));
        almacen.setNombre(request.nombre());
        almacen.setSucursal(sucursal);
        if (request.activo() != null) {
            almacen.setActivo(request.activo());
        } else if (almacen.getId() == null) {
            almacen.setActivo(true);
        }
    }
}

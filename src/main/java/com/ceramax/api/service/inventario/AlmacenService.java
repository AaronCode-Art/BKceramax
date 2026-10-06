package com.ceramax.api.service.inventario;

import com.ceramax.api.dto.request.AlmacenRequest;
import com.ceramax.api.dto.response.AlmacenResponse;
import com.ceramax.api.exception.ResourceNotFoundException;
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

    public AlmacenService(
        AlmacenRepository almacenRepository,
        SucursalRepository sucursalRepository,
        EntityManager entityManager
    ) {
        this.almacenRepository = almacenRepository;
        this.sucursalRepository = sucursalRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<AlmacenResponse> listarActivos() {
        return almacenRepository.findByActivoTrueOrderByNombreAsc().stream().map(this::toResponse).toList();
    }

    public AlmacenResponse crear(AlmacenRequest request) {
        Almacen almacen = new Almacen();
        applyRequest(request, almacen);
        almacenRepository.saveAndFlush(almacen);
        entityManager.refresh(almacen);
        return toResponse(almacen);
    }

    public AlmacenResponse actualizar(UUID id, AlmacenRequest request) {
        Almacen almacen = almacenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado"));
        applyRequest(request, almacen);
        almacenRepository.saveAndFlush(almacen);
        entityManager.refresh(almacen);
        return toResponse(almacen);
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

    private AlmacenResponse toResponse(Almacen almacen) {
        return new AlmacenResponse(
            almacen.getId(),
            almacen.getCodigo(),
            almacen.getNombre(),
            almacen.getSucursal().getId(),
            almacen.getSucursal().getNombre(),
            almacen.getActivo(),
            almacen.getCreatedAt(),
            almacen.getUpdatedAt()
        );
    }
}

package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.RutaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RutaRepository extends JpaRepository<RutaEntity, Long> {

    /** Ruta del endpoint central (sin proveedor); {@code codigo} = nombre del MessageType. */
    Optional<RutaEntity> findFirstByCanalProveedorIsNullAndTipoMensaje_CodigoAndEliminadoFalse(String codigo);

    /** Ruta propia de un proveedor. */
    Optional<RutaEntity> findFirstByCanalProveedor_CodigoAndTipoMensaje_CodigoAndEliminadoFalse(String proveedorCodigo, String codigo);
}

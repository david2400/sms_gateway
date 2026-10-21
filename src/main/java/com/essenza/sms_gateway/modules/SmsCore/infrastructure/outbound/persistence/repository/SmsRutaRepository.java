package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsRutaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsRutaRepository extends JpaRepository<SmsRutaEntity, Long> {

    /** Ruta del endpoint central (sin proveedor); {@code codigo} = nombre del MessageType. */
    Optional<SmsRutaEntity> findFirstByProveedorIsNullAndTipoMensaje_CodigoAndActivoTrue(String codigo);

    /** Ruta propia de un proveedor. */
    Optional<SmsRutaEntity> findFirstByProveedor_CodigoAndTipoMensaje_CodigoAndActivoTrue(String proveedorCodigo, String codigo);
}

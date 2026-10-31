package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository;

import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.RutaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<RutaEntity, Long> {

    /** Ruta del endpoint central (sin proveedor); {@code codigo} = nombre del MessageType. */
    Optional<RutaEntity> findFirstByCanalProveedorIsNullAndTipoMensaje_CodigoAndEliminadoFalse(String codigo);

    /** Ruta propia de un proveedor. */
    Optional<RutaEntity> findFirstByCanalProveedor_CodigoAndTipoMensaje_CodigoAndEliminadoFalse(String proveedorCodigo, String codigo);

    /** Ruta activa pedida por código (sin importar mayúsculas). @SoftDelete ya excluye las inactivas. */
    Optional<RutaEntity> findFirstByCodigoIgnoreCase(String codigo);

    /**
     * Estado ({@code eliminado}) de la ruta de SMS con ese id o código, incluidas las inactivas que @SoftDelete oculta:
     * distingue "no existe" de "está inactiva". Primero la coincidencia por id.
     */
    @Query(value = "SELECT r.eliminado FROM CanalRuta r WHERE r.canal = 'SMS' AND (r.id_ruta = :id OR UPPER(r.codigo) = UPPER(:codigo)) "
            + "ORDER BY CASE WHEN r.id_ruta = :id THEN 0 ELSE 1 END", nativeQuery = true)
    List<Boolean> findEstado(@Param("id") Long id, @Param("codigo") String codigo);
}

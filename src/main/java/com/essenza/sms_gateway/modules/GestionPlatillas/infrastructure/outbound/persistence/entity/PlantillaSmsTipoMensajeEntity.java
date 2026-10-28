package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Tipos de mensaje de SMS ({@code CanalTipoMensaje}, canal = 'SMS') vistos por el módulo de plantillas: solo lectura
 * (el catálogo lo administra notificacion). Nombre de entidad propio para no chocar con el de SmsCore (misma tabla).
 */
@Entity(name = "PlantillaSmsTipoMensaje")
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalTipoMensaje")
@Immutable
@Getter
@NoArgsConstructor
public class PlantillaSmsTipoMensajeEntity extends AuditInfo {

    @Id
    @Column(name = "id_tipo_mensaje")
    private Long idSmsTipoMensaje;

    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}

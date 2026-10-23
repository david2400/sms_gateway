package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * {@code SmsTipoMensaje} vista por el módulo de plantillas: solo lectura (el catálogo lo administra SmsAdmin).
 * Nombre de entidad propio para no chocar con las de SmsCore/SmsAdmin que mapean la misma tabla.
 */
@Entity(name = "PlantillaSmsTipoMensaje")
@Table(name = "SmsTipoMensaje")
@Immutable
@Getter
@NoArgsConstructor
public class PlantillaSmsTipoMensajeEntity extends AuditInfo {

    @Id
    @Column(name = "id_sms_tipo_mensaje")
    private Long idSmsTipoMensaje;

    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;
}

package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.SmsTipoMensajeEntity;
import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Servicio habilitado de un proveedor ({@code SmsProveedorServicio}) = (proveedor, tipo de mensaje).
 * Si no existe la fila, el proveedor no atiende ese tipo.
 */
@Entity
@Table(name = "SmsProveedorServicio",
        uniqueConstraints = @UniqueConstraint(name = "uq_sms_proveedor_servicio", columnNames = {"proveedor_id", "tipo_mensaje_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsProveedorServicioEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_proveedor_servicio")
    private Long idSmsProveedorServicio;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private SmsProveedorEntity proveedor;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_mensaje_id", nullable = false)
    private SmsTipoMensajeEntity tipoMensaje;

}

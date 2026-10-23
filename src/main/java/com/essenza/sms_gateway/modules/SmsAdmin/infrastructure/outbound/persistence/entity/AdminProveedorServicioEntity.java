package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla {@code SmsProveedorServicio} vista por administración: tipos de mensaje que atiende cada proveedor.
 * Tabla de relación sin borrado lógico: quitar un servicio borra la fila.
 */
@Entity(name = "AdminProveedorServicio")
@Table(name = "SmsProveedorServicio")
@Getter
@Setter
@NoArgsConstructor
public class AdminProveedorServicioEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_proveedor_servicio")
    private Long idSmsProveedorServicio;

    // EAGER: Hibernate no permite LAZY hacia entidades con @SoftDelete (AdminProveedorEntity)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private AdminProveedorEntity proveedor;

    // EAGER: Hibernate no permite LAZY hacia entidades con @SoftDelete (AdminTipoMensajeEntity)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_mensaje_id", nullable = false)
    private AdminTipoMensajeEntity tipoMensaje;
}

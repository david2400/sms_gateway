package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import jakarta.persistence.UniqueConstraint;
import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Proveedor de SMS: filas {@code canal = 'SMS'} de {@code CanalProveedor} (las administra notificacion).
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalProveedor",
        uniqueConstraints = @UniqueConstraint(name = "uq_canal_proveedor", columnNames = {"canal", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsProveedorEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idSmsProveedor;

    /** Debe coincidir con {@code SmsSenderPort.providerCode()}. */
    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs = 2000;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs = 5000;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}

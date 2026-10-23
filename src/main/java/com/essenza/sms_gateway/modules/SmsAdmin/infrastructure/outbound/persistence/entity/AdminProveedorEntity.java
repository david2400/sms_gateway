package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla {@code sms_proveedor} vista por el módulo de administración.
 * Mapea la misma tabla que usa SmsCore; el nombre de entidad es distinto para no chocar en JPA.
 */
@Entity(name = "AdminProveedor")
@Table(name = "SmsProveedor")
@Getter
@Setter
@NoArgsConstructor
public class AdminProveedorEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_proveedor")
    private Long idSmsProveedor;

    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs;

}

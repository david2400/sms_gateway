package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

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
 * Proveedor de SMS ({@code sms_proveedor}).
 */
@Entity
@Table(name = "sms_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class SmsProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Debe coincidir con {@code SmsSenderPort.providerCode()}. */
    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @Column(name = "connect_timeout_ms", nullable = false)
    private Integer connectTimeoutMs = 2000;

    @Column(name = "read_timeout_ms", nullable = false)
    private Integer readTimeoutMs = 5000;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}

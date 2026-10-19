package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

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

/**
 * Ruta ({@code sms_ruta}) = (proveedor, tipo de mensaje).
 * {@code proveedor == null} es la ruta del endpoint central.
 */
@Entity
@Table(name = "sms_ruta")
@Getter
@Setter
@NoArgsConstructor
public class SmsRutaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private SmsProveedorEntity proveedor;

    @Column(name = "tipo_mensaje", length = 30, nullable = false)
    private String tipoMensaje;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;
}

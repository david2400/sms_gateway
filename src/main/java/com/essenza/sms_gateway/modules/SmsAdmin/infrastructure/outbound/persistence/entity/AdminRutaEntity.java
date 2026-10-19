package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

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
 * Tabla {@code sms_ruta} vista por el módulo de administración. {@code proveedor == null} = ruta central.
 */
@Entity(name = "AdminRuta")
@Table(name = "SmsRuta")
@Getter
@Setter
@NoArgsConstructor
public class AdminRutaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private AdminProveedorEntity proveedor;

    @Column(name = "tipo_mensaje", length = 30, nullable = false)
    private String tipoMensaje;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}

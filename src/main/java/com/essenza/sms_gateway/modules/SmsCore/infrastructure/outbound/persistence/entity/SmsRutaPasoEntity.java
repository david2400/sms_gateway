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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paso de una ruta ({@code sms_ruta_paso}): orden, cuenta e intentos.
 */
@Entity
@Table(name = "sms_ruta_paso",
        uniqueConstraints = @UniqueConstraint(name = "uq_sms_ruta_paso", columnNames = {"ruta_id", "orden"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsRutaPasoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private SmsRutaEntity ruta;

    /** 1 = principal, 2.. = fallback. */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private SmsCuentaEntity cuenta;

    @Column(name = "max_intentos", nullable = false)
    private Integer maxIntentos = 3;

    @Column(name = "espera_ms", nullable = false)
    private Integer esperaMs = 500;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;
}

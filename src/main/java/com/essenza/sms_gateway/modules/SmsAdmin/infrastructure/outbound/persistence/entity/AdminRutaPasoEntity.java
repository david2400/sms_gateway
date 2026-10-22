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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla {@code sms_ruta_paso} vista por el módulo de administración.
 */
@Entity(name = "AdminRutaPaso")
@Table(name = "SmsRutaPaso",
        uniqueConstraints = @UniqueConstraint(name = "uq_sms_ruta_paso", columnNames = {"ruta_id", "orden"}))
@Getter
@Setter
@NoArgsConstructor
public class AdminRutaPasoEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_ruta_paso")
    private Long idSmsRutaPaso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private AdminRutaEntity ruta;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private AdminCuentaEntity cuenta;

    @Column(name = "max_intentos", nullable = false)
    private Integer maxIntentos;

    @Column(name = "espera_ms", nullable = false)
    private Integer esperaMs;
}

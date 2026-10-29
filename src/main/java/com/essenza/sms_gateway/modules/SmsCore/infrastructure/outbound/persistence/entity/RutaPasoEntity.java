package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

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
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paso de una ruta (filas {@code canal = 'SMS'} de {@code CanalRutaPaso}): orden, cuenta e intentos.
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalRutaPaso",
        uniqueConstraints = @UniqueConstraint(name = "uq_canal_ruta_paso", columnNames = {"ruta_id", "orden"}))
@Getter
@Setter
@NoArgsConstructor
public class RutaPasoEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta_paso")
    private Long idRutaPaso;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private RutaEntity ruta;

    /** 1 = principal, 2.. = fallback. */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "canal_cuenta_id", nullable = false)
    private CanalCuentaEntity canalCuenta;

    @Column(name = "max_intentos", nullable = false)
    private Integer maxIntentos = 3;

    @Column(name = "espera_ms", nullable = false)
    private Integer esperaMs = 500;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}

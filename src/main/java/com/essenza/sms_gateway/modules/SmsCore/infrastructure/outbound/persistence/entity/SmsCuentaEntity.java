package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import jakarta.persistence.UniqueConstraint;
import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de un proveedor: filas {@code canal = 'SMS'} de {@code CanalCuenta} (las administra notificacion).
 * La contraseña se guarda cifrada en {@code claveCifrada} (AES-256-GCM, llave en SMS_CRYPTO_KEY) o, si no hay,
 * se lee de la variable de entorno indicada en {@code secretRef}.
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalCuenta",
        uniqueConstraints = @UniqueConstraint(name = "uq_canal_cuenta", columnNames = {"canal", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsCuentaEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Long idSmsCuenta;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private SmsProveedorEntity proveedor;

    @Column(name = "codigo", length = 50, nullable = false)
    private String codigo;

    @Column(name = "usuario", length = 320)
    private String usuario;

    /** Nombre de la variable de entorno / propiedad con la contraseña (alternativa a {@code claveCifrada}). */
    @Column(name = "secret_ref", length = 100)
    private String secretRef;

    /** Contraseña cifrada ({@code v1:...}); tiene prioridad sobre {@code secretRef}. */
    @Column(name = "clave_cifrada", length = 4000)
    private String claveCifrada;

    @Column(name = "remitente", length = 320)
    private String remitente;

    /** JSON plano clave→valor con datos propios del proveedor. */
    @Lob
    @Column(name = "parametros_json", columnDefinition = "LONGTEXT")
    private String parametrosJson;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}

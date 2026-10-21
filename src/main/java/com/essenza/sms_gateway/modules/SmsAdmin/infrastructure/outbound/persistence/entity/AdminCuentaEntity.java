package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla {@code sms_cuenta} vista por el módulo de administración.
 * Mapea la misma tabla que usa SmsCore; el nombre de entidad es distinto para no chocar en JPA.
 * La contraseña se guarda cifrada en {@code claveCifrada}; {@code secretRef} (variable de entorno) es la alternativa.
 */
@Entity(name = "AdminCuenta")
@Table(name = "SmsCuenta")
@Getter
@Setter
@NoArgsConstructor
public class AdminCuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_cuenta")
    private Long idSmsCuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private AdminProveedorEntity proveedor;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "usuario", length = 100)
    private String usuario;

    /** Nombre de la variable de entorno / propiedad con la contraseña. */
    @Column(name = "secret_ref", length = 100)
    private String secretRef;

    /** Contraseña cifrada ({@code v1:...}); nunca sale en las respuestas. */
    @Column(name = "clave_cifrada", length = 500)
    private String claveCifrada;

    @Column(name = "remitente", length = 20)
    private String remitente;

    /** JSON plano clave→valor con datos propios del proveedor. */
    @Lob
    @Column(name = "parametros_json")
    private String parametrosJson;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;
}

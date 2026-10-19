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
 * Tabla {@code sms_cuenta} vista por el módulo de administración.
 * {@code secretRef} es el NOMBRE de la variable de entorno con la contraseña, nunca la contraseña.
 */
@Entity(name = "AdminCuenta")
@Table(name = "sms_cuenta")
@Getter
@Setter
@NoArgsConstructor
public class AdminCuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private AdminProveedorEntity proveedor;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "secret_ref", length = 100, nullable = false)
    private String secretRef;

    @Column(name = "remitente", length = 20)
    private String remitente;

    @Column(name = "parametros_json", columnDefinition = "NVARCHAR(MAX)")
    private String parametrosJson;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}

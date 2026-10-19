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
 * Cuenta de un proveedor ({@code sms_cuenta}).
 * La contraseña NO se guarda: {@code secretRef} es el nombre de la variable de entorno que la contiene.
 */
@Entity
@Table(name = "sms_cuenta")
@Getter
@Setter
@NoArgsConstructor
public class SmsCuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private SmsProveedorEntity proveedor;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "usuario", length = 100)
    private String usuario;

    /** Nombre de la variable de entorno / propiedad con la contraseña. */
    @Column(name = "secret_ref", length = 100, nullable = false)
    private String secretRef;

    @Column(name = "remitente", length = 20)
    private String remitente;

    /** JSON plano clave→valor con datos propios del proveedor. */
    @Column(name = "parametros_json", columnDefinition = "NVARCHAR(MAX)")
    private String parametrosJson;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;
}

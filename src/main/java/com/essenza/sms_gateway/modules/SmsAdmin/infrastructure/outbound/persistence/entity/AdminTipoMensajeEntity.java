package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla {@code sms_tipo_mensaje}: catálogo de tipos de mensaje y sus capacidades.
 * {@code codigo} es el valor que viaja en {@code sms_ruta.tipo_mensaje}.
 */
@Entity(name = "AdminTipoMensaje")
@Table(name = "SmsTipoMensaje")
@Getter
@Setter
@NoArgsConstructor
public class AdminTipoMensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_tipo_mensaje")
    private Long idSmsTipoMensaje;

    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "canal", length = 20, nullable = false)
    private String canal;

    @Column(name = "otp", nullable = false)
    private Boolean otp;

    @Column(name = "masivo", nullable = false)
    private Boolean masivo;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}

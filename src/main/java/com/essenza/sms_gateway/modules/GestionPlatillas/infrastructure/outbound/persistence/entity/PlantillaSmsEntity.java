package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla SMS ({@code SmsPlantilla}). Única entidad de la tabla: la usan la administración y el motor
 * de plantillas. Se activa/desactiva con {@code activo} (no se borra: el log de envíos guarda su código).
 */
@Entity(name = "PlantillaSms")
@Table(name = "SmsPlantilla")
@Getter
@Setter
@NoArgsConstructor
public class PlantillaSmsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_plantilla")
    private Long idSmsPlantilla;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /** Tipo por defecto (null = lo define el request). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_mensaje_id")
    private PlantillaSmsTipoMensajeEntity tipoMensaje;

    @Column(name = "contenido", length = 1600, nullable = false)
    private String contenido;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PlantillaSmsVariableEntity> variables = new ArrayList<>();
}

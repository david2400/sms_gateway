package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity;

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

/** Variable declarada de una plantilla ({@code SmsPlantillaVariable}). */
@Entity(name = "PlantillaSmsVariable")
@Table(name = "SmsPlantillaVariable",
        uniqueConstraints = @UniqueConstraint(name = "uq_sms_plantilla_variable", columnNames = {"plantilla_id", "nombre"}))
@Getter
@Setter
@NoArgsConstructor
public class PlantillaSmsVariableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_plantilla_variable")
    private Long idSmsPlantillaVariable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private PlantillaSmsEntity plantilla;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    /** TEXTO, ENTERO, DECIMAL, MONEDA, FECHA, HORA, URL. */
    @Column(name = "tipo_dato", length = 20, nullable = false)
    private String tipoDato;

    @Column(name = "obligatoria", nullable = false)
    private Boolean obligatoria;

    @Column(name = "longitud_max")
    private Integer longitudMax;

    @Column(name = "patron", length = 200)
    private String patron;

    @Column(name = "valor_defecto", length = 200)
    private String valorDefecto;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}

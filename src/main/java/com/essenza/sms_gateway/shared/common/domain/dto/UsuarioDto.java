package com.essenza.sms_gateway.shared.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class UsuarioDto {
    private Integer codigo;
    private String cedula;
    private String nombre;
    private String usuario;
    private String clave;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private LocalDate expiraClave;

    private String area;
    private String cargo;
    private String mensaje;
    private String email;
    private String emailInterno;
    private String telefono;
    private String usuarioEdificio;
    private String claveEdificio;
    private String usuarioCvc;
    private String claveCvc;
    private String usuarioBosque;
    private String claveBosque;
    private Boolean internet;
    private String estado;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private LocalDate fechaAdd;
}

package com.essenza.sms_gateway.shared.common.domain.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@Data
public class BaseEntity extends AuditDatesEntity {
    @Column(name = "usrCrea", length = 50)
    private String usrCrea;

    @Column(name = "usrMod", length = 50)
    private String usrMod;

//    @PrePersist
//    public void prePersist() {
//        InformacionUsuarioDto helpersUsuarios = HelpersUsuarios.getInstance();
//        this.usrCrea = helpersUsuarios.getUsuario();
//    }
//
//    @PreUpdate
//    public void prePersistUpdate() {
//        InformacionUsuarioDto helpersUsuarios = HelpersUsuarios.getInstance();
//        this.usrMod = helpersUsuarios.getUsuario();
//    }

}

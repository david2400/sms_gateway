package com.essenza.sms_gateway.shared.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SoftDelete(columnName = "eliminado")
public class AuditInfo {
    @Column(name = "eliminado", insertable = false, updatable = false)
    private boolean eliminado;

    @Column(name = "usr_crea", nullable = false, length = 50)
    private Integer usrCrea;

    @Column(name = "usr_mod", length = 50)
    private Integer usrMod;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    // @Column(name = "deleted_at")
    // @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    // @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss a")
    // @JsonInclude(JsonInclude.Include.NON_EMPTY)
    // private Instant deletedAt;

    // @PreRemove
    // public void preRemove() {
    // this.deletedAt = Instant.now();
    // }

    @PrePersist
    public void prePersist() {
        // InformacionUsuarioDto helpersUsuarios = HelpersUsuarios.getInstance();
        this.usrCrea = 1;
    }

    @PreUpdate
    public void prePersistUpdate() {
        // InformacionUsuarioDto helpersUsuarios = HelpersUsuarios.getInstance();
        this.usrMod = 1;
    }

}
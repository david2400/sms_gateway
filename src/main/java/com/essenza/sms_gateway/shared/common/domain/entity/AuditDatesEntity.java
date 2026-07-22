package com.essenza.sms_gateway.shared.common.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
@Data
public class AuditDatesEntity {
    @Column(name = "createdAt", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updatedAt", insertable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "deletedAt")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss",timezone = "UTC")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Instant deletedAt;
}

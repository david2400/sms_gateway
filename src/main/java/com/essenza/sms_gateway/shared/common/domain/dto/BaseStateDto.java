package com.essenza.sms_gateway.shared.common.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseStateDto extends AuditDatesDto implements Serializable {
    private String estado;

    @Schema(hidden = true)
    private String usrCrea;

    @Schema(hidden = true)
    private String usrMod;


}

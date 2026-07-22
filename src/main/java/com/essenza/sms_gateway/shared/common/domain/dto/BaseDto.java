package com.essenza.sms_gateway.shared.common.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseDto extends AuditDatesDto implements Serializable {
    @Schema(hidden = true)
    private String usrCrea;

    @Schema(hidden = true)
    private String usrMod;


}

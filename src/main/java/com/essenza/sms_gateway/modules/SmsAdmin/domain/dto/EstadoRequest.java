package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.shared.common.domain.dto.AuditInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Activar o desactivar: eliminado=true desactiva, eliminado=false activa")
public class EstadoRequest extends AuditInfoDto {
}

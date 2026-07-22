package com.essenza.sms_gateway.shared.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformacionUsuarioDto implements Serializable {
	@Schema(hidden = true)
	Integer id;

	@JsonProperty("cedula")
	private String cedula;

	private String usuario;

	@JsonProperty("firts_name")
	String firtsName;
	@JsonProperty("last_name")
	String lastName;
	String email;
	Integer codigo;
	@JsonProperty("create_at")
	String createAt;
	@JsonProperty("update_at")
	String updateAt;
	@JsonProperty("email_verified_at")
	String emailVerifiedAt;



}

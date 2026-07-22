package com.essenza.sms_gateway.shared.common.response.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Response<T> {

	private Map<String, Object> headers;
	private Map<String, Object> info;
	private Map<String, Object> result;
	private Map<String, Object> errors;
	private int status;
	private String message;
	private Timestamp timestamp;
	private String token = "";

	
	public Response(Map<String, Object> headers, Page<T> entities, Integer status, String message, Map<String, Object> errors, String token) {
		this.headers = headers;

		this.info = new HashMap<>();
		this.info.put("totalItems", entities != null ? entities.getTotalElements() : 0);

        int totalPages = entities != null ? entities.getTotalPages() : 0;

        Map<String, Object> result = new HashMap<>();

		if(totalPages != 0) {
			Integer prev = null, next = null;
			if(entities.hasPrevious()) {
				prev = entities.getNumber() - 1;
			}
	        if(entities.hasNext()) {
				next = entities.getNumber() + 1;
			}

	        info.put("itemsPerPage", entities.getSize());
			info.put("pages", totalPages);
			info.put("next", next);
			info.put("prev", prev);

		    result.put("items", entities.getContent());
		}

		this.result = result;
		this.errors = errors != null ? errors : new HashMap<>();
		this.status = status;
		this.message = message;
		this.timestamp = new Timestamp((new Date()).getTime());
		this.token = token;
	}

}

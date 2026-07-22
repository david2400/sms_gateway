package com.essenza.sms_gateway.shared.common.response.service;

import com.essenza.sms_gateway.shared.common.functions.TriFunction;
import com.essenza.sms_gateway.shared.common.modelmapper.Mapper;
import com.essenza.sms_gateway.shared.common.request.RequestType;
import com.essenza.sms_gateway.shared.common.response.entity.Response;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResponseService<T, D, E> {

	private static ResponseService responseService;

	private ResponseService() {}

	public static ResponseService create() {
		if(responseService == null) {
			responseService = new ResponseService();
		}

		return responseService;
	}


	public ResponseEntity<Response> handlePagedResponse(RequestType requestType, Page<E> page, String token) {
		if (page.getContent() == null || page.getContent().isEmpty()) {
			return ResponseService.create().getErrorResponse(requestType, page, token);
		}

		return ResponseService.create().getDoneResponse(requestType, page, token);
	}


	public ResponseEntity<Response> handlePagedResponse(RequestType requestType, Page<E> entityPage, Class<D> dtoClass, String token) {
		if (entityPage.getContent() == null || entityPage.getContent().isEmpty()) {
			return ResponseService.create().getErrorResponse(requestType, entityPage, token);
		}

		Page<D> page = dtoPage.apply(null, entityPage, dtoClass);

		return ResponseService.create().getDoneResponse(requestType, page, token);
	}


	public TriFunction<D, Page<E>, Class<D>, Page<D>> dtoPage = (dto, entityPage, dtoClass) -> {
		List<D> list;
		Pageable pageable;
		Page<D> page = null;

		if (dto != null) {
			list = Mapper.create().getList(dto);
			pageable = PageRequest.of(0, 1);
			page = new PageImpl<>(list, pageable, 1);
		} else {
			list = Mapper.create().convertToListDto(entityPage.getContent(), dtoClass);
			pageable = entityPage.getPageable();
			page = new PageImpl<>(list, pageable, entityPage.getTotalElements());
		}

		return page;
	};


	public ResponseEntity<Response> getValidationResponse(RequestType requestType, Page<T> entities, Map<String, Object> validation, String token) {
		Response<T> response = new Response<>(getResponseHeaders(), entities, getErrorCode(requestType),
				getValidationInfo(getHttpStatusError(requestType)), validation, token);

		return new ResponseEntity<>(response, getHttpStatusError(requestType));
	}


	public ResponseEntity<Response> getErrorResponse(RequestType requestType, Page<T> entities, String token) {
		Response<T> response = new Response<>(getResponseHeaders(), entities, getErrorCode(requestType),
				getMessageError(getHttpStatusError(requestType)), getOperationError(requestType), token);

		return new ResponseEntity<>(response, getHttpStatusError(requestType));
	}


	public ResponseEntity<Response> getDoneResponse(RequestType requestType, Page<T> entities, String token) {
		Response<T> response = new Response<>(getResponseHeaders(), entities, 200, getMessageDone(requestType), null, token);

		return new ResponseEntity<>(response, getHttpStatusDone(requestType));
	}


	private Map<String, Object> getResponseHeaders() {
		String appDescription = "Draco Api";
		Map<String, Object> headerJson = new HashMap<>();
		headerJson.put("x-custom-header", appDescription);

		Map<String, Object> headers = new HashMap<>();
        for (Map.Entry<String, Object> entry : headerJson.entrySet()) {
        	headers.put(entry.getKey(), entry.getValue());
        }

		return headers;
	}


	private int getErrorCode(RequestType operation) {
		switch (operation) {
		case LOGIN: return 401;
		case VALIDATION: return 422;
		case UPLOAD: return 401;
		case CREATE: return 409;
		default: return 409;
		}
	}


	private HttpStatus getHttpStatusError(RequestType operation) {
		switch (operation) {
		case LOGIN: return HttpStatus.UNAUTHORIZED;
		case VALIDATION: return HttpStatus.UNPROCESSABLE_ENTITY;
		case UPLOAD: return HttpStatus.BAD_REQUEST;
		case CREATE: return HttpStatus.CONFLICT;
		case STOCK: return HttpStatus.CONFLICT;
		default: return HttpStatus.NOT_FOUND;
		}
	}


	private String getValidationInfo(HttpStatus internalServerError) {
		return internalServerError.getReasonPhrase();
	}


	private String getMessageError(HttpStatus internalServerError) {
		return internalServerError.getReasonPhrase() + " Error";
	}


	private HttpStatus getHttpStatusDone(RequestType operation) {
		return HttpStatus.OK;
	}


	private String getMessageDone(RequestType operation) {
		switch (operation) {
		case LOGIN: return "Valid credentials";
		case UPLOAD: return "New resource created";
		case CREATE: return "New resource created";
		case READ: return "Resource list";
		case UPDATE: return "Resource updated";
		case DELETE: return "Resource disabled";
		case ENABLE: return "Resource enabled";
		case STOCK: return "Resource updated";
		default: return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase() + " Error";
		}
	}


	private Map<String, Object> getOperationError(RequestType operation) {
		Map<String, Object> error = new HashMap<>();

		switch (operation) {
		case LOGIN:
			error.put("authorization error", "Invalid credentials");
			break;
		case UPLOAD:
			error.put("creating error", "Bad request");
			break;
		case CREATE:
			error.put("creating error", "duplication of information or the entity on which it depends does not exist");
			break;
		case STOCK:
			error.put("error", "Quantity no valid");
			break;
		default:
			error.put("error", "Resource not found!");
			break;
		}

		Map<String, Object> errors = new HashMap<>();
        for (Map.Entry<String, Object> entry : error.entrySet()) {
        	errors.put(entry.getKey(), entry.getValue());
        }

		return errors;
	}

}

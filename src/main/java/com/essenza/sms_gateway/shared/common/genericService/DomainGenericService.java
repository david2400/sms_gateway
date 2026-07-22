package com.essenza.sms_gateway.shared.common.genericService;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


public interface DomainGenericService<T, ID extends Serializable> {

	Object save(T entity);

	Page<T> getAll(int pageNumber, int pageSize, String sort);

	Object update(T entity);

	T delete(ID id);

	String getEntityIdValue(T entity);

	List<Map<String, Object>> getAllProyection(String search);

}

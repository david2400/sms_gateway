package com.essenza.sms_gateway.shared.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;

@NoRepositoryBean
public interface JpaRepositoryExtended <T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
	
	List<Map<String, Object>> find(String search);
	
}

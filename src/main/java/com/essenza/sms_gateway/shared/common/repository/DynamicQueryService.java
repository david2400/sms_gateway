//package com.essenza.sms_gateway.shared.common.repository;
//
//import com.essenza.sms_gateway.shared.common.service.GeneralEntityService;
//import org.springframework.stereotype.Service;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.persistence.TypedQuery;
//import jakarta.persistence.criteria.CriteriaBuilder;
//import jakarta.persistence.criteria.CriteriaQuery;
//import jakarta.persistence.criteria.Predicate;
//import jakarta.persistence.criteria.Root;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class DynamicQueryService<T> {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//
//    public List<T> findByUniqueAttributes(Class<T> entityClass, Map<String, Object> attributes) {
//    	CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//        CriteriaQuery<T> query = cb.createQuery(entityClass);
//        Root<T> root = query.from(entityClass);
//
//        List<Predicate> predicates = new ArrayList<>();
//        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
//            if (entry.getValue() != null) {
//                predicates.add(cb.equal(root.get(entry.getKey()), entry.getValue()));
//            }
//        }
//
//        if (!predicates.isEmpty()) {
//            query.where(cb.or(predicates.toArray(new Predicate[0])));
//        } else {
//            return new ArrayList<>();
//        }
//
//        TypedQuery<T> typedQuery = entityManager.createQuery(query);
//
//        return typedQuery.getResultList();
//    }
//
//
//	public List<T> searchByRelatedEntityValues(T entity) {
//	    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//	    CriteriaQuery<T> query = cb.createQuery((Class<T>) entity.getClass());
//	    Root<T> root = query.from((Class<T>) entity.getClass());
//
//	    List<Predicate> predicates = new ArrayList<>();
//
//	    GeneralEntityService entityService = GeneralEntityService.create();
//	    List<Field> attributes = entityService.getRelationshipAttributes(entity.getClass(), entity);
//
//	    for (Field attr : attributes) {
//	        try {
//	            attr.setAccessible(true);
//	            Object relatedEntity = attr.get(entity);
//
//	            if (relatedEntity != null) {
//	                Field idField = entityService.getIdField(relatedEntity.getClass());
//
//	                if (idField != null) {
//	                    idField.setAccessible(true);
//	                    Object idValue = idField.get(relatedEntity);
//
//	                    if (idValue != null) {
//	                        predicates.add(cb.equal(root.get(attr.getName()).get(idField.getName()), idValue));
//	                    }
//	                }
//	            }
//	        } catch (IllegalAccessException e) {
//	            e.printStackTrace();
//	        }
//	    }
//
//	    if (!predicates.isEmpty()) {
//	        query.where(cb.and(predicates.toArray(new Predicate[0])));
//	    }
//
//	    return entityManager.createQuery(query).getResultList();
//	}
//
//
//	public List<T> searchByNonNullAttributes(T entity) {
//	    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//	    CriteriaQuery<T> query = cb.createQuery((Class<T>) entity.getClass());
//	    Root<T> root = query.from((Class<T>) entity.getClass());
//
//	    GeneralEntityService entityService = GeneralEntityService.create();
//
//	    List<Field> attributes = entityService.getRelationshipAttributes(entity.getClass(), entity);
//
//	    List<Predicate> predicates = new ArrayList<>();
//
//	    for (Field attr : attributes) {
//	        try {
//	            attr.setAccessible(true);
//	            Object value = attr.get(entity);
//
//	            if (value != null) {
//	                predicates.add(cb.equal(root.get(attr.getName()), value));
//	            }
//	        } catch (IllegalAccessException e) {
//	            e.printStackTrace();
//	        }
//	    }
//
//	    query.where(cb.and(predicates.toArray(new Predicate[0])));
//
//	    return entityManager.createQuery(query).getResultList();
//	}
//
//
//	public List<T> searchByIdCoincidenceAndNonNullAttributes(T entity) {
//		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//	    CriteriaQuery<T> query = cb.createQuery((Class<T>) entity.getClass());
//	    Root<T> root = query.from((Class<T>) entity.getClass());
//
//	    List<Predicate> predicates = new ArrayList<>();
//
//	    GeneralEntityService entityService = GeneralEntityService.create();
//
//	    List<Field> attributes = entityService.getRelationshipAttributes(entity.getClass(), entity);
//
//	    for (Field attr : attributes) {
//	        try {
//	            attr.setAccessible(true);
//	            Object value = attr.get(entity);
//
//	            if (value != null) {
//	                // Special treatment for the "inventory" attribute
//	                if ("inventory".equals(attr.getName())) {
//	                    Field idField = entityService.getIdField(value.getClass());
//	                    idField.setAccessible(true);
//	                    Object inventoryId = idField.get(value);
//
//	                    if (inventoryId != null) {
//	                        // Add LIKE condition for inventory id
//	                        predicates.add(cb.like(root.get("inventory").get(idField.getName()), "%" + inventoryId + "%"));
//	                    }
//	                } else {
//	                    Field idField = entityService.getIdField(value.getClass());
//	                    idField.setAccessible(true);
//	                    Object relatedId = idField.get(value);
//
//	                    if (relatedId != null) {
//	                        predicates.add(cb.equal(root.get(attr.getName()).get(idField.getName()), relatedId));
//	                    }
//	                }
//	            }
//	        } catch (IllegalAccessException e) {
//	            e.printStackTrace();
//	        }
//	    }
//
//	    query.where(cb.and(predicates.toArray(new Predicate[0])));
//
//	    return entityManager.createQuery(query).getResultList();
//	}
//
//}

//package com.essenza.sms_gateway.shared.common.genericService;
//
//import com.essenza.sms_gateway.shared.common.repository.DynamicQueryService;
//import com.essenza.sms_gateway.shared.common.repository.JpaRepositoryExtended;
//import com.essenza.sms_gateway.shared.common.service.GeneralEntityService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.Id;
//import jakarta.persistence.PersistenceContext;
//import java.io.Serializable;
//import java.lang.reflect.Field;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public abstract class DomainGenericServiceImpl<T, ID extends Serializable> implements DomainGenericService<T, ID> {
//
//	public abstract JpaRepositoryExtended<T, ID> getRepository();
//
//	@Autowired
//    private DynamicQueryService<T> dynamicQueryService;
//
//	private DynamicQueryService<T> getDynamicQueryService() {
//	    return dynamicQueryService;
//	}
//
//	@PersistenceContext
//    private EntityManager entityManager;
//
//
//	@Override
//	public Object save(T entity) {
//		GeneralEntityService entityService = GeneralEntityService.create();
//		Class<?> entityClass = entity.getClass();
//
//		List<T> existingEntities = null;
//		try {
//			Map<String, Object> attributes = entityService.getUniqueAttributes(entityClass, entity);
//
//			if (!attributes.isEmpty()) {
//				existingEntities = getDynamicQueryService().findByUniqueAttributes((Class<T>) entityClass, attributes);
//			}
//			entityService.setNullAnnotatedAttributes(entity);
//
//			if (existingEntities.size() == 0) {
//				return getRepository().save(entity);
//			}
//		}catch (DataAccessException e){
//			return returnErrorWhenSave(e.getMostSpecificCause().getLocalizedMessage() + e.getMessage());
//	    } catch (Exception e) {
//	        return returnErrorWhenSave(e.getMessage());
//	    }
//
//		return null;
//    }
//
//
//	private boolean checkIfEntityExists(Class<?> entityClass, Object entityId) {
//	    return getRepository().findById((ID) entityId) != null;
//	}
//
//
//	@Override
//	public Page<T> getAll(int pageNumber, int pageSize, String sort) {
//		try {
//			if (sort == null || sort.isEmpty()) {
//				sort = "id";
//			}
//
//            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sort).ascending());
//            Page<T> resultPage = getRepository().findAll(pageable);
//
//            return resultPage;
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            return Page.empty();
//        }
//	}
//
//
//	@Override
//	public List<Map<String, Object>> getAllProyection(String search) {
//		try {
//			List<Map<String, Object>> results = getRepository().find(search);
//
//			return results;
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            return null;
//        }
//	}
//
//
//	@Override
//	public Object update(T entity) {
//		String id = getEntityIdValue(entity);
//		T entityInDB = get((ID) id);
//
//		if(entityInDB == null) {
//			return 404;
//		}
//
//		GeneralEntityService entityService = GeneralEntityService.create();
//
//		Map<String, Object> validations = new HashMap<>();
//
//		Class<?> entityClass = entity.getClass();
//		Map<String, Object> attributes = entityService.getUniqueAttributes(entityClass, entity);
//
//		List<T> existingEntities = null;
//		try {
//			if (!attributes.isEmpty()) {
//				existingEntities = getDynamicQueryService().findByUniqueAttributes((Class<T>) entityClass, attributes);
//			}
//
//			if (existingEntities.size() <= 1) {
//				getRepository().save(entity);
//				return 200;
//			}
//		} catch (Exception e) {
//			return returnErrorWhenSave(e.getMessage());
//		}
//
//		return 409;
//	}
//
//
//	@Override
//	public String getEntityIdValue(T entity) {
//		Class<?> clazz = entity.getClass();
//
//		for (Field field : clazz.getDeclaredFields()) {
//			if (field.isAnnotationPresent(Id.class)) {
//				field.setAccessible(true);
//
//				try {
//					Object value = field.get(entity);
//					if (value != null) {
//						return value.toString();
//					}
//				} catch (IllegalAccessException e) {
//					throw new RuntimeException("Could not access the ID field.", e);
//				}
//			}
//		}
//
//		throw new RuntimeException("No field with @Id annotation found in class " + clazz.getName());
//	}
//
//
//	@Override
//	public T delete(ID id) {
//		T entity = get(id);
//
//		if(entity == null) {
//			return null;
//		}
//
//		getRepository().deleteById(id);
//
//		return entity;
//	}
//
//
//	public T searchAndRetrieveRelatedEntity(T entity, Field field) {
//	    field.setAccessible(true);
//
//	    try {
//	        Object relatedEntity = field.get(entity);
//
//	        if (relatedEntity != null) {
//	            GeneralEntityService entityService = GeneralEntityService.create();
//	            Map<String, Object> idAttribute = entityService.getIdAttribute(relatedEntity.getClass(), relatedEntity);
//
//	            if (!idAttribute.isEmpty()) {
//	                Class<?> entityClass = relatedEntity.getClass();
//
//	                List<T> existingEntities = getDynamicQueryService().findByUniqueAttributes((Class<T>) entityClass, idAttribute);
//
//	                if (!existingEntities.isEmpty()) {
//	                    return existingEntities.get(0);
//	                }
//	            }
//	        }
//	    } catch (IllegalAccessException e) {
//	        e.printStackTrace();
//	    }
//
//	    return null;
//	}
//
//
//	public T get(ID id) {
//		Optional<T> optional = getRepository().findById(id);
//
//		if(optional.isPresent()) {
//			return optional.get();
//		}
//
//		return null;
//	}
//
//
//	private String returnErrorWhenSave(String err) {
//		Pattern pattern;
//		Matcher matcher;
//
//		pattern = Pattern.compile("Duplicate entry '(.*?)'");
//		matcher = pattern.matcher(err);
//		if (matcher.find()) {
//			return matcher.group(0);
//		}
//
//		pattern = Pattern.compile("UNIQUE KEY.*? \\((\\d+)\\)");
//		matcher = pattern.matcher(err);
//		if (matcher.find()) {
//			return "DUPLICATE KEY " + matcher.group(1);
//		}
//
//		pattern = Pattern.compile("Unable to find (\\S+) with id (\\d+)");
//	    matcher = pattern.matcher(err);
//	    if (matcher.find()) {
//	    	String entityClassWithPackage = matcher.group(1);
//	        String idValue = matcher.group(2);
//	        String entityClass = entityClassWithPackage.substring(entityClassWithPackage.lastIndexOf(".") + 1);
//
//	        return "'" + entityClass + "' with ID " + idValue + " was not found";
//	    }
//
//	    pattern = Pattern.compile("transient instance must be saved before current operation : ([\\w\\.]+)\\.(\\w+) -> ([\\w\\.]+)");
//	    matcher = pattern.matcher(err);
//	    if (matcher.find()) {
//	    	String entityClassWithPackage = matcher.group(1);
//	        String propertyName = matcher.group(2);
//	        String entityClass = entityClassWithPackage.substring(entityClassWithPackage.lastIndexOf(".") + 1);
//
//	        return "Property '" + propertyName + "' in '" + entityClass + "' must be saved before current operation.";
//	    }
//
//	    pattern = Pattern.compile("Cannot add or update a child row: a foreign key constraint fails \\(`(.*?)`\\.\\`(.*?)`.*?CONSTRAINT `([^`]*)` FOREIGN KEY \\(`(.*?)`\\) REFERENCES `(.*?)` \\(`(.*?)`\\)");
//	    matcher = pattern.matcher(err);
//	    if (matcher.find()) {
//	        String database = matcher.group(1);
//	        String table = matcher.group(2);
//	        String constraint = matcher.group(3);
//	        String foreignKeyColumn = matcher.group(4);
//	        String referencedTable = matcher.group(5);
//	        String referencedColumn = matcher.group(6);
//
//	        return String.format("The %s in %s does not exist in %s", foreignKeyColumn, table, referencedTable);
//	    }
//
//	    return err;
//	}
//
//}

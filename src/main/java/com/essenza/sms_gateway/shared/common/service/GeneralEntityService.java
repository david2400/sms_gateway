//package com.essenza.sms_gateway.shared.common.service;
//
//import com.essenza.sms_gateway.shared.common.anotations.NullAttribute;
//import org.hibernate.Metamodel;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//
//import jakarta.persistence.*;
//import jakarta.persistence.criteria.CriteriaBuilder;
//import jakarta.persistence.criteria.CriteriaQuery;
//import jakarta.persistence.criteria.Predicate;
//import jakarta.persistence.criteria.Root;
//import jakarta.persistence.metamodel.EntityType;
//import jakarta.persistence.metamodel.SingularAttribute;
//import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Positive;
//import jakarta.validation.constraints.Size;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Service
//public class GeneralEntityService<T> {
//
//	private static GeneralEntityService<?> generalEntityService;
//
//	private GeneralEntityService() {}
//
//	public static GeneralEntityService<?> create() {
//		if (generalEntityService == null) {
//			generalEntityService = new GeneralEntityService();
//		}
//
//		return generalEntityService;
//	}
//
//	@PersistenceContext
//	private EntityManager entityManager;
//
//
//	public <T> Map<String, Object> getIdAttribute(Class<?> entityClass, T entity) {
//		Map<String, Object> attributes = new HashMap<>();
//
//		for (Field field : entityClass.getDeclaredFields()) {
//			if (field.isAnnotationPresent(Id.class)) {
//				try {
//					field.setAccessible(true);
//					attributes.put(field.getName(), field.get(entity));
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		return attributes;
//	}
//
//
//	public Field getIdField(Class<?> clazz) {
//		Field[] fields = clazz.getDeclaredFields();
//
//		for (Field field : fields) {
//			if (field.isAnnotationPresent(Id.class)) {
//				return field;
//			}
//		}
//
//		return null;
//	}
//
//
//	public <T> Map<String, Object> getUniqueAttributes(Class<?> entityClass, T entity) {
//		Map<String, Object> attributes = new HashMap<>();
//
//		for (Field field : entityClass.getDeclaredFields()) {
//			if (field.isAnnotationPresent(Column.class)) {
//				Column columnAnnotation = field.getAnnotation(Column.class);
//				if (columnAnnotation.unique()) {
//					try {
//						field.setAccessible(true);
//						attributes.put(field.getName(), field.get(entity));
//					} catch (IllegalAccessException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//
//			if (field.isAnnotationPresent(Id.class)) {
//				try {
//					field.setAccessible(true);
//					attributes.put(field.getName(), field.get(entity));
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		return attributes;
//	}
//
//
//	public <T> List<Object> getRelationshipAttributes(Class<?> entityClass, T entity) {
//		List<Object> attributes = new ArrayList<>();
//
//		for (Field field : entityClass.getDeclaredFields()) {
//			if (field.isAnnotationPresent(ManyToOne.class) ||
//					field.isAnnotationPresent(OneToMany.class) ||
//					field.isAnnotationPresent(OneToOne.class) ||
//					field.isAnnotationPresent(ManyToMany.class)) {
//
//				field.setAccessible(true);
//				attributes.add(field);
//			}
//		}
//
//		return attributes;
//	}
//
//
//	public <T> void setNullAnnotatedAttributes(T obj) {
//		Field[] fields = obj.getClass().getDeclaredFields();
//
//		for (Field field : fields) {
//			if (field.isAnnotationPresent(NullAttribute.class)) {
//				field.setAccessible(true);
//				try {
//					field.set(obj, null);
//				} catch (IllegalArgumentException | IllegalAccessException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//	}
//
//
//	public <T> Map<Object, Object> createInstanceWithoutNullAttributes(T obj) {
//		if (obj == null) throw new IllegalArgumentException("The provided object must not be null");
//
//		Map<Object, Object> entity = null;
//
//		try {
//			Class<?> clazz = obj.getClass();
//			T newInstance = (T) clazz.newInstance();
//
//			Field[] fields = clazz.getDeclaredFields();
//
//			entity = new HashMap<>();
//
//			for (Field field : fields) {
//				field.setAccessible(true);
//				int modifiers = field.getModifiers();
//
//				if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) continue;
//
//				Object value = field.get(obj);
//
//				if (value != null) entity.put(field.getName(), value);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return entity;
//	}
//
//
//	public T printForeignKeys(Class<?> entityClass, Object entity) {
//		Metamodel metamodel = (Metamodel) entityManager.getMetamodel();
//		EntityType<?> entityType = metamodel.entity(entityClass);
//
//		Set<?> attributes = entityType.getSingularAttributes();
//		for (Object attr : attributes) {
//			SingularAttribute<?, ?> attribute = (SingularAttribute<?, ?>) attr;
//
//			Field field = null;
//			try {
//				field = entityClass.getDeclaredField(attribute.getName());
//				field.setAccessible(true);
//
//				Object value = field.get(entity);
//
//				if (attribute.isAssociation()) {
//					return (T) value;
//				}
//			} catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException e) {
//				e.printStackTrace();
//			}
//		}
//		return null;
//	}
//
//
//	public Specification<T> byAttributes(Map<String, Object> attributes) {
//		return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
//			Predicate predicate = builder.conjunction();
//			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
//				String key = entry.getKey();
//				Object value = entry.getValue();
//
//				if (value != null && !value.toString().isEmpty()) {
//					predicate = builder.and(predicate, builder.equal(root.get(key), value));
//				}
//			}
//			return predicate;
//		};
//	}
//
//
//	public <D> void initializeFields(D dto) throws IllegalAccessException {
//		Field[] fields = dto.getClass().getDeclaredFields();
//
//		List<Annotation> annotationList = new ArrayList<>();
//		Set<Class<? extends Annotation>> annotationTypes = Set.of(NotNull.class, NotEmpty.class);
//		boolean hasNotNullOrNotEmpty = false;
//
//		for (Field field : fields) {
//			annotationList.clear();
//
//			field.setAccessible(true);
//			if (field.get(dto) == null) {
//				for (Annotation annotation : field.getAnnotations()) {
//					annotationList.add(annotation);
//				}
//
//				hasNotNullOrNotEmpty = containsAnnotations(annotationList, annotationTypes);
//
//				if(hasNotNullOrNotEmpty) {
//					setDefaultFieldValue(dto, field, annotationList);
//				}
//			}
//		}
//	}
//
//
//	public static boolean containsAnnotations(List<Annotation> annotationList, Set<Class<? extends Annotation>> annotationTypes) {
//		return annotationList.stream()
//				.anyMatch(annotation -> annotationTypes.contains(annotation.annotationType()));
//	}
//
//
//	private <D> void setDefaultFieldValue(D dto, Field field, List<Annotation> annotationList) throws IllegalArgumentException, IllegalAccessException {
//		if(annotationList.stream()
//				.anyMatch(annotation -> annotation instanceof Positive)) {
//			if (field.getType() == Integer.class || field.getType() == int.class) field.set(dto, 1);
//			if (field.getType() == String.class) field.set(dto, "1");
//		}
//
//		if(annotationList.stream()
//				.anyMatch(annotation -> annotation instanceof Size)) {
//			Annotation annotation = annotationList.get(returnFirstMatchingAnnotation(annotationList, Size.class));
//			Size size = (Size) annotation;
//			int minSize = size.min();
//
//			if (field.getType() == String.class) {
//				String fieldValue = (String) field.get(dto);
//
//				if(fieldValue == null) fieldValue = "x";
//
//				field.set(dto, fieldValue.repeat(minSize));
//			}
//		}
//	}
//
//
//	public static int returnFirstMatchingAnnotation(List<Annotation> annotationList, Class<? extends Annotation> annotationClass) {
//		return annotationList.stream()
//				.filter(annotationClass::isInstance)
//				.findFirst()
//				.map(annotationList::indexOf)
//				.orElse(-1);
//	}
//
//
//	public List<T> mapResultsToDto(List<Map<String, Object>> results, Class<T> dtoClass) {
//		List<T> dtoList = new ArrayList<>();
//
//		try {
//			for (Map<String, Object> row : results) {
//				T dtoInstance = dtoClass.getDeclaredConstructor().newInstance();
//				Field[] fields = dtoClass.getDeclaredFields();
//
//				for (Field field : fields) {
//					field.setAccessible(true);
//					String paramName = field.getName();
//					Object value = row.get(paramName);
//
//					if (value != null) {
//						field.set(dtoInstance, value);
//					}
//				}
//
//				dtoList.add(dtoInstance);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return dtoList;
//	}
//
//
//	public void setCurrentTimestamp(Object entity) {
//		if (entity == null) return;
//
//		LocalDateTime now = LocalDateTime.now();
//
//		for (Field field : entity.getClass().getDeclaredFields()) {
//			if (field.getType().equals(LocalDateTime.class)) {
//				field.setAccessible(true);
//				try {
//					field.set(entity, now);
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//	}
//
//}

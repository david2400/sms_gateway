package com.essenza.sms_gateway.shared.common.modelmapper;


import com.essenza.sms_gateway.shared.common.anotations.ForeignKey;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mapper {

    private static Mapper genericMapper;

    private ModelMapper modelMapper;

    private Mapper() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    private static class Holder {
        private static final Mapper INSTANCE = new Mapper();
    }

    public static Mapper create() {
        return Holder.INSTANCE;
    }

    public <D, T> List<D> convertToListDto(List<T> entityList, Class<D> dtoClass) {
        if (entityList == null) return null;

        List<D> dtoList = new ArrayList<>();

        entityList.forEach(entity -> dtoList.add(entityToDto(entity, dtoClass)));

        return dtoList;
    }


    public <D, E> E dtoToEntity(D dto, Class<E> entityClass) {
        if (dto == null) return null;

        E entity = modelMapper.map(dto, entityClass);
        mapAssociatedEntities(dto, entity);

        return entity;
    }

    public <D, T> T mergeEntities(D source, T target) {
        if (source == null || target == null) return target;

        try {
            Class<?> sourceClass = source.getClass();
            Class<?> targetClass = target.getClass();

            for (Field sourceField : getAllFields(sourceClass)) {
                sourceField.setAccessible(true);
                Object value = sourceField.get(source);

                if (value != null) {
                    try {
                        Field targetField = getFieldByName(targetClass, sourceField.getName());
                        if (targetField != null) {
                            targetField.setAccessible(true);
                            if (targetField.getType().isAssignableFrom(sourceField.getType())) {
                                targetField.set(target, value);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        // Ignorar campos inaccesibles
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error merging entities", e);
        }

        return target;
    }

    // Obtiene todos los campos (incluyendo los heredados)
    private Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        while (type != null && type != Object.class) {
            fields.addAll(Arrays.asList(type.getDeclaredFields()));
            type = type.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    // Busca un campo por nombre (incluyendo en superclases)
    private Field getFieldByName(Class<?> type, String name) {
        while (type != null && type != Object.class) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private <D, E> void mapAssociatedEntities(D dto, E entity) {
        try {
            Field[] dtoFields = dto.getClass().getDeclaredFields();
            for (Field dtoField : dtoFields) {
                if (dtoField.isAnnotationPresent(ForeignKey.class)) {
                    dtoField.setAccessible(true);
                    Object relatedEntityId = dtoField.get(dto);
                    if (relatedEntityId != null) {
                        ForeignKey foreignKey = dtoField.getAnnotation(ForeignKey.class);
                        Class<?> relatedEntityClass = foreignKey.referencedEntity();

                        Field entityField = findEntityField(entity.getClass(), relatedEntityClass);
                        if (entityField != null) {
                            entityField.setAccessible(true);
                            Object relatedEntityInstance = relatedEntityClass.newInstance();
                            Field idField = findIdField(relatedEntityClass);
                            if (idField != null) {
                                idField.setAccessible(true);
                                idField.set(relatedEntityInstance, relatedEntityId);
                                entityField.set(entity, relatedEntityInstance);
                            } else {
                                throw new RuntimeException("No @Id field found in " + relatedEntityClass.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map associated entities", e);
        }
    }

    private Field findEntityField(Class<?> entityClass, Class<?> relatedEntityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getType().equals(relatedEntityClass)) {
                return field;
            }
        }
        return null;
    }


    private Field findIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }


    public <D, T> D entityToDto(T entity, Class<D> dtoClass) {
        if (entity == null) return null;

        D dto = modelMapper.map(entity, dtoClass);

        return dto;
    }


    public <E> List<E> getList(E entity) {
        if (entity != null) {
            List<E> list = new ArrayList<>();

            list.add(entity);

            return list;
        }

        return null;
    }

}

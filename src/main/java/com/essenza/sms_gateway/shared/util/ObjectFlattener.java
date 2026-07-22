package com.essenza.sms_gateway.shared.util;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

public class ObjectFlattener {

    public static Map<String, Object> flatten(Object obj) {
        return flatten(obj, "", new HashSet<>());
    }

    private static Map<String, Object> flatten(Object obj, String path, Set<Object> visited) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (obj == null || visited.contains(obj)) {
            return result;
        }

        visited.add(obj);

        Class<?> clazz = obj.getClass();

        if (isSimpleType(clazz)) {
            result.put(path, obj);
            return result;
        }

        if (obj instanceof Collection) {
            result.put(path, obj);
            return result;
        }

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                String newPath = path.isEmpty() ? field.getName() : path + "." + field.getName();

                if (value == null || isSimpleType(value.getClass())) {
                    result.put(newPath, value);
                } else if (value instanceof Collection) {
                    // Incluir lista completa como valor, sin expandir
                    result.put(newPath, value);
                } else {
                    // Objeto compuesto
                    result.putAll(flatten(value, newPath, visited));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz == Character.class
                || clazz == Date.class
                || clazz == LocalDate.class
                || clazz == LocalDateTime.class
                || clazz == Instant.class
                || clazz == ZonedDateTime.class
                || clazz == OffsetDateTime.class
                || clazz.isEnum();
    }
}

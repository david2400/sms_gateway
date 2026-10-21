//package com.essenza.sms_gateway.config;
//
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
//import org.springframework.core.type.classreading.MetadataReader;
//import org.springframework.core.type.classreading.MetadataReaderFactory;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class MaestraPackageScanner {
//
//    private static final String BASE_PACKAGE = "com.essenza.sms_gateway.modules";
//    private static final String MAESTRA_PATTERN = "**/maestra/**/*.class";
//
//    public String[] scanMaestraPackages() {
//        List<String> packages = new ArrayList<>();
//        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
//
//        try {
//            String searchPattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
//                                 BASE_PACKAGE.replace('.', '/') + "/" + MAESTRA_PATTERN;
//
//            Resource[] resources = resolver.getResources(searchPattern);
//
//            for (Resource resource : resources) {
//                if (resource.isReadable()) {
//                    try {
//                        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
//                        String className = metadataReader.getClassMetadata().getClassName();
//
//                        // Extraer el paquete que contiene 'maestra'
//                        String packageName = extractMaestraPackage(className);
//                        if (packageName != null && !packages.contains(packageName)) {
//                            packages.add(packageName);
//                        }
//                    } catch (IOException e) {
//                        // Ignorar clases que no se pueden leer
//                    }
//                }
//            }
//        } catch (IOException e) {
//            // En caso de error, devolver los paquetes conocidos
//        }
//
//        return  packages.toArray(new String[0]);
//    }
//
//    private String extractMaestraPackage(String className) {
//        if (className.contains(".maestra.")) {
//            int maestraIndex = className.indexOf(".maestra.");
//            int nextDotIndex = className.indexOf('.', maestraIndex + 9); // 9 = ".maestra.".length()
//
//            if (nextDotIndex > 0) {
//                return className.substring(0, nextDotIndex);
//            } else {
//                // Si no hay más paquetes después de maestra, tomar hasta maestra
//                return className.substring(0, maestraIndex + 8); // 8 = ".maestra".length()
//            }
//        }
//        return null;
//    }
//
//}

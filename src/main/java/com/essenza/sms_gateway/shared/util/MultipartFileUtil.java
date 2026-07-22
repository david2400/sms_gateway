//package com.essenza.sms_gateway.shared.util;
//
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//
//public class MultipartFileUtil {
//    public static MultipartFile fileToMultipartFile(File archivoPdf) throws IOException {
//        try (FileInputStream fis = new FileInputStream(archivoPdf)) {
//            return new MockMultipartFile(
//                    "file",
//                    archivoPdf.getName(),
//                    "application/pdf",
//                    fis
//            );
//        }
//    }
//
//    public static MultipartFile bytesToMultipartFile(byte[] contenido, String nombreArchivo) {
//        return new MockMultipartFile(
//                "file",
//                nombreArchivo,
//                "application/pdf",
//                contenido
//        );
//    }
//}

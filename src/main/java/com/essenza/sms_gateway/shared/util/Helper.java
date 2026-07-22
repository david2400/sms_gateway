//package com.essenza.sms_gateway.shared.util;
//
//import com.comfenalco.servicio.exception.response.status.InternalServerErrorException;
//import com.comfenalco.servicios.dracoFuncionarios.InitDataAws;
//import com.comfenalco.servicios.dracoFuncionarios.modules.aws.consumer.IAmazonS3Consumer;
//import com.comfenalco.servicios.dracoFuncionarios.modules.aws.domain.dto.ArchivosAmazonS3;
//import com.comfenalco.servicios.dracoFuncionarios.modules.aws.domain.dto.ArchivosAmazonS3DTO;
//import com.comfenalco.servicios.dracoFuncionarios.modules.aws.domain.dto.DocumentoDto;
//
//import jakarta.servlet.http.HttpServletRequest;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class Helper {
//
//
//    public static List<ArchivosAmazonS3> getInformacionArchivo(IAmazonS3Consumer iAmazonS3Consumer, ArchivosAmazonS3DTO archivosAmazonS3DTO, String token) {
//
//        return iAmazonS3Consumer.informacionArhivo(token, archivosAmazonS3DTO);
//
//    }
//
//    public static String getToken(HttpServletRequest request) {
//
//        return request.getHeader("Authorization");
//    }
//
//
//    public static DocumentoDto getDocumentoDto(Integer id, IAmazonS3Consumer iAmazonS3Consumer) {
//
//        DocumentoDto documentoDto = new DocumentoDto();
//
//        try {
//            List<DocumentoDto> documentoDtoList = iAmazonS3Consumer.getTipoDocumentos();
//            documentoDto = documentoDtoList.stream().filter(documentoDto1 -> Objects.equals(documentoDto1.getId(), id)).findFirst().orElse(new DocumentoDto());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return documentoDto;
//
//
//    }
//
//    public static ArchivosAmazonS3 infoFileAmazonS3(Long archivoAmazonS3Id, InitDataAws initDataAws, IAmazonS3Consumer iAmazonS3Consumer, HttpServletRequest httpServletRequest) {
//
//        ArchivosAmazonS3DTO archivosAmazonS3DTO = ArchivosAmazonS3DTO.builder().id(archivoAmazonS3Id).build();
//        List<ArchivosAmazonS3> archivosAmazonS3;
//
//        ArchivosAmazonS3 archivosAmazonS31;
//
//        try {
//            archivosAmazonS3 = archivoAmazonS3Id == null ? new ArrayList<>() : iAmazonS3Consumer.informacionArhivo(HelpersUsuarios.getToken(httpServletRequest), archivosAmazonS3DTO);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new InternalServerErrorException("No se encuentran archivos de beneficiario");
//        }
//
//        archivosAmazonS31 = archivosAmazonS3.stream().findFirst().orElse(new ArchivosAmazonS3());
//
//        if (Objects.equals(archivosAmazonS31.getBuketAwsId(), initDataAws.getBuket().getNombreBuket())) {
//            archivosAmazonS31.setBuketAwsId(initDataAws.getBuket().getId());
//        } else {
//            archivosAmazonS31.setBuketAwsId(21L);
//        }
//
//
//        return archivosAmazonS31;
//
//
//    }
//
//}

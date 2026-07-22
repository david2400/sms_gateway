//package com.essenza.sms_gateway.shared.util;
//
//import com.comfenalco.servicio.exception.response.status.BadRequestException;
//import com.comfenalco.servicios.dracoFuncionarios.common.domain.dto.InformacionUsuarioDto;
//import com.comfenalco.servicios.dracoFuncionarios.consumer.IInfoUsuarioConsumer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.servlet.http.HttpServletRequest;
//import java.util.concurrent.atomic.AtomicReference;
//
//@Component
//public class HelpersUsuarios {
//
//    @Autowired
//    private IInfoUsuarioConsumer iInfoUsuarioConsumer;
//
//    private static IInfoUsuarioConsumer staticIInfoUsuarioConsumer;
//
//
//    private static final AtomicReference<InformacionUsuarioDto> instance = new AtomicReference<>();
//
//    public HelpersUsuarios() {
//    }
//
//    @PostConstruct
//    public void init() {
//        staticIInfoUsuarioConsumer = iInfoUsuarioConsumer;
//    }
//
//    public static InformacionUsuarioDto getInstance() {
//        if (instance.get() == null) {
//            instance.compareAndSet(null, new InformacionUsuarioDto());
//        }
//        return instance.get();
//    }
//
//    public static void setUsuario(HttpServletRequest request) {
//        try {
//            instance.set(staticIInfoUsuarioConsumer.getInformacionUsuario(getToken(request)));
//        } catch (Exception e) {
//            throw new BadRequestException("Error al consultar cedula usuario: " + e.getMessage());
//        }
//    }
//
//    public static String getCedulaUsuario() {
//        try {
//            return getInstance().getCedula();
//        } catch (Exception e) {
//            throw new BadRequestException("Error al consultar cedula usuario: " + e.getMessage());
//        }
//    }
//
//    public static String getUsuario() {
//        try {
//            return getInstance().getUsuario();
//        } catch (Exception e) {
//            throw new BadRequestException("Error al consultar el usuario: " + e.getMessage());
//        }
//    }
//
//    public static Integer getIdUsuario() {
//        try {
//            return getInstance().getId();
//        } catch (Exception e) {
//            throw new BadRequestException("Error al consultar ID usuario: " + e.getMessage());
//        }
//    }
//
//    public static String getToken(HttpServletRequest request) {
//        return request.getHeader("Authorization");
//    }
//}

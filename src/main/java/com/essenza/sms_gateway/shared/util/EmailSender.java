//package com.essenza.sms_gateway.shared.util;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.InputStreamSource;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.UnsupportedEncodingException;
//
//@Component
//public class EmailSender {
//    @Autowired
//    private JavaMailSender mailSender;
//
//    @Value("${aplicacion.contacto.nombre}")
//    private String nombre;
//
//    @Value("${spring.mail.username}")
//    private String mail;
//
//    public void sendHtmlEmail(String to, String subject, String htmlBody, MultipartFile adjunto) throws MessagingException, MessagingException, UnsupportedEncodingException {
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//        helper.setFrom(new InternetAddress(mail,"Comfenalco Quindío"));
//        helper.setTo(to.contains(";") ? to.split(";") : new String[]{to.trim()});
//        helper.setSubject(subject);
//        helper.setText(htmlBody, true);
//
//        if (adjunto != null && !adjunto.isEmpty()) {
//            helper.addAttachment(
//                    adjunto.getOriginalFilename(),
//                    new InputStreamSource() {
//                        @Override
//                        public InputStream getInputStream() throws IOException {
//                            return adjunto.getInputStream();
//                        }
//                    }
//            );
//        }
//
//        mailSender.send(message);
//    }
//
//    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlBody, File file) throws MessagingException, MessagingException, UnsupportedEncodingException {
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//        helper.setFrom(new InternetAddress(mail,"Comfenalco Quindío"));
//        helper.setTo(to.contains(";") ? to.split(";") : new String[]{to.trim()});
//        helper.setSubject(subject);
//        helper.setText(htmlBody, true);
//        helper.addAttachment(file.getName(), file);
//        mailSender.send(message);
//    }
//}

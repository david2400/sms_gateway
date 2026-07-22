package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.inalambria;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsMessageType;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendInalambriaBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SendSmsBatchCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms.SmsTypeRequest;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.InalambriaAccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Orquestador para transformar comandos de negocio a comandos específicos de Inalambria.
 * <p>
 * Es infraestructura pura, específica de este proveedor: traduce los tipos de
 * mensaje de dominio ({@link SmsMessageType}) a las cuentas y códigos propios
 * de la API de Inalambria. El dominio y la aplicación nunca conocen
 * {@link InalambriaAccountType}; solo este proveedor lo hace.
 */
@Slf4j
@Component
public class InalambriaAccountOrchestrator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Transforma SendSmsBatchCommand a SendInalambriaBatchCommand
     */
    public SendInalambriaBatchCommand transformBatchCommand(SendSmsBatchCommand command) {
        log.debug("[ORCHESTRATOR] Transformando batch command con {} destinatarios",
                command.getDestinations().size());

        Map<String, String> devices = new HashMap<>();
        command.getDestinations().forEach(dest -> {
            String phone = dest.getPhoneNumber();
            String countryCode = dest.getCountryCode() != null ? dest.getCountryCode() : "57";
            devices.put(phone, countryCode); // Usar countryCode como valor para referencia
        });

        // Usar el primer countryCode del primer destinatario como countryCode del batch
        String batchCountryCode = command.getDestinations().isEmpty() ? "57" : 
            (command.getDestinations().get(0).getCountryCode() != null ? 
             command.getDestinations().get(0).getCountryCode() : "57");

        return SendInalambriaBatchCommand.builder()
                .accountType(SendInalambriaBatchCommand.AccountType.MASIVO)
                .messageText(command.getMessage())
                .transactionNumber("BATCH_" + System.currentTimeMillis())
                .devices(devices)
                .type("0")
                .flashSms(false)
                .hasMore(false)
                .dateMessage(LocalDateTime.now().format(DATE_FORMAT))
                .scheduledDate(command.getScheduledDate())
                .countryCode(batchCountryCode)
                .build();
    }

    /**
     * Transforma SendOtpRequest a SendInalambriaBatchCommand
     */
    public SendInalambriaBatchCommand transformOtpCommand(SendOtpRequest request) {
        log.debug("[ORCHESTRATOR] Transformando OTP request para: {}", request.getPhoneNumber());

        Map<String, String> devices = new HashMap<>();
        devices.put(request.getPhoneNumber(), "OTP_RECIPIENT");

        return SendInalambriaBatchCommand.builder()
                .accountType(SendInalambriaBatchCommand.AccountType.OTP)
                .messageText(request.buildFullMessage())
                .transactionNumber(request.getReferenceId())
                .devices(devices)
                .type("1")
                .flashSms(false)
                .hasMore(false)
                .dateMessage(LocalDateTime.now().format(DATE_FORMAT))
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .build();
    }

    /**
     * Transforma SmsTypeRequest a SendInalambriaBatchCommand
     */
    public SendInalambriaBatchCommand transformSmsTypeRequest(SmsTypeRequest request) {
        log.debug("[ORCHESTRATOR] Transformando SMS type request: {} para: {}",
                request.getSmsType(), request.getTo());

        Map<String, String> devices = new HashMap<>();
        devices.put(request.getTo(), "RECIPIENT");

        InalambriaAccountType accountType = resolveAccountType(request.getSmsType(), request.getRecipientCount());

        return SendInalambriaBatchCommand.builder()
                .accountType(convertAccountType(accountType))
                .messageText(request.getMessage())
                .transactionNumber("SMS_" + System.currentTimeMillis())
                .devices(devices)
                .type(convertTypeToString(accountType))
                .flashSms(false)
                .hasMore(false)
                .dateMessage(LocalDateTime.now().format(DATE_FORMAT))
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .build();
    }


    /**
     * Resuelve el tipo de cuenta Inalambria a partir del tipo de mensaje de dominio,
     * considerando si el envío es masivo (más de un destinatario)
     */
    private InalambriaAccountType resolveAccountType(SmsMessageType smsType, Integer recipientCount) {
        if (recipientCount != null && recipientCount > 1) {
            log.info("[ORCHESTRATOR] Envío masivo detectado ({} destinatarios), usando cuenta MASIVO", recipientCount);
            return InalambriaAccountType.MASIVO;
        }
        return mapSmsTypeToAccountType(smsType);
    }

    /**
     * Mapea el tipo de SMS al tipo de cuenta de Inalambria
     */
    private InalambriaAccountType mapSmsTypeToAccountType(SmsMessageType smsType) {
        switch (smsType) {
            case OTP:
                return InalambriaAccountType.OTP;
            case HABEAS_DATA:
                return InalambriaAccountType.HABEASDATA;
            case TEMPLATE:
                return InalambriaAccountType.TEMPLATE;
            case PERSONALIZED:
                return InalambriaAccountType.PERSONALIZADA;
            case NORMAL:
            default:
                return InalambriaAccountType.MASIVO;
        }
    }

    /**
     * Convierte InalambriaAccountType a SendInalambriaBatchCommand.AccountType
     */
    private SendInalambriaBatchCommand.AccountType convertAccountType(InalambriaAccountType accountType) {
        switch (accountType) {
            case OTP:
                return SendInalambriaBatchCommand.AccountType.OTP;
            case TEMPLATE:
                return SendInalambriaBatchCommand.AccountType.TEMPLATE;
            case PERSONALIZADA:
                return SendInalambriaBatchCommand.AccountType.PERSONALIZADA;
            case MASIVO:
                return SendInalambriaBatchCommand.AccountType.MASIVO;
            case HABEASDATA:
                return SendInalambriaBatchCommand.AccountType.HABEASDATA;
            default:
                return SendInalambriaBatchCommand.AccountType.MASIVO;
        }
    }

    /**
     * Convierte InalambriaAccountType a string para el API
     */
    private String convertTypeToString(InalambriaAccountType accountType) {
        switch (accountType) {
            case OTP:
                return "1";
            case TEMPLATE:
                return "2";
            case HABEASDATA:
                return "3";
            case PERSONALIZADA:
                return "4";
            case MASIVO:
            default:
                return "0";
        }
    }
}

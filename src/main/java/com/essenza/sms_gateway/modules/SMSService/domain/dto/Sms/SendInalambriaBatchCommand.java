package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Command: send SMS through Inalambria accounts (OTP, TEMPLATE, etc.).
 */
@Value
@Builder(toBuilder = true)
public class SendInalambriaBatchCommand {

    public enum AccountType {
        OTP, TEMPLATE, PERSONALIZADA, MASIVO, HABEASDATA
    }

    AccountType accountType;
    String dateMessage;
    @Singular("device")
    Map<String, String> devices;
    boolean flashSms;
    boolean hasMore;
    String messageData;
    String messagePattern;
    String messageText;
    String templateId;
    String transactionNumber;
    String type;
    String url;
    LocalDateTime scheduledDate;
    String countryCode = "57";

    public boolean isValid() {
        return accountType != null
            && messageText != null && !messageText.isBlank()
            && devices != null && !devices.isEmpty();
    }
}

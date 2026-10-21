package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendOtpUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SmsAvailabilityUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpSendService implements SendOtpUseCase, SmsAvailabilityUseCase {

    private static final String DEFAULT_MESSAGE = "Tu código de verificación es: {OTP}. Válido por 5 minutos.";

    private final OtpService otpService;
    private final RoutingPolicyPort routingPolicy;

    @Override
    public OtpSendResult send(SendOtpRequest request, String idempotencyKey) {
        return otpService.send(request, idempotencyKey);
    }

    @Override
    public OtpSendResult sendAutoGenerate(String phoneNumber, String message, Integer expirationMinutes, String cuenta) {
        return otpService.sendAutoGenerate(phoneNumber, message, expirationMinutes, cuenta);
    }

    @Override
    public OtpSendResult sendWithDefaultMessage(String phoneNumber, String cuenta) {
        return otpService.sendAutoGenerate(phoneNumber, DEFAULT_MESSAGE, 5, cuenta);
    }

    @Override
    public boolean isAvailable(MessageType type) {
        return !routingPolicy.resolve(null, type).isEmpty();
    }

    @Override
    public Optional<String> primaryProvider(MessageType type) {
        RoutePlan plan = routingPolicy.resolve(null, type);
        return plan.isEmpty() ? Optional.empty() : Optional.of(plan.getSteps().get(0).getAccount().getProviderCode());
    }
}

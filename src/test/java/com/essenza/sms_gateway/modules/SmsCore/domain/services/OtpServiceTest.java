package com.essenza.sms_gateway.modules.SmsCore.domain.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.otp.SendOtpRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioDto;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.usuario.ActualizarUsuarioResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.UsuarioServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    private FakeDispatch dispatch;
    private FakeUsuario usuario;
    private OtpService service;

    @BeforeEach
    void setUp() {
        dispatch = new FakeDispatch();
        usuario = new FakeUsuario();
        service = new OtpService(dispatch, new PinGeneratorService(), usuario);
    }

    @Test
    void otpNormalUsaRutaCentralTipoOtp() {
        OtpSendResult r = service.send(SendOtpRequest.builder().phoneNumber("3157132338")
                .otpCode("482913").message("Su código es {OTP}").build(), "k1");

        assertTrue(r.isSuccess());
        assertEquals(dispatch.result.getEnvioId().toString(), r.getMessageId());
        assertEquals("INALAMBRIA/INALAMBRIA_OTP", r.getProviderReference());
        SmsDispatch sent = dispatch.sent.get(0);
        assertEquals(MessageType.OTP, sent.getType());
        assertEquals("Su código es 482913", sent.getText());
        assertEquals("k1", sent.getIdempotencyKey());
        assertNull(dispatch.provider);
        assertEquals(DispatchChannel.CENTRAL, dispatch.channel);
        assertNull(sent.getAccountCode());
    }

    @Test
    void otpConCuentaLaPasaAlMotor() {
        service.send(SendOtpRequest.builder().phoneNumber("3157132338").otpCode("482913")
                .message("Su código es {OTP}").cuenta("INALAMBRIA_MASIVO").build(), null);

        assertEquals("INALAMBRIA_MASIVO", dispatch.sent.get(0).getAccountCode());
    }

    @Test
    void otpAutoGeneradoConCuenta() {
        service.sendAutoGenerate("3157132338", null, 5, "INALAMBRIA_OTP");

        assertEquals("INALAMBRIA_OTP", dispatch.sent.get(0).getAccountCode());
        assertEquals(MessageType.OTP, dispatch.sent.get(0).getType());
    }

    @Test
    void otpInvalidoNoEnvia() {
        OtpSendResult r = service.send(SendOtpRequest.builder().phoneNumber("3157132338").otpCode("12").message("x").build(), null);
        assertFalse(r.isSuccess());
        assertEquals("VALIDATION_ERROR", r.getErrorCode());
        assertTrue(dispatch.sent.isEmpty());
    }

    @Test
    void conPinEnviaYActualizaUsuario() {
        OtpSendResult r = service.send(pinRequest(), null);

        assertTrue(r.isSuccess());
        assertTrue(r.isPinUpdated());
        String pin = usuario.last.getPin();
        assertEquals(6, pin.length());
        assertTrue(dispatch.sent.get(0).getText().contains(pin));
    }

    @Test
    void conPinFallaUsuarioPeroOtpSalio() {
        usuario.ok = false;
        OtpSendResult r = service.send(pinRequest(), null);

        assertFalse(r.isSuccess());
        assertTrue(r.isOtpSent());
        assertEquals("PIN_UPDATE_FAILED", r.getErrorCode());
    }

    @Test
    void fallaElEnvioNoActualizaPin() {
        dispatch.result = dispatch.result.toBuilder().status(DispatchStatus.FALLIDO)
                .errorCode(DispatchResult.ERROR_TODAS_FALLARON).detail("500").build();
        OtpSendResult r = service.send(pinRequest(), null);

        assertFalse(r.isSuccess());
        assertEquals(DispatchResult.ERROR_TODAS_FALLARON, r.getErrorCode());
        assertNull(usuario.last);
    }

    private static SendOtpRequest pinRequest() {
        return SendOtpRequest.builder().phoneNumber("3157132338").tipoIdentificacion("CC")
                .nroIdentificacion("123456789").nombre("Juan").withPinUpdate(true).build();
    }

    static class FakeDispatch extends SmsDispatchService {
        final List<SmsDispatch> sent = new java.util.ArrayList<>();
        DispatchChannel channel;
        String provider;
        DispatchResult result = DispatchResult.builder().envioId(UUID.randomUUID()).status(DispatchStatus.ENVIADO)
                .providerCode("INALAMBRIA").accountCode("INALAMBRIA_OTP").httpStatus(200).attempts(1).build();

        FakeDispatch() {
            super(null, null, List.of());
        }

        @Override
        public DispatchResult dispatch(SmsDispatch message, DispatchChannel channel, String providerCode) {
            sent.add(message);
            this.channel = channel;
            this.provider = providerCode;
            return result;
        }
    }

    static class FakeUsuario implements UsuarioServicePort {
        boolean ok = true;
        ActualizarUsuarioDto last;

        @Override
        public ActualizarUsuarioResult guardarUsuario(ActualizarUsuarioDto u) {
            last = u;
            return ok ? ActualizarUsuarioResult.success("ok") : ActualizarUsuarioResult.failure("caído", "SERVER_ERROR_500");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}

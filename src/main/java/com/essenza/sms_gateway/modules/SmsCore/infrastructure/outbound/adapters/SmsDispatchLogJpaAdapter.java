package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.exception.DuplicateDispatchException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsDispatchLogPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioIntentoEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsCuentaRepository;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsEnvioIntentoRepository;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsEnvioRepository;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Persiste el log de envíos e intentos. Cada método abre su propia transacción
 * corta ({@code REQUIRES_NEW}) para que el log quede guardado aunque falle el resto.
 */
@Component
@RequiredArgsConstructor
public class SmsDispatchLogJpaAdapter implements SmsDispatchLogPort {

    private static final String TX = "maestraTransactionManager";

    private final SmsEnvioRepository envioRepository;
    private final SmsEnvioIntentoRepository intentoRepository;
    private final SmsProveedorRepository proveedorRepository;
    private final SmsCuentaRepository cuentaRepository;

    @Override
    @Transactional(transactionManager = TX, propagation = Propagation.REQUIRES_NEW)
    public UUID start(SmsDispatch message, DispatchChannel channel, String requestedProvider) {
        SmsEnvioEntity envio = new SmsEnvioEntity();
        envio.setUuid(message.getId());
        envio.setIdempotencyKey(message.getIdempotencyKey());
        envio.setCanal(channel.name());
        envio.setProveedorSolicitado(requestedProvider);
        envio.setTipoMensaje(message.getType() != null ? message.getType().name() : MessageType.NORMAL.name());
        envio.setDestino(truncate(message.getTo() != null ? message.getTo().trim() : "", 20));
        envio.setMensaje(truncate(message.getText() != null ? message.getText() : "", 1600));
        envio.setEstado(DispatchStatus.EN_PROCESO.name());
        envio.setTotalIntentos(0);
        envio.setFechaCreacion(LocalDateTime.now());
        try {
            envioRepository.saveAndFlush(envio);
        } catch (DataIntegrityViolationException e) {
            if (message.getIdempotencyKey() != null) {
                throw new DuplicateDispatchException(message.getIdempotencyKey());
            }
            throw e;
        }
        return envio.getUuid();
    }

    @Override
    @Transactional(transactionManager = TX, propagation = Propagation.REQUIRES_NEW)
    public void attempt(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome) {
        SmsEnvioIntentoEntity intento = new SmsEnvioIntentoEntity();
        intento.setEnvio(envioRepository.findByUuid(envioId).orElseThrow());
        intento.setPasoOrden(step.getOrder());
        intento.setNroIntento(attemptNumber);
        intento.setProveedor(proveedorRepository.getReferenceById(step.getAccount().getProviderId()));
        intento.setCuenta(cuentaRepository.getReferenceById(step.getAccount().getId()));
        intento.setExitoso(outcome.isSuccess());
        intento.setHttpStatus(outcome.getHttpStatus());
        intento.setDetalle(truncate(outcome.getDetail(), 1000));
        intento.setDuracionMs((int) Math.min(Integer.MAX_VALUE, outcome.getDurationMs()));
        intento.setFecha(LocalDateTime.now());
        intentoRepository.save(intento);
    }

    @Override
    @Transactional(transactionManager = TX, propagation = Propagation.REQUIRES_NEW)
    public void finish(DispatchResult result, RouteStep lastStep) {
        envioRepository.findByUuid(result.getEnvioId()).ifPresent(envio -> {
            envio.setEstado(result.getStatus().name());
            envio.setTotalIntentos(result.getAttempts());
            envio.setHttpStatusFinal(result.getHttpStatus());
            envio.setCodigoError(result.getErrorCode());
            envio.setDetalleError(truncate(result.getDetail(), 1000));
            if (lastStep != null) {
                envio.setProveedorFinal(proveedorRepository.getReferenceById(lastStep.getAccount().getProviderId()));
                envio.setCuentaFinal(cuentaRepository.getReferenceById(lastStep.getAccount().getId()));
            }
            envio.setFechaFinalizacion(LocalDateTime.now());
            envioRepository.save(envio);
        });
    }

    @Override
    @Transactional(transactionManager = TX, readOnly = true)
    public Optional<DispatchResult> findByIdempotencyKey(String idempotencyKey) {
        return envioRepository.findByIdempotencyKey(idempotencyKey)
                .map(e -> DispatchResult.builder()
                        .envioId(e.getUuid())
                        .status(DispatchStatus.valueOf(e.getEstado()))
                        .providerCode(e.getProveedorFinal() != null ? e.getProveedorFinal().getCodigo() : null)
                        .accountCode(e.getCuentaFinal() != null ? e.getCuentaFinal().getCodigo() : null)
                        .httpStatus(e.getHttpStatusFinal())
                        .attempts(e.getTotalIntentos() != null ? e.getTotalIntentos() : 0)
                        .errorCode(e.getCodigoError())
                        .detail(e.getDetalleError())
                        .build());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

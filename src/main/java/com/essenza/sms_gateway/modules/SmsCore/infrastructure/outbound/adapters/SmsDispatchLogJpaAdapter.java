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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persiste el log de envíos e intentos. Cada método abre su propia transacción
 * corta ({@code REQUIRES_NEW}) para que el log quede guardado aunque falle el resto.
 */
@Component
@RequiredArgsConstructor
public class SmsDispatchLogJpaAdapter implements SmsDispatchLogPort {

    private final SmsEnvioRepository envioRepository;
    private final SmsEnvioIntentoRepository intentoRepository;
    private final SmsProveedorRepository proveedorRepository;
    private final SmsCuentaRepository cuentaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(SmsDispatch message, DispatchChannel channel, String requestedProvider) {
        SmsEnvioEntity envio = newEnvio(message, channel, requestedProvider);
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

    /** Masivo: todos los envíos en una sola transacción (sin idempotencia por destinatario). */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> startAll(List<SmsDispatch> messages, DispatchChannel channel, String requestedProvider) {
        List<SmsEnvioEntity> envios = messages.stream().map(m -> newEnvio(m, channel, requestedProvider)).toList();
        envioRepository.saveAll(envios);
        return envios.stream().map(SmsEnvioEntity::getUuid).toList();
    }

    private static SmsEnvioEntity newEnvio(SmsDispatch message, DispatchChannel channel, String requestedProvider) {
        SmsEnvioEntity envio = new SmsEnvioEntity();
        envio.setUuid(message.getId());
        envio.setIdempotencyKey(message.getIdempotencyKey());
        envio.setCanal(channel.name());
        envio.setProveedorSolicitado(requestedProvider != null ? requestedProvider
                : (message.getPreferredProvider() != null ? truncate(message.getPreferredProvider().trim().toUpperCase(), 30) : null));
        envio.setTipoMensaje(message.getType() != null ? message.getType().name() : MessageType.NORMAL.name());
        envio.setDestino(truncate(message.getTo() != null ? message.getTo().trim() : "", 20));
        envio.setMensaje(truncate(message.getText() != null ? message.getText() : "", 1600));
        envio.setPlantillaCodigo(truncate(message.getTemplateCode(), 50));
        envio.setEstado(DispatchStatus.EN_PROCESO.name());
        envio.setTotalIntentos(0);
        // fecha de creación: AuditInfo.createdAt (@CreationTimestamp)
        return envio;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attempt(UUID envioId, RouteStep step, int attemptNumber, SendOutcome outcome) {
        intentoRepository.save(newIntento(envioRepository.findByUuid(envioId).orElseThrow(), step, attemptNumber, outcome));
    }

    /** Masivo: el mismo intento (un lote) para todos los envíos, en una sola transacción. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attemptAll(List<UUID> envioIds, RouteStep step, int attemptNumber, SendOutcome outcome) {
        List<SmsEnvioIntentoEntity> intentos = new ArrayList<>(envioIds.size());
        for (SmsEnvioEntity envio : envioRepository.findByUuidIn(envioIds)) {
            intentos.add(newIntento(envio, step, attemptNumber, outcome));
        }
        intentoRepository.saveAll(intentos);
    }

    private SmsEnvioIntentoEntity newIntento(SmsEnvioEntity envio, RouteStep step, int attemptNumber, SendOutcome outcome) {
        SmsEnvioIntentoEntity intento = new SmsEnvioIntentoEntity();
        intento.setEnvio(envio);
        intento.setPasoOrden(step.getOrder());
        intento.setNroIntento(attemptNumber);
        intento.setProveedor(proveedorRepository.getReferenceById(step.getAccount().getProviderId()));
        intento.setCuenta(cuentaRepository.getReferenceById(step.getAccount().getId()));
        intento.setExitoso(outcome.isSuccess());
        intento.setHttpStatus(outcome.getHttpStatus());
        intento.setDetalle(truncate(outcome.getDetail(), 1000));
        intento.setDuracionMs((int) Math.min(Integer.MAX_VALUE, outcome.getDurationMs()));
        intento.setFecha(LocalDateTime.now());
        return intento;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(DispatchResult result, RouteStep lastStep) {
        envioRepository.findByUuid(result.getEnvioId()).ifPresent(envio -> {
            apply(envio, result, lastStep);
            envioRepository.save(envio);
        });
    }

    /** Masivo: cierra en una sola transacción los envíos que terminaron en el mismo paso. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishAll(List<DispatchResult> results, RouteStep lastStep) {
        Map<UUID, DispatchResult> byId = new HashMap<>();
        results.forEach(r -> byId.put(r.getEnvioId(), r));
        List<SmsEnvioEntity> envios = envioRepository.findByUuidIn(byId.keySet());
        envios.forEach(e -> apply(e, byId.get(e.getUuid()), lastStep));
        envioRepository.saveAll(envios);
    }

    private void apply(SmsEnvioEntity envio, DispatchResult result, RouteStep lastStep) {
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
    }

    @Override
    @Transactional(readOnly = true)
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

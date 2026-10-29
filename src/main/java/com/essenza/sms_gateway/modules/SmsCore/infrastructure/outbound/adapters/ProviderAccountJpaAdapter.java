package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers.ProviderAccountFactory;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.CanalCuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lee cuentas de proveedor desde {@code CanalCuenta}.
 */
@Component
@RequiredArgsConstructor
public class ProviderAccountJpaAdapter implements ProviderAccountQueryPort {

    private final CanalCuentaRepository cuentaRepository;
    private final ProviderAccountFactory accountFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderAccount> findActive(String providerCode, String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            return Optional.empty();
        }
        String code = accountCode.trim();
        return (providerCode == null
                ? cuentaRepository.findByCodigo(code)
                : cuentaRepository.findByCodigoAndCanalProveedor_Codigo(code, providerCode.toUpperCase()))
                .filter(accountFactory::isUsable)
                .map(accountFactory::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderAccount> findFirstActive(String providerCode) {
        return cuentaRepository.findFirstByCanalProveedor_CodigoAndEliminadoFalseOrderByIdCanalCuentaAsc(providerCode.toUpperCase())
                .filter(accountFactory::isUsable)
                .map(accountFactory::toAccount);
    }
}

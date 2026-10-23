package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.ProviderAccountQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers.ProviderAccountFactory;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsCuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lee cuentas de proveedor desde {@code sms_cuenta}.
 */
@Component
@RequiredArgsConstructor
public class ProviderAccountJpaAdapter implements ProviderAccountQueryPort {

    private final SmsCuentaRepository cuentaRepository;
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
                : cuentaRepository.findByCodigoAndProveedor_Codigo(code, providerCode.toUpperCase()))
                .filter(accountFactory::isUsable)
                .map(accountFactory::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderAccount> findFirstActive(String providerCode) {
        return cuentaRepository.findFirstByProveedor_CodigoAndEliminadoFalseOrderByIdSmsCuentaAsc(providerCode.toUpperCase())
                .filter(accountFactory::isUsable)
                .map(accountFactory::toAccount);
    }
}

package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Lista los proveedores con módulo implementado a partir de los {@link SmsSenderPort} registrados en Spring. */
@Component
public class SmsSenderCatalogAdapter implements ProveedorCatalogPort {

    private final Set<String> implementados;

    public SmsSenderCatalogAdapter(List<SmsSenderPort> senders) {
        TreeSet<String> codigos = senders.stream()
                .map(s -> s.providerCode().toUpperCase())
                .collect(Collectors.toCollection(TreeSet::new));
        this.implementados = Collections.unmodifiableSortedSet(codigos);
    }

    @Override
    public Set<String> implementados() {
        return implementados;
    }
}

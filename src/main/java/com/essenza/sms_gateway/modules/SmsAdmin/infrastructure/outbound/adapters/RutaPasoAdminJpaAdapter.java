package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoEntityMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaPasoEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminCuentaRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaPasoRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adapter JPA de {@code sms_ruta_paso}. */
@Component
@RequiredArgsConstructor
public class RutaPasoAdminJpaAdapter implements RutaPasoAdminPort {

    private final AdminRutaPasoRepository pasoRepository;
    private final AdminRutaRepository rutaRepository;
    private final AdminCuentaRepository cuentaRepository;
    private final RutaPasoEntityMapper mapper;

    @Override
    public List<Paso> findByRuta(Long rutaId) {
        return pasoRepository.findByRuta_IdSmsRutaOrderByOrdenAsc(rutaId).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Paso> replace(Long rutaId, List<Paso> pasos) {
        pasoRepository.deleteByRutaId(rutaId);
        List<AdminRutaPasoEntity> entities = pasos.stream()
                .map(p -> {
                    AdminRutaPasoEntity e = mapper.toEntity(p);
                    e.setRuta(rutaRepository.getReferenceById(rutaId));
                    e.setCuenta(cuentaRepository.getReferenceById(p.getCuentaId()));
                    return e;
                })
                .toList();
        pasoRepository.saveAllAndFlush(entities);
        return findByRuta(rutaId);
    }

    @Override
    public void deleteByRuta(Long rutaId) {
        pasoRepository.deleteByRutaId(rutaId);
    }

    @Override
    public List<Long> rutasQueUsanCuenta(Long cuentaId) {
        return pasoRepository.findRutaIdsByCuentaId(cuentaId);
    }
}

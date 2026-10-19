package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RutaPasoAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.mappers.RutaPasoPersistenceMapper;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminCuentaRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaPasoRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.repository.AdminRutaRepository;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaEntity;
import com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity.AdminRutaPasoEntity;
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
    private final RutaPasoPersistenceMapper mapper;

    @Override
    public List<Paso> findByRuta(Long rutaId) {
        return pasoRepository.findByRuta_IdOrderByOrdenAsc(rutaId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Paso> replace(Long rutaId, List<Paso> pasos) {
        // El borrado se ejecuta de inmediato para no chocar con el UNIQUE (ruta_id, orden) al insertar
        pasoRepository.deleteByRutaId(rutaId);
        AdminRutaEntity ruta = rutaRepository.getReferenceById(rutaId);
        for (Paso p : pasos) {
            AdminRutaPasoEntity e = mapper.toEntity(p);
            e.setRuta(ruta);
            e.setCuenta(cuentaRepository.getReferenceById(p.getCuentaId()));
            pasoRepository.save(e);
        }
        pasoRepository.flush();
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

package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.TipoMensajeRef;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.TipoMensajeConsultaPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsTipoMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TipoMensajeConsultaJpaAdapter implements TipoMensajeConsultaPort {

    private final PlantillaSmsTipoMensajeRepository repository;

    @Override
    public Optional<TipoMensajeRef> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .map(t -> new TipoMensajeRef(t.getIdSmsTipoMensaje(), t.getCodigo(), Boolean.FALSE.equals(t.getEliminado())));
    }
}

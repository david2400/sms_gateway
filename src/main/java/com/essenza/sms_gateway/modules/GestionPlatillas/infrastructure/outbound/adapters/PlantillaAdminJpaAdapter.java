package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.Plantilla;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.PlantillaVariable;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaAdminPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.mappers.PlantillaMapper;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.entity.PlantillaSmsVariableEntity;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsRepository;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsTipoMensajeRepository;
import com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.outbound.persistence.repository.PlantillaSmsVariableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Adapter JPA de la administración de plantillas (variables en cascada, sincronizadas por nombre al guardar). */
@Component
@RequiredArgsConstructor
public class PlantillaAdminJpaAdapter implements PlantillaAdminPort {

    private final PlantillaSmsRepository repository;
    private final PlantillaSmsTipoMensajeRepository tipos;
    private final PlantillaSmsVariableRepository variables;
    private final PlantillaMapper mapper;

    @Override
    public List<Plantilla> findAll() {
        return repository.findAllByOrderByCodigoAsc().stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Plantilla> findById(Long id) {
        return repository.findWithVariablesByIdSmsPlantilla(id).map(mapper::toModel);
    }

    @Override
    public Optional<Plantilla> findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toModel);
    }

    @Override
    @Transactional
    public Plantilla save(Plantilla p) {
        if (p.getId() != null && !p.getVariables().isEmpty()) {
            variables.purgarEliminadas(p.getId(), p.getVariables().stream().map(PlantillaVariable::getNombre).toList());
        }
        PlantillaSmsEntity e = p.getId() != null
                ? repository.findWithVariablesByIdSmsPlantilla(p.getId()).orElseThrow()
                : new PlantillaSmsEntity();
        mapper.updateEntity(p, e);
        e.setTipoMensaje(p.getTipoMensajeId() == null ? null : tipos.getReferenceById(p.getTipoMensajeId()));
        sincronizarVariables(e, p.getVariables());
        return mapper.toModel(repository.saveAndFlush(e));
    }

    /**
     * Actualiza las variables EN SITIO por nombre (sin distinguir mayúsculas, como el UNIQUE de MySQL): las que siguen
     * se actualizan, las nuevas se insertan y las que ya no vienen se eliminan (soft delete por orphanRemoval).
     * Borrar todas y reinsertarlas chocaba con el UNIQUE (plantilla_id, nombre): con @SoftDelete las "borradas"
     * siguen en la tabla.
     */
    private void sincronizarVariables(PlantillaSmsEntity e, List<PlantillaVariable> nuevas) {
        Map<String, PlantillaSmsVariableEntity> actuales = new HashMap<>();
        e.getVariables().forEach(v -> actuales.put(clave(v.getNombre()), v));
        List<PlantillaSmsVariableEntity> resultado = new ArrayList<>();
        for (PlantillaVariable v : nuevas) {
            PlantillaSmsVariableEntity ve = actuales.remove(clave(v.getNombre()));
            if (ve == null) {
                ve = mapper.toEntity(v);
                ve.setPlantilla(e);
            } else {
                mapper.updateEntity(v, ve);
            }
            ve.setTipoDato(v.getTipoDato().name());
            resultado.add(ve);
        }
        e.getVariables().clear();
        e.getVariables().addAll(resultado);
    }

    private static String clave(String nombre) {
        return nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
    }
}


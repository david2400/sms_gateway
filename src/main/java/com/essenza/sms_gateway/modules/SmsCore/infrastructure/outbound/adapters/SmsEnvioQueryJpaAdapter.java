package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.PageResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioDetalleResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioFilter;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsEnvioQueryPort;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.mappers.SmsEnvioPersistenceMapper;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity.SmsEnvioEntity;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsEnvioIntentoRepository;
import com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.repository.SmsEnvioRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta del log de envíos con filtros dinámicos.
 */
@Component
@RequiredArgsConstructor
public class SmsEnvioQueryJpaAdapter implements SmsEnvioQueryPort {

    private final SmsEnvioRepository envioRepository;
    private final SmsEnvioIntentoRepository intentoRepository;
    private final SmsEnvioPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SmsEnvioDetalleResponse> findById(UUID envioId) {
        return envioRepository.findWithFinalByUuid(envioId).map(entity -> {
            SmsEnvioDetalleResponse detalle = mapper.toDetalle(entity);
            detalle.setIntentos(mapper.toIntentos(intentoRepository.findByEnvio_UuidOrderByIdSmsEnvioIntentoAsc(envioId)));
            return detalle;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SmsEnvioDetalleResponse> search(SmsEnvioFilter filter) {
        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        Page<SmsEnvioEntity> page = envioRepository.findAll(toSpecification(filter), pageRequest);
        return PageResult.<SmsEnvioDetalleResponse>builder()
                .content(page.getContent().stream().map(mapper::toDetalle).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private Specification<SmsEnvioEntity> toSpecification(SmsEnvioFilter f) {
        return (root, query, cb) -> {
            // Evita N+1 al mapear proveedor/cuenta final (no aplica al count)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("proveedorFinal", JoinType.LEFT);
                root.fetch("cuentaFinal", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (f.getDestino() != null && !f.getDestino().isBlank()) {
                predicates.add(cb.equal(root.get("destino"), f.getDestino().trim()));
            }
            if (f.getEstado() != null) {
                predicates.add(cb.equal(root.get("estado"), f.getEstado().name()));
            }
            if (f.getTipoMensaje() != null) {
                predicates.add(cb.equal(root.get("tipoMensaje"), f.getTipoMensaje().name()));
            }
            if (f.getProveedor() != null && !f.getProveedor().isBlank()) {
                predicates.add(cb.equal(root.get("proveedorFinal").get("codigo"),
                        f.getProveedor().trim().toUpperCase()));
            }
            if (f.getDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), f.getDesde()));
            }
            if (f.getHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), f.getHasta()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

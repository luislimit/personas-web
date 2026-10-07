package es.gestionpersonas.entidad.servicio;

import es.gestionpersonas.cuenta.dto.CuentaDtos;
import es.gestionpersonas.cuenta.repositorio.RepositorioCuentaUsuario;
import es.gestionpersonas.comun.*;
import es.gestionpersonas.entidad.dto.EntidadDtos;
import es.gestionpersonas.entidad.modelo.*;
import es.gestionpersonas.entidad.repositorio.*;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.AccesoActual;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ServicioEntidad {
    private final RepositorioEntidad entityRepository;
    private final RepositorioTipoEntidad typeRepository;
    private final RepositorioPersona personRepository;
    private final RepositorioContactoEntidad contactRepository;
    private final RepositorioRelacionEntidad relationRepository;
    private final RepositorioTipoRelacion relationTypeRepository;
    private final RepositorioCuentaUsuario accountRepository;
    private final ServicioControlAcceso controlAcceso;

    public ServicioEntidad(
        RepositorioEntidad e,
        RepositorioTipoEntidad t,
        RepositorioPersona p,
        RepositorioContactoEntidad c,
        RepositorioRelacionEntidad r,
        RepositorioTipoRelacion rt,
        RepositorioCuentaUsuario a,
        ServicioControlAcceso controlAcceso
    ) {
        entityRepository = e;
        typeRepository = t;
        personRepository = p;
        contactRepository = c;
        relationRepository = r;
        relationTypeRepository = rt;
        accountRepository = a;
        this.controlAcceso = controlAcceso;
    }

    public List<EntidadDtos.Resumen> search(String type, String status, String name) {
        AccesoActual acceso = ContextoAccesoActual.require();

        return entityRepository.search(blank(type), blank(status), blank(name)).stream()
            .filter(e -> controlAcceso.hasPermissionForEntity(acceso, "ENTIDAD.LEER", e.getId()))
            .map(this::summary)
            .toList();
    }

    public EntidadDtos.Detalle get(Long id) {
        requirePermission(id, "ENTIDAD.LEER");
        Entidad e = entityRepository.findDetailedById(id)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe la entidad " + id));

        var p = personRepository.findById(id)
            .map(x -> new EntidadDtos.DatosPersona(
                x.getFirstName(),
                x.getLastName(),
                x.getSecondLastName(),
                x.getBirthDate(),
                x.getGender()
            ))
            .orElse(null);

        return new EntidadDtos.Detalle(
            e.getId(),
            e.getEntityType().getCode(),
            e.getName(),
            e.getStatus(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            p
        );
    }

    @Transactional
    public EntidadDtos.Detalle create(EntidadDtos.CrearEntidad in) {
        AccesoActual acceso = ContextoAccesoActual.require();
        controlAcceso.requirePermissionForEntity(acceso, "ENTIDAD.CREAR", acceso.contextEntityId());

        String code = in.codigoTipo().trim().toUpperCase();
        TipoEntidad type = typeRepository.findByCode(code)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe tipo_entidad " + code));

        String status = normEntityStatus(in.estado() == null ? "ACTIVO" : in.estado());

        if ("PERSONA".equals(code) && in.persona() == null) {
            throw new IllegalArgumentException("persona es obligatorio para una entidad PERSONA");
        }

        Entidad e = entityRepository.save(new Entidad(type, in.nombre().trim(), status));

        if ("PERSONA".equals(code)) {
            var p = in.persona();
            personRepository.save(
                new Persona(
                    e,
                    p.nombre().trim(),
                    p.primerApellido().trim(),
                    p.segundoApellido(),
                    p.fechaNacimiento(),
                    p.genero()
                )
            );
        }

        vincularNuevaEntidadAlContexto(acceso, e.getId());

        return get(e.getId());
    }

    @Transactional
    public EntidadDtos.Detalle update(Long id, EntidadDtos.ModificarEntidad in) {
        requirePermission(id, "ENTIDAD.MODIFICAR");

        Entidad e = requireDetailed(id);

        if (in.estado() != null) {
            normEntityStatus(in.estado());
        }

        e.update(
            trimOrNull(in.nombre()),
            in.estado() == null ? null : in.estado().trim().toUpperCase()
        );

        if (in.persona() != null) {
            if (!"PERSONA".equals(e.getEntityType().getCode())) {
                throw new IllegalArgumentException(
                    "persona solo puede modificarse en entidades PERSONA"
                );
            }

            Persona p = personRepository.findById(id)
                .orElseThrow(
                    () -> new ExcepcionNoEncontrado(
                        "No existen datos de persona para la entidad " + id
                    )
                );

            var pi = in.persona();
            p.update(
                pi.nombre().trim(),
                pi.primerApellido().trim(),
                pi.segundoApellido(),
                pi.fechaNacimiento(),
                pi.genero()
            );
        }

        return get(id);
    }

    @Transactional
    public void archive(Long id) {
        requirePermission(id, "ENTIDAD.ELIMINAR");
        Entidad e = requireDetailed(id);
        e.update(null, "ARCHIVADO");
    }

    public List<EntidadDtos.Contacto> contacts(Long id) {
        requireScope(id);
        require(id);

        return contactRepository
            .findByEntityIdOrderByContactTypeAscPreferredDescIdAsc(id)
            .stream()
            .map(this::contact)
            .toList();
    }

    @Transactional
    public EntidadDtos.Contacto addContact(Long entityId, EntidadDtos.CrearContacto in) {
        requireScope(entityId);
        require(entityId);

        String type = in.tipo().trim().toUpperCase();
        boolean active = in.activo() == null || in.activo();
        boolean preferred = Boolean.TRUE.equals(in.preferido()) && active;

        if (preferred) {
            clearPreferred(entityId, type, null);
        }

        ContactoEntidad c = contactRepository.save(
            new ContactoEntidad(
                entityId,
                type,
                in.valor().trim(),
                in.etiqueta(),
                preferred,
                active,
                in.fechaVerificacion(),
                in.validoDesde(),
                in.validoHasta()
            )
        );

        return contact(c);
    }

    @Transactional
    public EntidadDtos.Contacto updateContact(
        Long entityId,
        Long contactId,
        EntidadDtos.ModificarContacto in
    ) {
        requireScope(entityId);
        require(entityId);

        ContactoEntidad c = contactRepository.findByIdAndEntityId(contactId, entityId)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe el contacto " + contactId + " en la entidad " + entityId
                )
            );

        String newType = in.tipo() == null
            ? c.getContactType()
            : in.tipo().trim().toUpperCase();

        boolean willActive = in.activo() == null ? c.isActive() : in.activo();
        boolean willPreferred = in.preferido() == null
            ? c.isPreferred()
            : in.preferido();

        if (willPreferred && willActive) {
            clearPreferred(entityId, newType, contactId);
        }

        c.update(
            in.tipo() == null ? null : newType,
            trimOrNull(in.valor()),
            in.etiqueta(),
            in.preferido(),
            in.activo(),
            in.fechaVerificacion(),
            in.validoDesde(),
            in.validoHasta()
        );

        if (!c.isActive()) {
            c.setPreferred(false);
        }

        return contact(c);
    }

    @Transactional
    public void deleteContact(Long entityId, Long contactId) {
        requireScope(entityId);
        require(entityId);

        ContactoEntidad c = contactRepository.findByIdAndEntityId(contactId, entityId)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe el contacto " + contactId));

        c.deactivate();
    }

    public List<CuentaDtos.Cuenta> accounts(Long id) {
        controlAcceso.requirePermissionForEntity(ContextoAccesoActual.require(), "CUENTA.LEER", id);
        require(id);

        return accountRepository.findByPersonEntityIdOrderByPreferredDescIdAsc(id).stream()
            .map(a -> new CuentaDtos.Cuenta(
                a.getId(),
                a.getPersonEntityId(),
                a.getUsername(),
                a.getLoginEmail(),
                a.getAuthProvider(),
                a.isPreferred(),
                a.getStatus(),
                a.getLastLoginAt()
            ))
            .toList();
    }

    public List<EntidadDtos.Relacion> relations(Long id) {
        requirePermission(id, "ENTIDAD.LEER");
        require(id);

        AccesoActual acceso = ContextoAccesoActual.require();

        return relationRepository.findAllForEntity(id).stream()
            .filter(r -> {
                Long relatedId = Objects.equals(r.getSourceEntityId(), id)
                    ? r.getTargetEntityId()
                    : r.getSourceEntityId();
                return controlAcceso.hasPermissionForEntity(acceso, "ENTIDAD.LEER", relatedId);
            })
            .map(r -> relationView(id, r))
            .toList();
    }

    @Transactional
    public EntidadDtos.Relacion addRelation(Long sourceId, EntidadDtos.CrearRelacion in) {
        requirePermission(sourceId, "ENTIDAD.MODIFICAR");
        requirePermission(in.entidadDestinoId(), "ENTIDAD.MODIFICAR");
        require(sourceId);
        require(in.entidadDestinoId());

        if (Objects.equals(sourceId, in.entidadDestinoId())) {
            throw new IllegalArgumentException(
                "Una relación no puede apuntar a la misma entidad"
            );
        }

        String code = in.codigoTipoRelacion().trim().toUpperCase();

        TipoRelacion type = relationTypeRepository.findByCode(code)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe tipo_relacion " + code
                )
            );

        String status = normRelationStatus(
            in.estado() == null ? "ACTIVO" : in.estado()
        );

        RelacionEntidad r = relationRepository.save(
            new RelacionEntidad(
                sourceId,
                in.entidadDestinoId(),
                type,
                in.validoDesde(),
                in.validoHasta(),
                status
            )
        );

        return relationView(sourceId, r);
    }

    @Transactional
    public EntidadDtos.Relacion updateRelation(
        Long entityId,
        Long relationId,
        EntidadDtos.ModificarRelacion in
    ) {
        requirePermission(entityId, "ENTIDAD.MODIFICAR");
        require(entityId);

        RelacionEntidad r = relationRepository.findDetailedById(relationId)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe la relación " + relationId
                )
            );

        ensureRelationMember(entityId, r);

        Long relatedId = Objects.equals(r.getSourceEntityId(), entityId)
            ? r.getTargetEntityId()
            : r.getSourceEntityId();

        requirePermission(relatedId, "ENTIDAD.MODIFICAR");

        String status = in.estado() == null
            ? null
            : normRelationStatus(in.estado());

        r.update(in.validoDesde(), in.validoHasta(), status);

        return relationView(entityId, r);
    }

    @Transactional
    public void endRelation(Long entityId, Long relationId) {
        requirePermission(entityId, "ENTIDAD.MODIFICAR");
        require(entityId);

        RelacionEntidad r = relationRepository.findDetailedById(relationId)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe la relación " + relationId
                )
            );

        ensureRelationMember(entityId, r);
        Long relatedId = Objects.equals(r.getSourceEntityId(), entityId)
            ? r.getTargetEntityId()
            : r.getSourceEntityId();
        requirePermission(relatedId, "ENTIDAD.MODIFICAR");
        r.end();
    }

    private void vincularNuevaEntidadAlContexto(AccesoActual acceso, Long entityId) {
        if (acceso == null
            || acceso.contextEntityId() == null
            || "ADMIN_PLATAFORMA".equalsIgnoreCase(acceso.roleCode())
            || Objects.equals(acceso.contextEntityId(), entityId)) {
            return;
        }

        TipoRelacion pertenencia = relationTypeRepository.findByCode("PERTENENCIA")
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe el tipo de relación estructural PERTENENCIA"
                )
            );

        relationRepository.save(
            new RelacionEntidad(
                acceso.contextEntityId(),
                entityId,
                pertenencia,
                LocalDate.now(),
                null,
                "ACTIVO"
            )
        );
    }

    private void clearPreferred(Long entityId, String type, Long except) {
        for (ContactoEntidad c :
            contactRepository.findByEntityIdAndContactTypeAndActiveTrueAndPreferredTrue(
                entityId,
                type
            )) {
            if (except == null || !except.equals(c.getId())) {
                c.setPreferred(false);
            }
        }
    }

    private EntidadDtos.Relacion relationView(Long id, RelacionEntidad r) {
        boolean f = Objects.equals(r.getSourceEntityId(), id);
        Long rid = f ? r.getTargetEntityId() : r.getSourceEntityId();

        Entidad related = entityRepository.findById(rid)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "Entidad relacionada inexistente: " + rid
                )
            );

        var rt = r.getRelationType();

        return new EntidadDtos.Relacion(
            r.getId(),
            rid,
            related.getName(),
            rt.getCode(),
            f ? rt.getForwardName() : rt.getReverseName(),
            f ? "DIRECTA" : "INVERSA",
            r.getValidFrom(),
            r.getValidTo(),
            r.getStatus()
        );
    }

    private void ensureRelationMember(Long id, RelacionEntidad r) {
        if (!Objects.equals(id, r.getSourceEntityId())
            && !Objects.equals(id, r.getTargetEntityId())) {
            throw new ExcepcionNoEncontrado(
                "La relación " + r.getId() + " no pertenece a la entidad " + id
            );
        }
    }

    private EntidadDtos.Contacto contact(ContactoEntidad c) {
        return new EntidadDtos.Contacto(
            c.getId(),
            c.getContactType(),
            c.getContactValue(),
            c.getLabel(),
            c.isPreferred(),
            c.isActive(),
            c.getVerifiedAt(),
            c.getValidFrom(),
            c.getValidTo()
        );
    }

    private EntidadDtos.Resumen summary(Entidad e) {
        return new EntidadDtos.Resumen(
            e.getId(),
            e.getEntityType().getCode(),
            e.getName(),
            e.getStatus()
        );
    }

    private Entidad requireDetailed(Long id) {
        return entityRepository.findDetailedById(id)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe la entidad " + id
                )
            );
    }

    private void require(Long id) {
        if (!entityRepository.existsById(id)) {
            throw new ExcepcionNoEncontrado("No existe la entidad " + id);
        }
    }

    private void requireScope(Long id) {
        controlAcceso.requireEntityAccess(ContextoAccesoActual.require(), id);
    }

    private void requirePermission(Long id, String permission) {
        controlAcceso.requirePermissionForEntity(ContextoAccesoActual.require(), permission, id);
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private String normEntityStatus(String s) {
        String x = s.trim().toUpperCase();
        if (!Set.of("ACTIVO", "INACTIVO", "ARCHIVADO").contains(x)) {
            throw new IllegalArgumentException("Estado de entidad no válido: " + s);
        }
        return x;
    }

    private String normRelationStatus(String s) {
        String x = s.trim().toUpperCase();
        if (!Set.of("ACTIVO", "INACTIVO", "FINALIZADO").contains(x)) {
            throw new IllegalArgumentException("Estado de relación no válido: " + s);
        }
        return x;
    }
}

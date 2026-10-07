package es.gestionpersonas.solicitud.servicio;

import es.gestionpersonas.comun.ExcepcionNoEncontrado;
import es.gestionpersonas.comun.ExcepcionProhibido;
import es.gestionpersonas.entidad.repositorio.RepositorioEntidad;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.AccesoActual;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import es.gestionpersonas.solicitud.dto.SolicitudDtos;
import es.gestionpersonas.solicitud.modelo.*;
import es.gestionpersonas.solicitud.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ServicioSolicitud {
    private final RepositorioSolicitud requests;
    private final RepositorioTipoSolicitud types;
    private final RepositorioParticipanteSolicitud participants;
    private final RepositorioMensajeSolicitud messages;
    private final RepositorioAccionSolicitud actions;
    private final RepositorioEntidad entities;
    private final ServicioControlAcceso controlAcceso;

    public ServicioSolicitud(
        RepositorioSolicitud r,
        RepositorioTipoSolicitud t,
        RepositorioParticipanteSolicitud p,
        RepositorioMensajeSolicitud m,
        RepositorioAccionSolicitud a,
        RepositorioEntidad e,
        ServicioControlAcceso controlAcceso
    ) {
        requests = r;
        types = t;
        participants = p;
        messages = m;
        actions = a;
        entities = e;
        this.controlAcceso = controlAcceso;
    }

    @Transactional(readOnly = true)
    public List<SolicitudDtos.Resumen> list() {
        AccesoActual acceso = ContextoAccesoActual.require();

        return requests.findAllDetailed().stream()
            .filter(r -> canAccessRequest(acceso, r))
            .map(r -> new SolicitudDtos.Resumen(
                r.getId(),
                r.getRequestType().getCode(),
                r.getRequestType().getName(),
                r.getSubject(),
                r.getCreatedByEntityId(),
                name(r.getCreatedByEntityId()),
                r.getRequestDate(),
                r.getGlobalStatus()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDtos.Detalle get(Long id) {
        Solicitud r = requireAccessible(id);

        var ps = participants.findByIdRequestIdOrderByIdRoleAsc(id).stream()
            .filter(p -> canExposeEntity(p.getId().getEntityId()))
            .map(this::pv)
            .toList();

        var ms = messages.findByRequestIdOrderByMessageDateAscIdAsc(id).stream()
            .filter(m -> canExposeEntity(m.getSenderEntityId()))
            .map(m -> new SolicitudDtos.VistaMensaje(
                m.getId(),
                m.getSenderEntityId(),
                name(m.getSenderEntityId()),
                m.getMessage(),
                m.getMessageDate()
            ))
            .toList();

        var as = actions.findByRequestIdOrderByCreatedAtAsc(id).stream()
            .filter(a -> a.getActorEntityId() == null || canExposeEntity(a.getActorEntityId()))
            .map(a -> new SolicitudDtos.VistaAccion(
                a.getId(),
                a.getActorEntityId(),
                a.getActorEntityId() == null ? null : name(a.getActorEntityId()),
                a.getActionType(),
                a.getCreatedAt(),
                a.getData()
            ))
            .toList();

        return new SolicitudDtos.Detalle(
            r.getId(),
            r.getRequestType().getCode(),
            r.getRequestType().getName(),
            r.getSubject(),
            r.getDescription(),
            r.getCreatedByEntityId(),
            name(r.getCreatedByEntityId()),
            r.getRequestDate(),
            r.getGlobalStatus(),
            r.getResolvedAt(),
            r.getClosedAt(),
            r.getParentRequestId(),
            r.getGeneratedEventId(),
            r.getGeneratedDocumentId(),
            ps,
            ms,
            as
        );
    }

    @Transactional
    public SolicitudDtos.Detalle create(SolicitudDtos.CrearSolicitud in) {
        AccesoActual acceso = ContextoAccesoActual.require();

        TipoSolicitud t = types.findByCode(in.codigoTipoSolicitud().trim().toUpperCase())
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe tipo_solicitud " + in.codigoTipoSolicitud()
                )
            );

        entidad(in.creadoPorEntidadId());
        controlAcceso.requireEntityAccess(acceso, in.creadoPorEntidadId());

        for (var p : in.participantes()) {
            entidad(p.entidadId());
            controlAcceso.requireEntityAccess(acceso, p.entidadId());
        }

        Solicitud r = requests.save(
            new Solicitud(
                t,
                in.asunto().trim(),
                in.descripcion(),
                in.creadoPorEntidadId()
            )
        );

        for (var p : in.participantes()) {
            addParticipantInternal(r.getId(), p);
        }

        actions.save(
            new AccionSolicitud(
                r.getId(),
                in.creadoPorEntidadId(),
                "CREADA",
                Map.of()
            )
        );

        return get(r.getId());
    }

    @Transactional
    public SolicitudDtos.Detalle update(Long id, SolicitudDtos.ModificarSolicitud in) {
        Solicitud r = requireAccessible(id);

        String status = in.estadoGlobal() == null
            ? null
            : status(in.estadoGlobal());

        r.update(
            in.asunto(),
            in.descripcion(),
            status,
            in.eventoGeneradoId(),
            in.documentoGeneradoId()
        );

        return get(id);
    }

    @Transactional
    public SolicitudDtos.VistaMensaje addMessage(
        Long id,
        SolicitudDtos.AgregarMensaje in
    ) {
        requireAccessible(id);
        entidad(in.entidadRemitenteId());
        requireEntityInCurrentScope(in.entidadRemitenteId());

        MensajeSolicitud m = messages.save(
            new MensajeSolicitud(
                id,
                in.entidadRemitenteId(),
                in.mensaje()
            )
        );

        actions.save(
            new AccionSolicitud(
                id,
                in.entidadRemitenteId(),
                "MENSAJE_ANADIDO",
                Map.of()
            )
        );

        return new SolicitudDtos.VistaMensaje(
            m.getId(),
            m.getSenderEntityId(),
            name(m.getSenderEntityId()),
            m.getMessage(),
            m.getMessageDate()
        );
    }

    @Transactional
    public SolicitudDtos.VistaParticipante addParticipant(
        Long id,
        SolicitudDtos.EntradaParticipante in
    ) {
        requireAccessible(id);
        requireEntityInCurrentScope(in.entidadId());
        ParticipanteSolicitud p = addParticipantInternal(id, in);
        return pv(p);
    }

    @Transactional
    public SolicitudDtos.VistaParticipante updateParticipant(
        Long id,
        Long entityId,
        String rolParticipante,
        SolicitudDtos.ModificarParticipante in
    ) {
        requireAccessible(id);
        requireEntityInCurrentScope(entityId);

        ParticipanteSolicitudId pk = new ParticipanteSolicitudId(
            id,
            entityId,
            rolParticipante.trim().toUpperCase()
        );

        ParticipanteSolicitud p = participants.findById(pk)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe el participante indicado"
                )
            );

        p.setStatus(participantStatus(in.estado()));

        if (Boolean.TRUE.equals(in.marcarLeido())) {
            p.markRead();
        }

        return pv(p);
    }

    @Transactional
    public void removeParticipant(
        Long id,
        Long entityId,
        String rolParticipante
    ) {
        requireAccessible(id);
        requireEntityInCurrentScope(entityId);

        ParticipanteSolicitudId pk = new ParticipanteSolicitudId(
            id,
            entityId,
            rolParticipante.trim().toUpperCase()
        );

        if (!participants.existsById(pk)) {
            throw new ExcepcionNoEncontrado(
                "No existe el participante indicado"
            );
        }

        participants.deleteById(pk);
    }

    @Transactional
    public SolicitudDtos.VistaAccion action(
        Long id,
        SolicitudDtos.EntradaAccion in
    ) {
        Solicitud r = requireAccessible(id);

        if (in.entidadActorId() != null) {
            entidad(in.entidadActorId());
            requireEntityInCurrentScope(in.entidadActorId());
        }

        if (in.estadoGlobal() != null) {
            r.update(
                null,
                null,
                status(in.estadoGlobal()),
                null,
                null
            );
        }

        if (in.entidadParticipanteId() != null) {
            requireEntityInCurrentScope(in.entidadParticipanteId());

            String rolParticipante = in.rolParticipante() == null
                ? "DESTINATARIO"
                : in.rolParticipante().trim().toUpperCase();

            ParticipanteSolicitud p = participants.findById(
                new ParticipanteSolicitudId(
                    id,
                    in.entidadParticipanteId(),
                    rolParticipante
                )
            ).orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe el participante indicado"
                )
            );

            if (in.estadoParticipante() != null) {
                p.setStatus(
                    participantStatus(in.estadoParticipante())
                );
            }
        }

        AccionSolicitud a = actions.save(
            new AccionSolicitud(
                id,
                in.entidadActorId(),
                in.tipoAccion().trim().toUpperCase(),
                in.datos() == null ? Map.of() : in.datos()
            )
        );

        return new SolicitudDtos.VistaAccion(
            a.getId(),
            a.getActorEntityId(),
            a.getActorEntityId() == null
                ? null
                : name(a.getActorEntityId()),
            a.getActionType(),
            a.getCreatedAt(),
            a.getData()
        );
    }

    private ParticipanteSolicitud addParticipantInternal(
        Long id,
        SolicitudDtos.EntradaParticipante in
    ) {
        entidad(in.entidadId());

        String rolParticipante =
            in.rolParticipante().trim().toUpperCase();

        ParticipanteSolicitudId pk =
            new ParticipanteSolicitudId(
                id,
                in.entidadId(),
                rolParticipante
            );

        if (participants.existsById(pk)) {
            return participants.findById(pk).orElseThrow();
        }

        String initial = "SOLICITANTE".equals(rolParticipante)
            ? "EN_ESPERA"
            : "PENDIENTE";

        return participants.save(
            new ParticipanteSolicitud(pk, initial)
        );
    }

    private SolicitudDtos.VistaParticipante pv(ParticipanteSolicitud p) {
        return new SolicitudDtos.VistaParticipante(
            p.getId().getEntityId(),
            name(p.getId().getEntityId()),
            p.getId().getRole(),
            p.getParticipantStatus(),
            p.getReadAt(),
            p.getRespondedAt()
        );
    }

    private Solicitud requireAccessible(Long id) {
        Solicitud solicitud = require(id);
        AccesoActual acceso = ContextoAccesoActual.require();

        if (!canAccessRequest(acceso, solicitud)) {
            throw new ExcepcionProhibido(
                "La solicitud " + id
                    + " queda fuera del alcance del contexto activo"
            );
        }

        return solicitud;
    }

    private boolean canAccessRequest(
        AccesoActual acceso,
        Solicitud solicitud
    ) {
        if (controlAcceso.canAccessEntity(
            acceso,
            solicitud.getCreatedByEntityId()
        )) {
            return true;
        }

        return participants
            .findByIdRequestIdOrderByIdRoleAsc(solicitud.getId())
            .stream()
            .anyMatch(p ->
                controlAcceso.canAccessEntity(
                    acceso,
                    p.getId().getEntityId()
                )
            );
    }

    private boolean canExposeEntity(Long entityId) {
        return controlAcceso.canAccessEntity(
            ContextoAccesoActual.require(),
            entityId
        );
    }

    private void requireEntityInCurrentScope(Long entityId) {
        entidad(entityId);
        controlAcceso.requireEntityAccess(
            ContextoAccesoActual.require(),
            entityId
        );
    }

    private Solicitud require(Long id) {
        return requests.findDetailedById(id)
            .orElseThrow(
                () -> new ExcepcionNoEncontrado(
                    "No existe la solicitud " + id
                )
            );
    }

    private void entidad(Long id) {
        if (!entities.existsById(id)) {
            throw new ExcepcionNoEncontrado(
                "No existe la entidad " + id
            );
        }
    }

    private String name(Long id) {
        return entities.findById(id)
            .map(e -> e.getName())
            .orElse("Entidad " + id);
    }

    private String status(String s) {
        String x = s.trim().toUpperCase();

        if (!Set.of(
            "ABIERTA",
            "RESUELTA",
            "CANCELADO",
            "CERRADA"
        ).contains(x)) {
            throw new IllegalArgumentException(
                "Estado de solicitud no válido: " + s
            );
        }

        return x;
    }

    private String participantStatus(String s) {
        String x = s.trim().toUpperCase();

        if (!Set.of(
            "EN_ESPERA",
            "PENDIENTE",
            "COMPLETADA",
            "CANCELADO"
        ).contains(x)) {
            throw new IllegalArgumentException(
                "Estado de participante no válido: " + s
            );
        }

        return x;
    }
}

package es.gestionpersonas.documento.servicio;

import es.gestionpersonas.comun.*;
import es.gestionpersonas.documento.dto.*;
import es.gestionpersonas.documento.modelo.*;
import es.gestionpersonas.documento.repositorio.*;
import es.gestionpersonas.entidad.repositorio.RepositorioEntidad;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly=true)
public class ServicioDocumento {
    private static final Set<String> ESTADOS = Set.of("BORRADOR","DISPONIBLE","RETIRADO","SUSTITUIDO");

    private final RepositorioDocumento documentos;
    private final RepositorioAccesoDocumento accesos;
    private final RepositorioEntidad entidades;
    private final EntityManager em;
    private final ServicioControlAcceso controlAcceso;

    public ServicioDocumento(RepositorioDocumento documentos, RepositorioAccesoDocumento accesos,
                             RepositorioEntidad entidades, EntityManager em,
                             ServicioControlAcceso controlAcceso) {
        this.documentos=documentos;
        this.accesos=accesos;
        this.entidades=entidades;
        this.em=em;
        this.controlAcceso=controlAcceso;
    }

    public List<DocumentoDto> porDestinatario(Long entidadId) {
        comprobarEntidad(entidadId);
        AccesoActual acceso=ContextoAccesoActual.require();
        return documentos.findByEntidadDestinatariaIdOrderByFechaDocumentoDescIdDesc(entidadId).stream()
            .map(this::vista)
            .filter(d -> puedeLeerClasificacion(acceso,d))
            .toList();
    }

    public DocumentoDto obtener(Long id) {
        DocumentoDto dto=vista(requerir(id));
        exigirLecturaClasificacion(dto);
        return dto;
    }

    public List<DocumentoDtos.Acceso> accesos(Long id) {
        DocumentoDto dto=obtener(id);
        return accesos.findByDocumentoIdOrderByFechaAccesoDescIdDesc(dto.id()).stream()
            .map(a -> vistaAcceso(a.getId(), a.getCuentaUsuarioId(), a.getFechaAcceso()))
            .toList();
    }

    @Transactional
    public DocumentoDto crear(DocumentoDtos.Crear entrada) {
        comprobarEntidad(entrada.entidadEmisoraId());
        comprobarEntidad(entrada.entidadDestinatariaId());
        TipoConfigurado tipo=tipo(entrada.codigoTipo());
        exigirEscrituraClasificacion(tipo.codigoClasificacion(),entrada.entidadDestinatariaId());

        if(entrada.sustituyeADocumentoId()!=null) {
            Documento anterior=requerirBloqueado(entrada.sustituyeADocumentoId());
            validarSustitucion(anterior,tipo.id(),entrada.entidadDestinatariaId(),null);
        }

        Documento documento=documentos.save(new Documento(
            tipo.id(), entrada.entidadEmisoraId(), entrada.entidadDestinatariaId(),
            tipo.clasificacionId(), tipo.categoriaSensibleId(), entrada.nombre().trim(),
            entrada.descripcion(), entrada.referenciaArchivo().trim(), entrada.fechaDocumento(),
            entrada.sustituyeADocumentoId(), ContextoAccesoActual.require().accountId()
        ));
        return vista(documento);
    }

    @Transactional
    public DocumentoDto modificar(Long id, DocumentoDtos.Modificar entrada) {
        Documento documento=requerirBloqueado(id);
        bloquearSiAccedido(documento);
        exigirEscrituraClasificacion(codigoClasificacion(documento.getClasificacionId()),
            documento.getEntidadDestinatariaId());

        TipoConfigurado nuevoTipo=null;
        if(entrada.codigoTipo()!=null) {
            nuevoTipo=tipo(entrada.codigoTipo());
            exigirEscrituraClasificacion(nuevoTipo.codigoClasificacion(),
                entrada.entidadDestinatariaId()!=null ? entrada.entidadDestinatariaId() : documento.getEntidadDestinatariaId());
            if(documento.getSustituyeADocumentoId()!=null) {
                Documento anterior=requerirBloqueado(documento.getSustituyeADocumentoId());
                validarSustitucion(anterior,nuevoTipo.id(),
                    entrada.entidadDestinatariaId()!=null ? entrada.entidadDestinatariaId() : documento.getEntidadDestinatariaId(), documento.getId());
            }
        }

        if(entrada.entidadEmisoraId()!=null) comprobarEntidad(entrada.entidadEmisoraId());
        if(entrada.entidadDestinatariaId()!=null) comprobarEntidad(entrada.entidadDestinatariaId());

        if(entrada.entidadDestinatariaId()!=null && documento.getSustituyeADocumentoId()!=null) {
            Documento anterior=requerirBloqueado(documento.getSustituyeADocumentoId());
            validarSustitucion(anterior,
                nuevoTipo!=null ? nuevoTipo.id() : documento.getTipoDocumentoId(),
                entrada.entidadDestinatariaId(), documento.getId());
        }

        documento.modificar(
            nuevoTipo==null ? null : nuevoTipo.id(),
            nuevoTipo==null ? null : nuevoTipo.clasificacionId(),
            nuevoTipo==null ? documento.getCategoriaSensibleId() : nuevoTipo.categoriaSensibleId(),
            entrada.entidadEmisoraId(), entrada.entidadDestinatariaId(), entrada.nombre(),
            entrada.descripcion(), entrada.referenciaArchivo(), entrada.fechaDocumento()
        );

        if(entrada.estado()!=null) cambiarEstado(documento,normalizarEstado(entrada.estado()));
        return vista(documento);
    }

    @Transactional
    public DocumentoDtos.ResultadoAcceso acceder(Long id) {
        Documento documento=requerirBloqueado(id);
        DocumentoDto dto=vista(documento);
        exigirLecturaClasificacion(dto);

        if(!"DISPONIBLE".equals(documento.getEstado()))
            throw new ExcepcionConflicto("El documento no está disponible para acceso");

        TipoConfigurado tipo=tipo(documento.getTipoDocumentoId());
        long numeroAccesos=accesos.countByDocumentoId(documento.getId());
        if(tipo.maximoAccesos()!=null && numeroAccesos>=tipo.maximoAccesos())
            throw new ExcepcionConflicto("El documento ha alcanzado su máximo de accesos");

        AccesoDocumento acceso=accesos.save(new AccesoDocumento(
            documento.getId(), ContextoAccesoActual.require().accountId()
        ));
        return new DocumentoDtos.ResultadoAcceso(
            acceso.getId(), documento.getId(), documento.getReferenciaArchivo(), acceso.getFechaAcceso()
        );
    }

    @Transactional
    public void eliminar(Long id) {
        Documento documento=requerirBloqueado(id);
        exigirEscrituraClasificacion(codigoClasificacion(documento.getClasificacionId()),
            documento.getEntidadDestinatariaId());

        if(accesos.existsByDocumentoId(id))
            throw new ExcepcionConflicto("No se puede eliminar un documento que ya ha sido accedido");
        if(documentos.existsBySustituyeADocumentoId(id))
            throw new ExcepcionConflicto("No se puede eliminar un documento que ya ha sido sustituido por otro");

        if(documento.getSustituyeADocumentoId()!=null && "DISPONIBLE".equals(documento.getEstado())) {
            Documento anterior=requerirBloqueado(documento.getSustituyeADocumentoId());
            if("SUSTITUIDO".equals(anterior.getEstado())) anterior.cambiarEstado("DISPONIBLE");
        }
        documentos.delete(documento);
        documentos.flush();
    }

    private void cambiarEstado(Documento documento,String nuevoEstado) {
        if(Objects.equals(documento.getEstado(),nuevoEstado)) return;
        if(accesos.existsByDocumentoId(documento.getId()))
            throw new ExcepcionConflicto("Un documento accedido ya no puede modificarse; debe crearse un documento sustituto");

        switch(nuevoEstado) {
            case "DISPONIBLE" -> publicar(documento);
            case "RETIRADO" -> documento.cambiarEstado("RETIRADO");
            case "BORRADOR" -> {
                if(!"BORRADOR".equals(documento.getEstado()))
                    throw new ExcepcionConflicto("Solo un documento en borrador puede permanecer como BORRADOR");
            }
            case "SUSTITUIDO" -> throw new ExcepcionConflicto("El estado SUSTITUIDO solo se asigna automáticamente al publicar un sustituto");
            default -> throw new IllegalArgumentException("Estado de documento no válido: "+nuevoEstado);
        }
    }

    private void publicar(Documento documento) {
        if(!Set.of("BORRADOR","RETIRADO").contains(documento.getEstado()))
            throw new ExcepcionConflicto("Solo se puede publicar un documento BORRADOR o RETIRADO");

        if(documento.getSustituyeADocumentoId()!=null) {
            Documento anterior=requerirBloqueado(documento.getSustituyeADocumentoId());
            validarSustitucion(anterior,documento.getTipoDocumentoId(),documento.getEntidadDestinatariaId(),documento.getId());
            anterior.cambiarEstado("SUSTITUIDO");
        }
        documento.cambiarEstado("DISPONIBLE");
    }

    private void validarSustitucion(Documento anterior, Long tipoNuevo, Long destinatarioNuevo, Long documentoActualId) {
        if(!Objects.equals(anterior.getTipoDocumentoId(),tipoNuevo))
            throw new ExcepcionConflicto("Un documento solo puede sustituir a otro del mismo tipo");
        if(!Objects.equals(anterior.getEntidadDestinatariaId(),destinatarioNuevo))
            throw new ExcepcionConflicto("Un documento sustituto debe mantener la misma entidad destinataria");
        if(!accesos.existsByDocumentoId(anterior.getId()))
            throw new ExcepcionConflicto("Solo se puede sustituir un documento que ya haya sido accedido");
        boolean yaTieneSustituto = documentoActualId==null
            ? documentos.existsBySustituyeADocumentoId(anterior.getId())
            : documentos.existsBySustituyeADocumentoIdAndIdNot(anterior.getId(),documentoActualId);
        if(yaTieneSustituto)
            throw new ExcepcionConflicto("El documento ya tiene un sustituto");
    }

    private void bloquearSiAccedido(Documento documento) {
        if(accesos.existsByDocumentoId(documento.getId()))
            throw new ExcepcionConflicto("El documento ya ha sido accedido y es inmutable; cree un documento sustituto");
    }

    private boolean puedeLeerClasificacion(AccesoActual acceso, DocumentoDto documento) {
        return switch(documento.codigoClasificacion()) {
            case "SENSIBLE" -> controlAcceso.hasPermission(acceso,"INFORMACION_SENSIBLE.LEER");
            case "PERSONAL" -> controlAcceso.canReadPersonalData(acceso,documento.entidadDestinatariaId());
            default -> true;
        };
    }

    private void exigirLecturaClasificacion(DocumentoDto documento) {
        if(!puedeLeerClasificacion(ContextoAccesoActual.require(),documento))
            throw new ExcepcionProhibido("No tiene permiso para consultar este documento");
    }

    private void exigirEscrituraClasificacion(String codigoClasificacion,Long destinatarioId) {
        AccesoActual acceso=ContextoAccesoActual.require();
        if("SENSIBLE".equals(codigoClasificacion)
            && !controlAcceso.hasPermission(acceso,"INFORMACION_SENSIBLE.MODIFICAR"))
            throw new ExcepcionProhibido("Se requiere INFORMACION_SENSIBLE.MODIFICAR");
        if("PERSONAL".equals(codigoClasificacion)
            && !controlAcceso.canUpdatePersonalData(acceso,destinatarioId))
            throw new ExcepcionProhibido("No tiene permiso para modificar documentos personales de la entidad "+destinatarioId);
    }

    private DocumentoDto vista(Documento documento) {
        TipoConfigurado tipo=tipo(documento.getTipoDocumentoId());
        Object[] clasificacion=fila("select codigo,nombre from clasificacion_dato where id=:id",documento.getClasificacionId());
        String codigoCategoria=null,nombreCategoria=null;
        if(documento.getCategoriaSensibleId()!=null) {
            Object[] categoria=fila("select codigo,nombre from categoria_sensible where id=:id",documento.getCategoriaSensibleId());
            codigoCategoria=String.valueOf(categoria[0]);
            nombreCategoria=String.valueOf(categoria[1]);
        }
        return new DocumentoDto(
            documento.getId(),documento.getEntidadEmisoraId(),documento.getEntidadDestinatariaId(),
            tipo.codigo(),tipo.nombre(),tipo.maximoAccesos(),String.valueOf(clasificacion[0]),String.valueOf(clasificacion[1]),
            codigoCategoria,nombreCategoria,documento.getNombre(),documento.getDescripcion(),documento.getFechaDocumento(),
            documento.getEstado(),documento.getSustituyeADocumentoId(),documento.getCreadoPorCuentaId(),
            documento.getFechaCreacion(),documento.getFechaDisponibilidad(),accesos.countByDocumentoId(documento.getId())
        );
    }

    private DocumentoDtos.Acceso vistaAcceso(Long accesoId,Long cuentaId,java.time.LocalDateTime fecha) {
        List<?> filas=em.createNativeQuery("""
            select cu.nombre_usuario, e.nombre
              from cuenta_usuario cu
              left join entidad e on e.id=cu.persona_entidad_id
             where cu.id=:id
            """).setParameter("id",cuentaId).getResultList();
        if(filas.isEmpty()) return new DocumentoDtos.Acceso(accesoId,cuentaId,"Cuenta "+cuentaId,null,fecha);
        Object[] f=(Object[])filas.get(0);
        return new DocumentoDtos.Acceso(accesoId,cuentaId,String.valueOf(f[0]),f[1]==null?null:String.valueOf(f[1]),fecha);
    }

    private TipoConfigurado tipo(String codigo) {
        List<?> filas=em.createNativeQuery("""
            select td.id,td.codigo,td.nombre,td.clasificacion_predeterminada_id,cd.codigo,
                   td.categoria_sensible_predeterminada_id,td.maximo_accesos
              from tipo_documento td
              join clasificacion_dato cd on cd.id=td.clasificacion_predeterminada_id
             where td.codigo=:codigo and td.activo=true
            """).setParameter("codigo",codigo.trim().toUpperCase()).getResultList();
        if(filas.isEmpty()) throw new ExcepcionNoEncontrado("No existe el tipo de documento activo "+codigo);
        return tipoDesdeFila((Object[])filas.get(0));
    }

    private TipoConfigurado tipo(Long id) {
        List<?> filas=em.createNativeQuery("""
            select td.id,td.codigo,td.nombre,td.clasificacion_predeterminada_id,cd.codigo,
                   td.categoria_sensible_predeterminada_id,td.maximo_accesos
              from tipo_documento td
              join clasificacion_dato cd on cd.id=td.clasificacion_predeterminada_id
             where td.id=:id
            """).setParameter("id",id).getResultList();
        if(filas.isEmpty()) throw new ExcepcionNoEncontrado("No existe el tipo de documento "+id);
        return tipoDesdeFila((Object[])filas.get(0));
    }

    private TipoConfigurado tipoDesdeFila(Object[] f) {
        return new TipoConfigurado(numero(f[0]),String.valueOf(f[1]),String.valueOf(f[2]),numero(f[3]),String.valueOf(f[4]),
            f[5]==null?null:numero(f[5]),f[6]==null?null:((Number)f[6]).intValue());
    }

    private String codigoClasificacion(Long id) {
        return String.valueOf(fila("select codigo,nombre from clasificacion_dato where id=:id",id)[0]);
    }

    private Documento requerir(Long id) {
        return documentos.findById(id).orElseThrow(() -> new ExcepcionNoEncontrado("No existe el documento "+id));
    }
    private Documento requerirBloqueado(Long id) {
        return documentos.buscarPorIdParaActualizar(id).orElseThrow(() -> new ExcepcionNoEncontrado("No existe el documento "+id));
    }
    private void comprobarEntidad(Long id) {
        if(!entidades.existsById(id)) throw new ExcepcionNoEncontrado("No existe la entidad "+id);
    }
    private Object[] fila(String sql,Long id) {
        return (Object[])em.createNativeQuery(sql).setParameter("id",id).getSingleResult();
    }
    private Long numero(Object o){return ((Number)o).longValue();}
    private String normalizarEstado(String estado) {
        String e=estado.trim().toUpperCase();
        if(!ESTADOS.contains(e)) throw new IllegalArgumentException("Estado de documento no válido: "+estado);
        return e;
    }

    private record TipoConfigurado(Long id,String codigo,String nombre,Long clasificacionId,
                                   String codigoClasificacion,Long categoriaSensibleId,Integer maximoAccesos) {}
}

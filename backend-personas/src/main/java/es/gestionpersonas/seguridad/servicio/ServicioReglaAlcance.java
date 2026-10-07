package es.gestionpersonas.seguridad.servicio;

import es.gestionpersonas.comun.*;
import es.gestionpersonas.cuenta.modelo.ContextoCuenta;
import es.gestionpersonas.cuenta.repositorio.RepositorioContextoCuenta;
import es.gestionpersonas.seguridad.dto.ReglaAlcanceDtos;
import es.gestionpersonas.seguridad.modelo.ReglaAlcanceAcceso;
import es.gestionpersonas.seguridad.repositorio.RepositorioReglaAlcanceAcceso;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ServicioReglaAlcance {
    private final RepositorioReglaAlcanceAcceso reglas;
    private final RepositorioContextoCuenta contextos;
    private final ServicioControlAcceso controlAcceso;
    private final EntityManager em;

    public ServicioReglaAlcance(RepositorioReglaAlcanceAcceso reglas,
                                RepositorioContextoCuenta contextos,
                                ServicioControlAcceso controlAcceso,
                                EntityManager em) {
        this.reglas = reglas;
        this.contextos = contextos;
        this.controlAcceso = controlAcceso;
        this.em = em;
    }

    public List<ReglaAlcanceDtos.Regla> listar(Long cuentaId, Long contextoCuentaId) {
        ContextoCuenta contexto = contextoGestionable(cuentaId, contextoCuentaId);
        return reglas.findByContextoCuentaIdOrderByPrioridadDescIdAsc(contexto.getId())
            .stream().map(this::vista).toList();
    }

    @Transactional
    public ReglaAlcanceDtos.Regla crear(Long cuentaId, Long contextoCuentaId, ReglaAlcanceDtos.CrearRegla entrada) {
        ContextoCuenta contexto = contextoGestionable(cuentaId, contextoCuentaId);
        DatosRegla d = normalizar(entrada.tipoRegla(), entrada.codigoTipoRelacion(), entrada.codigoRolFuncional(),
            entrada.codigoTipoEntidadObjetivo(), entrada.incluirDescendientes(), entrada.efecto(),
            entrada.prioridad(), entrada.activo());
        ReglaAlcanceAcceso regla = reglas.save(new ReglaAlcanceAcceso(
            contexto.getId(), d.tipoRegla(), d.tipoRelacionId(), d.rolFuncionalId(), d.tipoEntidadId(),
            d.incluirDescendientes(), d.efecto(), d.prioridad(), d.activo()));
        return vista(regla);
    }

    @Transactional
    public ReglaAlcanceDtos.Regla reemplazar(Long cuentaId, Long contextoCuentaId, Long reglaId,
                                              ReglaAlcanceDtos.ReemplazarRegla entrada) {
        ContextoCuenta contexto = contextoGestionable(cuentaId, contextoCuentaId);
        ReglaAlcanceAcceso regla = reglas.findByIdAndContextoCuentaId(reglaId, contexto.getId())
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe la regla de alcance " + reglaId));
        DatosRegla d = normalizar(entrada.tipoRegla(), entrada.codigoTipoRelacion(), entrada.codigoRolFuncional(),
            entrada.codigoTipoEntidadObjetivo(), entrada.incluirDescendientes(), entrada.efecto(),
            entrada.prioridad(), entrada.activo());
        regla.actualizar(d.tipoRegla(), d.tipoRelacionId(), d.rolFuncionalId(), d.tipoEntidadId(),
            d.incluirDescendientes(), d.efecto(), d.prioridad(), d.activo());
        return vista(regla);
    }

    @Transactional
    public void eliminar(Long cuentaId, Long contextoCuentaId, Long reglaId) {
        ContextoCuenta contexto = contextoGestionable(cuentaId, contextoCuentaId);
        ReglaAlcanceAcceso regla = reglas.findByIdAndContextoCuentaId(reglaId, contexto.getId())
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe la regla de alcance " + reglaId));
        reglas.delete(regla);
    }

    private ContextoCuenta contextoGestionable(Long cuentaId, Long contextoCuentaId) {
        ContextoCuenta contexto = contextos.findByIdAndAccountId(contextoCuentaId, cuentaId)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe el contexto de cuenta " + contextoCuentaId));
        controlAcceso.requireEntityAccess(ContextoAccesoActual.require(), contexto.getContextEntityId());
        if (!"PERSONALIZADO".equalsIgnoreCase(contexto.getScope())) {
            throw new IllegalArgumentException("Las reglas de alcance solo aplican a contextos con alcance PERSONALIZADO");
        }
        return contexto;
    }

    private DatosRegla normalizar(String tipoRegla, String codigoRelacion, String codigoRol, String codigoTipoEntidad,
                                  Boolean incluirDescendientes, String efecto, Integer prioridad, Boolean activo) {
        String tipo = normalizarEnum(tipoRegla, Set.of("RELACION", "ROL_FUNCIONAL", "TIPO_ENTIDAD", "COMPUESTA"), "tipo de regla");
        String efectoNormalizado = normalizarEnum(efecto, Set.of("PERMITIR", "DENEGAR"), "efecto");
        Long relacionId = idPorCodigo("tipo_relacion", codigoRelacion);
        Long rolId = idPorCodigo("rol_funcional", codigoRol);
        Long tipoEntidadId = idPorCodigo("tipo_entidad", codigoTipoEntidad);
        int criterios = (relacionId == null ? 0 : 1) + (rolId == null ? 0 : 1) + (tipoEntidadId == null ? 0 : 1);
        if (criterios == 0) throw new IllegalArgumentException("La regla debe definir al menos un criterio");
        switch (tipo) {
            case "RELACION" -> { if (relacionId == null || criterios != 1) throw new IllegalArgumentException("RELACION requiere únicamente codigoTipoRelacion"); }
            case "ROL_FUNCIONAL" -> { if (rolId == null || criterios != 1) throw new IllegalArgumentException("ROL_FUNCIONAL requiere únicamente codigoRolFuncional"); }
            case "TIPO_ENTIDAD" -> { if (tipoEntidadId == null || criterios != 1) throw new IllegalArgumentException("TIPO_ENTIDAD requiere únicamente codigoTipoEntidadObjetivo"); }
            case "COMPUESTA" -> { if (criterios < 2) throw new IllegalArgumentException("COMPUESTA requiere al menos dos criterios"); }
            default -> throw new IllegalStateException();
        }
        boolean descendientes = Boolean.TRUE.equals(incluirDescendientes);
        if (relacionId == null && descendientes) throw new IllegalArgumentException("incluirDescendientes solo aplica a reglas con relación");
        return new DatosRegla(tipo, relacionId, rolId, tipoEntidadId, descendientes, efectoNormalizado,
            prioridad == null ? 0 : prioridad, activo == null || activo);
    }

    private Long idPorCodigo(String tabla, String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        String sql = "select id from " + tabla + " where codigo=:codigo";
        List<?> filas = em.createNativeQuery(sql).setParameter("codigo", codigo.trim().toUpperCase()).getResultList();
        if (filas.isEmpty()) throw new ExcepcionNoEncontrado("No existe " + tabla + " con código " + codigo);
        return ((Number) filas.get(0)).longValue();
    }

    private String normalizarEnum(String valor, Set<String> permitidos, String nombre) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(nombre + " obligatorio");
        String v = valor.trim().toUpperCase();
        if (!permitidos.contains(v)) throw new IllegalArgumentException(nombre + " no válido: " + valor);
        return v;
    }

    private ReglaAlcanceDtos.Regla vista(ReglaAlcanceAcceso r) {
        return new ReglaAlcanceDtos.Regla(r.getId(), r.getContextoCuentaId(), r.getTipoRegla(),
            r.getTipoRelacionId(), codigo("tipo_relacion", r.getTipoRelacionId()),
            r.getRolFuncionalId(), codigo("rol_funcional", r.getRolFuncionalId()),
            r.getTipoEntidadObjetivoId(), codigo("tipo_entidad", r.getTipoEntidadObjetivoId()),
            r.isIncluirDescendientes(), r.getEfecto(), r.getPrioridad(), r.isActivo(),
            r.getFechaCreacion(), r.getFechaModificacion());
    }

    private String codigo(String tabla, Long id) {
        if (id == null) return null;
        return String.valueOf(em.createNativeQuery("select codigo from " + tabla + " where id=:id")
            .setParameter("id", id).getSingleResult());
    }

    private record DatosRegla(String tipoRegla, Long tipoRelacionId, Long rolFuncionalId, Long tipoEntidadId,
                              boolean incluirDescendientes, String efecto, int prioridad, boolean activo) {}
}

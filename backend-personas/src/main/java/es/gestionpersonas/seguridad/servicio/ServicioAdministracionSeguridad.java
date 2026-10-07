package es.gestionpersonas.seguridad.servicio;

import es.gestionpersonas.seguridad.dto.AdministracionSeguridadDtos;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServicioAdministracionSeguridad {
    private final EntityManager em;

    public ServicioAdministracionSeguridad(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    public List<AdministracionSeguridadDtos.RolSeguridad> roles() {
        List<Object[]> rows = em.createNativeQuery("""
            select id, codigo, nombre
              from rol_seguridad
             order by nombre, codigo
            """).getResultList();
        return rows.stream().map(r -> new AdministracionSeguridadDtos.RolSeguridad(
            number(r[0]), String.valueOf(r[1]), String.valueOf(r[2])
        )).toList();
    }

    @SuppressWarnings("unchecked")
    public List<AdministracionSeguridadDtos.Permiso> permisos() {
        List<Object[]> rows = em.createNativeQuery("""
            select id, codigo, descripcion
              from permiso
             order by codigo
            """).getResultList();
        return rows.stream().map(r -> new AdministracionSeguridadDtos.Permiso(
            number(r[0]), String.valueOf(r[1]), r[2] == null ? null : String.valueOf(r[2])
        )).toList();
    }

    private Long number(Object value) {
        return ((Number) value).longValue();
    }
}

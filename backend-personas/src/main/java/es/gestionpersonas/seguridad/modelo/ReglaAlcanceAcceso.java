package es.gestionpersonas.seguridad.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "regla_alcance_acceso")
public class ReglaAlcanceAcceso extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contexto_cuenta_id", nullable = false)
    private Long contextoCuentaId;

    @Column(name = "tipo_regla", nullable = false, length = 30)
    private String tipoRegla;

    @Column(name = "tipo_relacion_id")
    private Long tipoRelacionId;

    @Column(name = "rol_funcional_id")
    private Long rolFuncionalId;

    @Column(name = "tipo_entidad_objetivo_id")
    private Long tipoEntidadObjetivoId;

    @Column(name = "incluir_descendientes", nullable = false)
    private boolean incluirDescendientes;

    @Column(name = "efecto", nullable = false, length = 10)
    private String efecto;

    @Column(name = "prioridad", nullable = false)
    private int prioridad;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    protected ReglaAlcanceAcceso() {}

    public ReglaAlcanceAcceso(Long contextoCuentaId, String tipoRegla, Long tipoRelacionId,
                              Long rolFuncionalId, Long tipoEntidadObjetivoId,
                              boolean incluirDescendientes, String efecto, int prioridad, boolean activo) {
        this.contextoCuentaId = contextoCuentaId;
        this.tipoRegla = tipoRegla;
        this.tipoRelacionId = tipoRelacionId;
        this.rolFuncionalId = rolFuncionalId;
        this.tipoEntidadObjetivoId = tipoEntidadObjetivoId;
        this.incluirDescendientes = incluirDescendientes;
        this.efecto = efecto;
        this.prioridad = prioridad;
        this.activo = activo;
        this.fechaCreacion = LocalDateTime.now().withNano(0);
        this.fechaModificacion = this.fechaCreacion;
    }

    public void actualizar(String tipoRegla, Long tipoRelacionId, Long rolFuncionalId,
                           Long tipoEntidadObjetivoId, boolean incluirDescendientes,
                           String efecto, int prioridad, boolean activo) {
        this.tipoRegla = tipoRegla;
        this.tipoRelacionId = tipoRelacionId;
        this.rolFuncionalId = rolFuncionalId;
        this.tipoEntidadObjetivoId = tipoEntidadObjetivoId;
        this.incluirDescendientes = incluirDescendientes;
        this.efecto = efecto;
        this.prioridad = prioridad;
        this.activo = activo;
        this.fechaModificacion = LocalDateTime.now().withNano(0);
    }

    public Long getId() { return id; }
    public Long getContextoCuentaId() { return contextoCuentaId; }
    public String getTipoRegla() { return tipoRegla; }
    public Long getTipoRelacionId() { return tipoRelacionId; }
    public Long getRolFuncionalId() { return rolFuncionalId; }
    public Long getTipoEntidadObjetivoId() { return tipoEntidadObjetivoId; }
    public boolean isIncluirDescendientes() { return incluirDescendientes; }
    public String getEfecto() { return efecto; }
    public int getPrioridad() { return prioridad; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
}

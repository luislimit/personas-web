package es.gestionpersonas.documento.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="documento")
public class Documento extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="tipo_documento_id",nullable=false) private Long tipoDocumentoId;
    @Column(name="entidad_emisora_id",nullable=false) private Long entidadEmisoraId;
    @Column(name="entidad_destinataria_id",nullable=false) private Long entidadDestinatariaId;
    @Column(name="clasificacion_id",nullable=false) private Long clasificacionId;
    @Column(name="categoria_sensible_id") private Long categoriaSensibleId;
    @Column(name="nombre",nullable=false,length=220) private String nombre;
    @Column(name="descripcion",length=500) private String descripcion;
    @Column(name="referencia_archivo",nullable=false,length=500) private String referenciaArchivo;
    @Column(name="fecha_documento") private LocalDate fechaDocumento;
    @Column(name="estado",nullable=false,length=30) private String estado;
    @Column(name="sustituye_a_documento_id") private Long sustituyeADocumentoId;
    @Column(name="creado_por_cuenta_id",nullable=false) private Long creadoPorCuentaId;
    @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
    @Column(name="fecha_modificacion",nullable=false) private LocalDateTime fechaModificacion;
    @Column(name="fecha_disponibilidad") private LocalDate fechaDisponibilidad;

    protected Documento() {}

    public Documento(Long tipoDocumentoId, Long entidadEmisoraId, Long entidadDestinatariaId,
                     Long clasificacionId, Long categoriaSensibleId, String nombre,
                     String descripcion, String referenciaArchivo, LocalDate fechaDocumento,
                     Long sustituyeADocumentoId, Long creadoPorCuentaId) {
        this.tipoDocumentoId=tipoDocumentoId;
        this.entidadEmisoraId=entidadEmisoraId;
        this.entidadDestinatariaId=entidadDestinatariaId;
        this.clasificacionId=clasificacionId;
        this.categoriaSensibleId=categoriaSensibleId;
        this.nombre=nombre;
        this.descripcion=descripcion;
        this.referenciaArchivo=referenciaArchivo;
        this.fechaDocumento=fechaDocumento;
        this.sustituyeADocumentoId=sustituyeADocumentoId;
        this.creadoPorCuentaId=creadoPorCuentaId;
        this.estado="BORRADOR";
        this.fechaCreacion=LocalDateTime.now().withNano(0);
        this.fechaModificacion=this.fechaCreacion;
    }

    public void modificar(Long tipoDocumentoId, Long clasificacionId, Long categoriaSensibleId,
                          Long entidadEmisoraId, Long entidadDestinatariaId,
                          String nombre, String descripcion, String referenciaArchivo,
                          LocalDate fechaDocumento) {
        if(tipoDocumentoId!=null) this.tipoDocumentoId=tipoDocumentoId;
        if(clasificacionId!=null) this.clasificacionId=clasificacionId;
        this.categoriaSensibleId=categoriaSensibleId;
        if(entidadEmisoraId!=null) this.entidadEmisoraId=entidadEmisoraId;
        if(entidadDestinatariaId!=null) this.entidadDestinatariaId=entidadDestinatariaId;
        if(nombre!=null) this.nombre=nombre;
        if(descripcion!=null) this.descripcion=descripcion;
        if(referenciaArchivo!=null) this.referenciaArchivo=referenciaArchivo;
        if(fechaDocumento!=null) this.fechaDocumento=fechaDocumento;
        this.fechaModificacion=LocalDateTime.now().withNano(0);
    }

    public void cambiarEstado(String nuevoEstado) {
        this.estado=nuevoEstado;
        if("DISPONIBLE".equals(nuevoEstado) && this.fechaDisponibilidad==null)
            this.fechaDisponibilidad=LocalDate.now();
        this.fechaModificacion=LocalDateTime.now().withNano(0);
    }

    public Long getId(){return id;}
    public Long getTipoDocumentoId(){return tipoDocumentoId;}
    public Long getEntidadEmisoraId(){return entidadEmisoraId;}
    public Long getEntidadDestinatariaId(){return entidadDestinatariaId;}
    public Long getClasificacionId(){return clasificacionId;}
    public Long getCategoriaSensibleId(){return categoriaSensibleId;}
    public String getNombre(){return nombre;}
    public String getDescripcion(){return descripcion;}
    public String getReferenciaArchivo(){return referenciaArchivo;}
    public LocalDate getFechaDocumento(){return fechaDocumento;}
    public String getEstado(){return estado;}
    public Long getSustituyeADocumentoId(){return sustituyeADocumentoId;}
    public Long getCreadoPorCuentaId(){return creadoPorCuentaId;}
    public LocalDateTime getFechaCreacion(){return fechaCreacion;}
    public LocalDateTime getFechaModificacion(){return fechaModificacion;}
    public LocalDate getFechaDisponibilidad(){return fechaDisponibilidad;}
}

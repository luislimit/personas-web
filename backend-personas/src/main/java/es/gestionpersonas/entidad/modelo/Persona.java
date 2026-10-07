package es.gestionpersonas.entidad.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="persona")
public class Persona extends EntidadAuditable {
    @Id @Column(name="entidad_id") private Long entityId;
    @OneToOne(fetch=FetchType.LAZY) @MapsId @JoinColumn(name="entidad_id") private Entidad entidad;
    @Column(name="nombre",nullable=false,length=100) private String firstName;
    @Column(name="primer_apellido",nullable=false,length=150) private String lastName;
    @Column(name="segundo_apellido",length=150) private String secondLastName;
    @Column(name="fecha_nacimiento") private LocalDate birthDate; @Column(name="genero",length=30) private String gender;
    protected Persona() {}
    public Persona(Entidad entidad,String firstName,String lastName,String secondLastName,LocalDate birthDate,String gender){this.entidad=entidad;this.firstName=firstName;this.lastName=lastName;this.secondLastName=secondLastName;this.birthDate=birthDate;this.gender=gender;}
    public void update(String firstName,String lastName,String secondLastName,LocalDate birthDate,String gender){if(firstName!=null)this.firstName=firstName;if(lastName!=null)this.lastName=lastName;this.secondLastName=secondLastName;this.birthDate=birthDate;this.gender=gender;}
    public Long getEntityId(){return entityId;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getSecondLastName(){return secondLastName;} public LocalDate getBirthDate(){return birthDate;} public String getGender(){return gender;}
}

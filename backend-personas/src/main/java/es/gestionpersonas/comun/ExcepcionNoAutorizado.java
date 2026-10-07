package es.gestionpersonas.comun;

public class ExcepcionNoAutorizado extends RuntimeException {
    public ExcepcionNoAutorizado(String message) { super(message); }
}

package es.gestionpersonas.seguridad.web;

/**
 * @deprecated Desde v2.6 la seguridad HTTP se gestiona mediante Spring Security
 * y {@link FiltroAutenticacionBearer}. Se mantiene este tipo temporalmente para
 * que una actualización incremental no deje una clase antigua activa en el
 * árbol de fuentes.
 */
@Deprecated(forRemoval = true)
public final class InterceptorSeguridadApi {
    private InterceptorSeguridadApi() {}
}

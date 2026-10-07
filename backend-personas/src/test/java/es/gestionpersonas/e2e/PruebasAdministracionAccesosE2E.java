package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasAdministracionAccesosE2E {
    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");

    private static RestClient api;
    private static Sesion admin;
    private static Contexto residencia;
    private static Long personaId;
    private static Long cuentaId;
    private static Long contextoCuentaId;
    private static String usuario;
    private static Sesion usuarioSesion;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        admin = login("admin.residencia", "demo");
        residencia = contexto(admin, "Residencia Los Olivos");
        usuario = "e2e.admin." + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test @Order(1)
    @DisplayName("01 - Los catálogos administrativos exponen roles y permisos")
    void catalogos() {
        List<Map<String,Object>> roles = getListaMap("/api/seguridad/catalogos/roles", admin.token(), residencia.id());
        List<Map<String,Object>> permisos = getListaMap("/api/seguridad/catalogos/permisos", admin.token(), residencia.id());
        assertTrue(roles.stream().anyMatch(r -> "GESTOR_CONTEXTO".equals(r.get("codigo"))));
        assertTrue(permisos.stream().anyMatch(p -> "ENTIDAD.LEER".equals(p.get("codigo"))));
    }

    @Test @Order(2)
    @DisplayName("02 - El administrador crea una persona y su cuenta")
    void crearPersonaCuenta() {
        String nombre = "E2E Administración " + usuario;
        Map<String,Object> persona = postMapa(
            "/api/entidades", admin.token(), residencia.id(),
            Map.of(
                "codigoTipo", "PERSONA",
                "nombre", nombre,
                "persona", Map.of("nombre", "E2E", "primerApellido", "Administración")
            )
        );
        personaId = numero(persona.get("id"));

        Map<String,Object> cuenta = postMapa(
            "/api/cuentas", admin.token(), residencia.id(),
            Map.of(
                "personaEntidadId", personaId,
                "nombreUsuario", usuario,
                "correoAcceso", usuario + "@example.test",
                "contrasena", "demo1234",
                "proveedorAutenticacion", "LOCAL",
                "estado", "ACTIVO"
            )
        );
        cuentaId = numero(cuenta.get("id"));
        assertEquals(usuario, cuenta.get("nombreUsuario"));

        List<Map<String,Object>> cuentas = getListaMap(
            "/api/cuentas?texto=" + usuario, admin.token(), residencia.id()
        );
        assertTrue(cuentas.stream().anyMatch(c -> Objects.equals(cuentaId, numero(c.get("id")))));
    }

    @Test @Order(3)
    @DisplayName("03 - Se asigna un contexto con rol y alcance")
    void asignarContexto() {
        Map<String,Object> ctx = postMapa(
            "/api/cuentas/" + cuentaId + "/contextos", admin.token(), residencia.id(),
            Map.of(
                "entidadContextoId", residencia.id(),
                "codigoRol", "GESTOR_CONTEXTO",
                "alcance", "CONTEXTO",
                "habilitado", true
            )
        );
        contextoCuentaId = numero(ctx.get("id"));
        assertEquals("GESTOR_CONTEXTO", ctx.get("codigoRol"));
        assertEquals("CONTEXTO", ctx.get("alcance"));

        usuarioSesion = login(usuario, "demo1234");
        assertNotNull(contexto(usuarioSesion, "Residencia Los Olivos"));
    }

    @Test @Order(4)
    @DisplayName("04 - El diagnóstico muestra permisos efectivos y su procedencia")
    void accesoEfectivo() {
        Map<String,Object> acceso = getMapa(
            "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId + "/acceso-efectivo",
            admin.token(), residencia.id()
        );
        assertEquals("GESTOR_CONTEXTO", acceso.get("codigoRol"));
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> permisos = (List<Map<String,Object>>) acceso.get("permisos");
        Map<String,Object> entidadLeer = permiso(permisos, "ENTIDAD.LEER");
        Map<String,Object> cuentaLeer = permiso(permisos, "CUENTA.LEER");
        assertEquals(Boolean.TRUE, entidadLeer.get("concedidoPorRol"));
        assertEquals(Boolean.TRUE, entidadLeer.get("efectivo"));
        assertEquals(Boolean.FALSE, cuentaLeer.get("efectivo"));
    }

    @Test @Order(5)
    @DisplayName("05 - Una excepción DENEGAR se aplica inmediatamente a una sesión ya abierta")
    void denegarPermiso() {
        putMapa(
            "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId + "/excepciones-permiso/ENTIDAD.LEER",
            admin.token(), residencia.id(), Map.of("efecto", "DENEGAR")
        );

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getLista("/api/entidades", usuarioSesion.token(), residencia.id())
        );
        assertEquals(403, ex.getStatusCode().value());

        Map<String,Object> explicacion = getMapa(
            "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId +
                "/permisos/ENTIDAD.LEER/explicacion",
            admin.token(), residencia.id()
        );
        assertEquals(Boolean.FALSE, explicacion.get("efectivo"));
        assertEquals("DENEGAR", explicacion.get("excepcion"));
    }

    @Test @Order(6)
    @DisplayName("06 - Al retirar la excepción vuelve a aplicarse el permiso del rol")
    void restaurarPermiso() {
        delete(
            "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId +
                "/excepciones-permiso/ENTIDAD.LEER",
            admin.token(), residencia.id()
        );
        assertDoesNotThrow(() -> getLista("/api/entidades", usuarioSesion.token(), residencia.id()));
    }

    @Test @Order(7)
    @DisplayName("07 - Un admin de organización no puede conceder permisos que él mismo no posee")
    void evitaEscaladaPermiso() {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> putMapa(
                "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId +
                    "/excepciones-permiso/INFORMACION_SENSIBLE.LEER",
                admin.token(), residencia.id(), Map.of("efecto", "PERMITIR")
            )
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test @Order(8)
    @DisplayName("08 - Un admin de organización no puede asignar ADMIN_PLATAFORMA")
    void evitaEscaladaRol() {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> patchMapa(
                "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId,
                admin.token(), residencia.id(), Map.of("codigoRol", "ADMIN_PLATAFORMA")
            )
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test @Order(9)
    @DisplayName("09 - El rol y el alcance pueden modificarse administrativamente")
    void modificarRol() {
        Map<String,Object> ctx = patchMapa(
            "/api/cuentas/" + cuentaId + "/contextos/" + contextoCuentaId,
            admin.token(), residencia.id(),
            Map.of("codigoRol", "AUTOSERVICIO", "alcance", "PROPIO")
        );
        assertEquals("AUTOSERVICIO", ctx.get("codigoRol"));
        assertEquals("PROPIO", ctx.get("alcance"));
    }

    @Test @Order(10)
    @DisplayName("10 - Se pueden consultar las sesiones y bloquear la cuenta las invalida inmediatamente")
    void bloqueoRevoca() {
        List<Map<String,Object>> sesiones = getListaMap(
            "/api/cuentas/" + cuentaId + "/sesiones", admin.token(), residencia.id()
        );
        assertTrue(sesiones.stream().anyMatch(s -> Boolean.TRUE.equals(s.get("activa"))));

        Map<String,Object> bloqueada = postMapaSinCuerpo(
            "/api/cuentas/" + cuentaId + "/bloquear", admin.token(), residencia.id()
        );
        assertEquals("BLOQUEADA", bloqueada.get("estado"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getMapa("/api/autenticacion/yo", usuarioSesion.token(), null)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test @Order(11)
    @DisplayName("11 - Reactivar permite nuevo login y la revocación administrativa vuelve a invalidarlo")
    void reactivarYRevocar() {
        Map<String,Object> activa = postMapaSinCuerpo(
            "/api/cuentas/" + cuentaId + "/activar", admin.token(), residencia.id()
        );
        assertEquals("ACTIVO", activa.get("estado"));

        usuarioSesion = login(usuario, "demo1234");
        postSinCuerpo(
            "/api/cuentas/" + cuentaId + "/sesiones/revocar-todas",
            admin.token(), residencia.id()
        );
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getMapa("/api/autenticacion/yo", usuarioSesion.token(), null)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test @Order(12)
    @DisplayName("12 - La cuenta puede desvincularse y volver a vincularse a su persona")
    void vinculoPersona() {
        Map<String,Object> sinPersona = deleteMapa(
            "/api/cuentas/" + cuentaId + "/persona", admin.token(), residencia.id()
        );
        assertNull(sinPersona.get("personaEntidadId"));

        Map<String,Object> vinculada = putSinCuerpoMapa(
            "/api/cuentas/" + cuentaId + "/persona/" + personaId,
            admin.token(), residencia.id()
        );
        assertEquals(personaId, numero(vinculada.get("personaEntidadId")));
    }

    private static Map<String,Object> permiso(List<Map<String,Object>> permisos, String codigo) {
        return permisos.stream().filter(p -> codigo.equals(p.get("codigo"))).findFirst()
            .orElseThrow(() -> new AssertionError("No aparece permiso " + codigo));
    }

    private static Sesion login(String usuario, String password) {
        @SuppressWarnings("unchecked")
        Map<String,Object> body = api.post().uri("/api/autenticacion/iniciar-sesion")
            .body(Map.of("nombreUsuario", usuario, "contrasena", password))
            .retrieve().body(Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> raw = (List<Map<String,Object>>) body.get("contextos");
        List<Contexto> contextos = raw.stream()
            .map(c -> new Contexto(numero(c.get("entidadContextoId")), String.valueOf(c.get("nombreContexto"))))
            .toList();
        return new Sesion(String.valueOf(body.get("tokenAcceso")), numero(body.get("cuentaId")), contextos);
    }

    private static Contexto contexto(Sesion sesion, String nombre) {
        return sesion.contextos().stream().filter(c -> nombre.equals(c.nombre())).findFirst()
            .orElseThrow(() -> new AssertionError("No existe contexto " + nombre));
    }

    private static Map<String,Object> getMapa(String ruta, String token, Long ctx) {
        var s=api.get().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.retrieve().body(Map.class);
    }

    private static List<?> getLista(String ruta, String token, Long ctx) {
        var s=api.get().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.retrieve().body(List.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> getListaMap(String ruta, String token, Long ctx) {
        return (List<Map<String,Object>>)(List<?>)getLista(ruta,token,ctx);
    }

    private static Map<String,Object> postMapa(String ruta, String token, Long ctx, Object body) {
        var s=api.post().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.body(body).retrieve().body(Map.class);
    }

    private static Map<String,Object> postMapaSinCuerpo(String ruta, String token, Long ctx) {
        var s=api.post().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.retrieve().body(Map.class);
    }

    private static void postSinCuerpo(String ruta, String token, Long ctx) {
        var s=api.post().uri(ruta); s.headers(h -> headers(h,token,ctx));
        s.retrieve().toBodilessEntity();
    }

    private static Map<String,Object> putMapa(String ruta, String token, Long ctx, Object body) {
        var s=api.put().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.body(body).retrieve().body(Map.class);
    }

    private static Map<String,Object> putSinCuerpoMapa(String ruta, String token, Long ctx) {
        var s=api.put().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.retrieve().body(Map.class);
    }

    private static Map<String,Object> patchMapa(String ruta, String token, Long ctx, Object body) {
        var s=api.patch().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.body(body).retrieve().body(Map.class);
    }

    private static void delete(String ruta, String token, Long ctx) {
        var s=api.delete().uri(ruta); s.headers(h -> headers(h,token,ctx));
        s.retrieve().toBodilessEntity();
    }

    private static Map<String,Object> deleteMapa(String ruta, String token, Long ctx) {
        var s=api.delete().uri(ruta); s.headers(h -> headers(h,token,ctx));
        return s.retrieve().body(Map.class);
    }

    private static void headers(HttpHeaders h, String token, Long ctx) {
        if(token!=null) h.setBearerAuth(token);
        if(ctx!=null) h.set("X-Contexto-Id",ctx.toString());
    }

    private static Long numero(Object value) {
        if(value==null) return null;
        return value instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(value));
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long id, String nombre) {}
}

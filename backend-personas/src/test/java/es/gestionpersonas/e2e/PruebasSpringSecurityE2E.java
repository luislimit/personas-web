package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasSpringSecurityE2E {
    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");

    private static RestClient api;
    private static Sesion carlos;
    private static Sesion admin;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Test
    @Order(1)
    @DisplayName("01 - Endpoint público no requiere autenticación")
    void publico() {
        Map<String,Object> salud = get("/api/sistema/salud", null, null);
        assertEquals("ACTIVO", salud.get("estado"));
    }

    @Test
    @Order(2)
    @DisplayName("02 - Endpoint protegido sin Bearer devuelve 401")
    void sinToken() {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> get("/api/entidades", null, null)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @Order(3)
    @DisplayName("03 - Spring Security autentica el token opaco")
    void login() {
        carlos = login("carlos", "demo");
        assertNotNull(carlos.token());
        assertFalse(carlos.contextos().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("04 - Un endpoint de negocio exige X-Contexto-Id")
    void contextoObligatorio() {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> get("/api/entidades", carlos.token(), null)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @Order(5)
    @DisplayName("05 - GrantedAuthority permite ENTIDAD.LEER")
    void autoridadPermitida() {
        Contexto infantil = contexto(carlos, "CB Tres Cantos");
        List<?> entidades = getLista("/api/entidades?nombre=Diego", carlos.token(), infantil.id());
        assertFalse(entidades.isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("06 - @PreAuthorize deniega CUENTA.LEER cuando falta el permiso")
    void autoridadDenegada() {
        Contexto infantil = contexto(carlos, "CB Tres Cantos");
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> get("/api/cuentas/" + carlos.cuentaId(), carlos.token(), infantil.id())
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @Order(7)
    @DisplayName("07 - El mismo endpoint funciona para un rol que sí concede CUENTA.LEER")
    void autoridadAdmin() {
        admin = login("admin.residencia", "demo");
        Contexto residencia = contexto(admin, "Residencia Los Olivos");
        Map<String,Object> cuenta = get(
            "/api/cuentas/" + admin.cuentaId(), admin.token(), residencia.id()
        );
        assertEquals(admin.cuentaId(), numeroLong(cuenta.get("id")));
    }

    @Test
    @Order(8)
    @DisplayName("08 - Cerrar sesión revoca el token autenticado")
    void logout() {
        postSinCuerpo("/api/autenticacion/cerrar-sesion", carlos.token(), null);
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> get("/api/autenticacion/yo", carlos.token(), null)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    private static Sesion login(String usuario, String password) {
        @SuppressWarnings("unchecked")
        Map<String,Object> body = api.post()
            .uri("/api/autenticacion/iniciar-sesion")
            .body(Map.of("nombreUsuario", usuario, "contrasena", password))
            .retrieve().body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> raw = (List<Map<String,Object>>) body.get("contextos");
        List<Contexto> contextos = raw.stream()
            .map(c -> new Contexto(
                numeroLong(c.get("entidadContextoId")),
                String.valueOf(c.get("nombreContexto"))
            ))
            .toList();

        return new Sesion(
            String.valueOf(body.get("tokenAcceso")),
            numeroLong(body.get("cuentaId")),
            contextos
        );
    }

    private static Contexto contexto(Sesion sesion, String nombre) {
        return sesion.contextos().stream()
            .filter(c -> nombre.equals(c.nombre()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No existe contexto " + nombre));
    }

    private static Map<String,Object> get(String ruta, String token, Long contextoId) {
        var spec = api.get().uri(ruta);
        spec.headers(h -> headers(h, token, contextoId));
        @SuppressWarnings("unchecked")
        Map<String,Object> body = spec.retrieve().body(Map.class);
        return body;
    }

    private static List<?> getLista(String ruta, String token, Long contextoId) {
        var spec = api.get().uri(ruta);
        spec.headers(h -> headers(h, token, contextoId));
        return spec.retrieve().body(List.class);
    }

    private static void postSinCuerpo(String ruta, String token, Long contextoId) {
        var spec = api.post().uri(ruta);
        spec.headers(h -> headers(h, token, contextoId));
        spec.retrieve().toBodilessEntity();
    }

    private static void headers(HttpHeaders h, String token, Long contextoId) {
        if (token != null) h.setBearerAuth(token);
        if (contextoId != null) h.set("X-Contexto-Id", contextoId.toString());
    }

    private static Long numeroLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        return v == null ? null : Long.valueOf(v.toString());
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long id, String nombre) {}
}

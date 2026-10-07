package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasReglasAlcanceE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");

    private static RestClient api;
    private static Sesion admin;
    private static Sesion personalizado;
    private static Contexto residenciaAdmin;
    private static Contexto residenciaPersonalizada;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        admin = login("admin.residencia", "demo");
        personalizado = login("reglas.demo", "demo");
        residenciaAdmin = contexto(admin, "Residencia Los Olivos");
        residenciaPersonalizada = contexto(personalizado, "Residencia Los Olivos");
        limpiarReglas();
    }

    @AfterAll
    static void limpiar() {
        limpiarReglas();
    }

    @BeforeEach
    void prepararCaso() {
        limpiarReglas();
    }

    @Test
    @Order(1)
    @DisplayName("PERSONALIZADO sin reglas solo permite contexto y persona propia")
    void personalizadoSinReglas() {
        assertTrue(buscar(personalizado, residenciaPersonalizada, "Elena Martín").stream()
            .anyMatch(e -> "Elena Martín".equals(e.get("nombre"))));

        assertTrue(buscar(personalizado, residenciaPersonalizada, "Residencia Los Olivos").stream()
            .anyMatch(e -> "Residencia Los Olivos".equals(e.get("nombre"))));

        assertTrue(buscar(personalizado, residenciaPersonalizada, "Ana López").isEmpty(),
            "Ana no debe ser visible sin una regla personalizada");
    }

    @Test
    @Order(2)
    @DisplayName("Regla por rol funcional permite únicamente residentes")
    void reglaRolFuncional() {
        crearRegla(Map.of(
            "tipoRegla", "ROL_FUNCIONAL",
            "codigoRolFuncional", "RESIDENTE",
            "efecto", "PERMITIR",
            "prioridad", 100,
            "activo", true
        ));

        assertVisible("Ana López", true);
        assertVisible("Carmen Martín", true);
        assertVisible("José García", true);
        assertVisible("Marta García", false);
        assertVisible("Pedro Ruiz", false);
    }

    @Test
    @Order(3)
    @DisplayName("DENEGAR prevalece sobre PERMITIR cuando ambas reglas coinciden")
    void denegarPrevalece() {
        crearRegla(Map.of(
            "tipoRegla", "ROL_FUNCIONAL",
            "codigoRolFuncional", "RESIDENTE",
            "efecto", "PERMITIR",
            "prioridad", 10,
            "activo", true
        ));

        assertVisible("Ana López", true);

        Long denyId = numeroLong(crearRegla(Map.of(
            "tipoRegla", "COMPUESTA",
            "codigoRolFuncional", "RESIDENTE",
            "codigoTipoEntidadObjetivo", "PERSONA",
            "efecto", "DENEGAR",
            "prioridad", 999,
            "activo", true
        )).get("id"));

        assertVisible("Ana López", false);

        eliminarRegla(denyId);
        assertVisible("Ana López", true);
    }

    @Test
    @Order(4)
    @DisplayName("Regla de relación directa PROPIEDAD limita el acceso a propiedades directas")
    void reglaRelacionDirecta() {
        crearRegla(Map.of(
            "tipoRegla", "RELACION",
            "codigoTipoRelacion", "PROPIEDAD",
            "incluirDescendientes", false,
            "efecto", "PERMITIR",
            "prioridad", 20,
            "activo", true
        ));

        assertVisible("Sala de actividades", true);
        assertVisible("Consulta médica", true);
        assertVisible("Servicio médico", false);
    }

    @Test
    @Order(5)
    @DisplayName("Regla TIPO_ENTIDAD no escapa del árbol del contexto")
    void tipoEntidadNoEscapaContexto() {
        crearRegla(Map.of(
            "tipoRegla", "TIPO_ENTIDAD",
            "codigoTipoEntidadObjetivo", "INSTALACION",
            "efecto", "PERMITIR",
            "prioridad", 1,
            "activo", true
        ));

        assertVisible("Sala de actividades", true);
        assertVisible("Consulta médica", true);
        assertVisible("Polideportivo Municipal", false);
    }

    @Test
    @Order(6)
    @DisplayName("Una regla inactiva no concede acceso")
    void reglaInactiva() {
        crearRegla(Map.of(
            "tipoRegla", "ROL_FUNCIONAL",
            "codigoRolFuncional", "RESIDENTE",
            "efecto", "PERMITIR",
            "prioridad", 100,
            "activo", false
        ));

        assertVisible("Ana López", false);
    }

    @Test
    @Order(7)
    @DisplayName("CRUD de reglas devuelve criterios normalizados y auditables")
    void crudReglas() {
        Map<String, Object> creada = crearRegla(Map.of(
            "tipoRegla", "ROL_FUNCIONAL",
            "codigoRolFuncional", "RESIDENTE",
            "efecto", "PERMITIR",
            "prioridad", 5,
            "activo", true
        ));
        Long id = numeroLong(creada.get("id"));
        assertEquals("RESIDENTE", creada.get("codigoRolFuncional"));
        assertEquals("PERMITIR", creada.get("efecto"));

        Map<String, Object> reemplazada = putMap(rutaReglas() + "/" + id, Map.of(
            "tipoRegla", "TIPO_ENTIDAD",
            "codigoTipoEntidadObjetivo", "INSTALACION",
            "efecto", "DENEGAR",
            "prioridad", 50,
            "activo", true
        ));
        assertEquals("TIPO_ENTIDAD", reemplazada.get("tipoRegla"));
        assertEquals("INSTALACION", reemplazada.get("codigoTipoEntidadObjetivo"));
        assertEquals("DENEGAR", reemplazada.get("efecto"));

        List<Map<String, Object>> reglas = getLista(rutaReglas(), admin, residenciaAdmin);
        assertTrue(reglas.stream().anyMatch(r -> Objects.equals(numeroLong(r.get("id")), id)));

        eliminarRegla(id);
        assertFalse(getLista(rutaReglas(), admin, residenciaAdmin).stream()
            .anyMatch(r -> Objects.equals(numeroLong(r.get("id")), id)));
    }

    private static void assertVisible(String nombre, boolean esperado) {
        boolean visible = buscar(personalizado, residenciaPersonalizada, nombre).stream()
            .anyMatch(e -> nombre.equals(e.get("nombre")));
        assertEquals(esperado, visible, "Visibilidad inesperada de " + nombre);
    }

    private static List<Map<String, Object>> buscar(Sesion sesion, Contexto contexto, String nombre) {
        Object[] raw = api.get()
            .uri(builder -> builder.path("/api/entidades").queryParam("nombre", nombre).build())
            .headers(h -> cabeceras(h, sesion, contexto))
            .retrieve().body(Object[].class);
        return convertirLista(raw);
    }

    private static Map<String, Object> crearRegla(Map<String, Object> body) {
        return postMap(rutaReglas(), body);
    }

    private static void eliminarRegla(Long id) {
        api.delete().uri(rutaReglas() + "/" + id)
            .headers(h -> cabeceras(h, admin, residenciaAdmin))
            .retrieve().toBodilessEntity();
    }

    private static void limpiarReglas() {
        if (api == null || admin == null || personalizado == null) return;
        for (Map<String, Object> r : getLista(rutaReglas(), admin, residenciaAdmin)) {
            eliminarRegla(numeroLong(r.get("id")));
        }
    }

    private static String rutaReglas() {
        return "/api/cuentas/" + personalizado.cuentaId()
            + "/contextos/" + residenciaPersonalizada.contextoCuentaId()
            + "/reglas-alcance";
    }

    private static Map<String, Object> postMap(String ruta, Object body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = api.post().uri(ruta)
            .headers(h -> cabeceras(h, admin, residenciaAdmin))
            .body(body).retrieve().body(Map.class);
        return respuesta;
    }

    private static Map<String, Object> putMap(String ruta, Object body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = api.put().uri(ruta)
            .headers(h -> cabeceras(h, admin, residenciaAdmin))
            .body(body).retrieve().body(Map.class);
        return respuesta;
    }

    private static List<Map<String, Object>> getLista(String ruta, Sesion sesion, Contexto contexto) {
        Object[] raw = api.get().uri(ruta)
            .headers(h -> cabeceras(h, sesion, contexto))
            .retrieve().body(Object[].class);
        return convertirLista(raw);
    }

    private static List<Map<String, Object>> convertirLista(Object[] raw) {
        if (raw == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m) {
                Map<String, Object> x = new LinkedHashMap<>();
                m.forEach((k, v) -> x.put(String.valueOf(k), v));
                out.add(x);
            }
        }
        return out;
    }

    private static void cabeceras(HttpHeaders h, Sesion sesion, Contexto contexto) {
        h.setBearerAuth(sesion.token());
        h.set("X-Contexto-Id", contexto.entidadContextoId().toString());
    }

    private static Sesion login(String usuario, String clave) {
        @SuppressWarnings("unchecked")
        Map<String, Object> r = api.post().uri("/api/autenticacion/iniciar-sesion")
            .body(Map.of("nombreUsuario", usuario, "contrasena", clave))
            .retrieve().body(Map.class);

        List<Contexto> contextos = new ArrayList<>();
        Object rawContexts = r.get("contextos");
        if (rawContexts instanceof List<?> lista) {
            for (Object o : lista) {
                if (!(o instanceof Map<?, ?> m)) continue;
                contextos.add(new Contexto(
                    numeroLong(m.get("contextoCuentaId")),
                    numeroLong(m.get("entidadContextoId")),
                    String.valueOf(m.get("nombreContexto")),
                    String.valueOf(m.get("alcance"))
                ));
            }
        }
        return new Sesion(String.valueOf(r.get("tokenAcceso")), numeroLong(r.get("cuentaId")), contextos);
    }

    private static Contexto contexto(Sesion s, String nombre) {
        return s.contextos().stream().filter(c -> nombre.equals(c.nombre()))
            .findFirst().orElseThrow(() -> new AssertionError("No existe contexto " + nombre));
    }

    private static Long numeroLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        return v == null ? null : Long.valueOf(v.toString());
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long contextoCuentaId, Long entidadContextoId, String nombre, String alcance) {}
}

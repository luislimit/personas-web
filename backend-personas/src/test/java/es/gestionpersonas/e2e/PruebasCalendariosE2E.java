package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas funcionales E2E de calendarios, eventos y recurrencias.
 *
 * Requisitos:
 * - backend arrancado;
 * - BD demo v2.2;
 * - aislamiento transversal aplicado.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasCalendariosE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;
    private static Sesion admin;
    private static Contexto residencia;
    private static Long residenciaId;
    private static Long salaActividadesId;
    private static final String ID_EJECUCION =
        UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        admin = iniciarSesion("admin.residencia", "demo");
        residencia = contextoPorNombre(admin, "Residencia Los Olivos");
        residenciaId = residencia.entidadContextoId();
        salaActividadesId = buscarEntidadExacta("Sala de actividades");
    }

    @Test
    @Order(1)
    @DisplayName("01 - Calendario: crear, consultar, modificar y desactivar")
    void cicloCalendario() {
        String marca = sufijo();

        Map<String, Object> creado = postMap(
            "/api/calendarios",
            Map.of(
                "entidadPropietariaId", residenciaId,
                "nombre", "E2E calendario " + marca,
                "descripcion", "Calendario funcional E2E",
                "colorPredeterminado", "#123456",
                "zonaHoraria", "Europe/Madrid",
                "activo", true
            )
        );

        Long id = numeroLong(creado.get("id"));
        assertEquals(residenciaId, numeroLong(creado.get("entidadPropietariaId")));
        assertEquals(Boolean.TRUE, creado.get("activo"));

        Map<String, Object> obtenido = getMap("/api/calendarios/" + id);
        assertEquals("E2E calendario " + marca, obtenido.get("nombre"));

        Map<String, Object> modificado = putMap(
            "/api/calendarios/" + id,
            Map.of(
                "entidadPropietariaId", residenciaId,
                "nombre", "E2E calendario modificado " + marca,
                "descripcion", "Descripción actualizada",
                "colorPredeterminado", "#654321",
                "zonaHoraria", "Europe/Madrid",
                "activo", true
            )
        );

        assertEquals("E2E calendario modificado " + marca, modificado.get("nombre"));

        assertEquals(204, delete("/api/calendarios/" + id));

        Map<String, Object> desactivado = getMap("/api/calendarios/" + id);
        assertEquals(Boolean.FALSE, desactivado.get("activo"));
    }

    @Test
    @Order(2)
    @DisplayName("02 - Evento simple: crear, consultar, modificar y cancelar")
    void cicloEventoSimple() {
        Long calendarioId = crearCalendarioTemporal("evento-simple");

        Map<String, Object> creado = postMap(
            "/api/calendarios/" + calendarioId + "/eventos",
            Map.of(
                "titulo", "E2E evento simple",
                "descripcion", "Evento sin recurrencia",
                "fechaInicio", "2027-01-10T10:00:00",
                "fechaFin", "2027-01-10T11:00:00",
                "diaCompleto", false,
                "zonaHoraria", "Europe/Madrid",
                "entidadUbicacionId", salaActividadesId,
                "estado", "PROGRAMADO"
            )
        );

        Long eventoId = numeroLong(creado.get("id"));
        assertEquals(Boolean.FALSE, creado.get("recurrente"));
        assertEquals("PROGRAMADO", creado.get("estado"));

        Map<String, Object> consultado = getMap("/api/eventos/" + eventoId);
        assertEquals("E2E evento simple", consultado.get("titulo"));

        Map<String, Object> modificado = patchMap(
            "/api/eventos/" + eventoId,
            Map.of(
                "titulo", "E2E evento simple modificado",
                "fechaInicio", "2027-01-10T10:30:00",
                "fechaFin", "2027-01-10T11:30:00"
            )
        );

        assertEquals("E2E evento simple modificado", modificado.get("titulo"));
        assertTrue(String.valueOf(modificado.get("fechaInicio")).startsWith("2027-01-10T10:30"));

        assertEquals(204, delete("/api/eventos/" + eventoId));

        Map<String, Object> cancelado = getMap("/api/eventos/" + eventoId);
        assertEquals("CANCELADO", cancelado.get("estado"));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Recurrencia semanal: se expanden las ocurrencias esperadas")
    void recurrenciaSemanal() {
        Long calendarioId = crearCalendarioTemporal("recurrencia-semanal");

        Map<String, Object> creado = crearSerieSemanal(
            calendarioId,
            "E2E serie semanal",
            5
        );

        assertEquals(Boolean.TRUE, creado.get("recurrente"));
        assertNotNull(creado.get("recurrencia"));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId
                + "/eventos?desde=2027-01-01T00:00:00&hasta=2027-02-15T23:59:59"
        );

        List<Map<String, Object>> serie = eventos.stream()
            .filter(e -> "E2E serie semanal".equals(e.get("titulo")))
            .sorted(Comparator.comparing(e -> normalizaFecha(e.get("fechaInicio"))))
            .toList();

        assertEquals(5, serie.size());
        assertEquals("2027-01-04T18:00", normalizaFecha(serie.get(0).get("fechaInicio")));
        assertEquals("2027-01-11T18:00", normalizaFecha(serie.get(1).get("fechaInicio")));
    }

    @Test
    @Order(4)
    @DisplayName("04 - Recurrencia: modificar SOLO_ESTE crea excepción sin cambiar las demás")
    void modificarSoloEstaOcurrencia() {
        Long calendarioId = crearCalendarioTemporal("solo-este");
        Map<String, Object> creado = crearSerieSemanal(calendarioId, "E2E serie excepción", 5);
        Long serieId = numeroLong(creado.get("id"));

        Map<String, Object> modificada = patchMap(
            "/api/eventos/" + serieId
                + "?fechaInicioOcurrencia=2027-01-11T18:00:00&alcance=SOLO_ESTE",
            Map.of(
                "titulo", "E2E ocurrencia modificada",
                "fechaInicio", "2027-01-11T19:00:00",
                "fechaFin", "2027-01-11T20:00:00"
            )
        );

        assertEquals("E2E ocurrencia modificada", modificada.get("titulo"));
        assertEquals(Boolean.TRUE, modificada.get("ocurrenciaModificada"));
        assertEquals("2027-01-11T18:00", normalizaFecha(modificada.get("fechaInicioOriginal")));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId
                + "/eventos?desde=2027-01-01T00:00:00&hasta=2027-02-15T23:59:59"
        );

        assertEquals(5, eventos.size());

        long modificadas = eventos.stream()
            .filter(e -> Boolean.TRUE.equals(e.get("ocurrenciaModificada")))
            .count();

        assertEquals(1, modificadas);
        assertTrue(eventos.stream().anyMatch(e ->
            "E2E ocurrencia modificada".equals(e.get("titulo"))
        ));
        assertTrue(eventos.stream().anyMatch(e ->
            "E2E serie excepción".equals(e.get("titulo"))
                && "2027-01-18T18:00".equals(normalizaFecha(e.get("fechaInicio")))
        ));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Recurrencia: eliminar SOLO_ESTE cancela solo una ocurrencia")
    void eliminarSoloEstaOcurrencia() {
        Long calendarioId = crearCalendarioTemporal("cancelar-uno");
        Map<String, Object> creado = crearSerieSemanal(calendarioId, "E2E serie cancelar uno", 5);
        Long serieId = numeroLong(creado.get("id"));

        assertEquals(
            204,
            delete(
                "/api/eventos/" + serieId
                    + "?fechaInicioOcurrencia=2027-01-18T18:00:00&alcance=SOLO_ESTE"
            )
        );

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId
                + "/eventos?desde=2027-01-01T00:00:00&hasta=2027-02-15T23:59:59"
        );

        assertEquals(4, eventos.size());
        assertTrue(eventos.stream().noneMatch(e ->
            "2027-01-18T18:00".equals(normalizaFecha(e.get("fechaInicioOriginal")))
        ));
    }

    @Test
    @Order(6)
    @DisplayName("06 - Recurrencia: ESTE_Y_SIGUIENTES divide la serie")
    void modificarEstaYSiguientes() {
        Long calendarioId = crearCalendarioTemporal("split");
        Map<String, Object> creado = crearSerieSemanal(calendarioId, "E2E serie original", 6);
        Long serieId = numeroLong(creado.get("id"));

        Map<String, Object> resultado = patchMap(
            "/api/eventos/" + serieId
                + "?fechaInicioOcurrencia=2027-01-18T18:00:00&alcance=ESTE_Y_SIGUIENTES",
            Map.of(
                "titulo", "E2E serie nueva",
                "fechaInicio", "2027-01-18T19:00:00",
                "fechaFin", "2027-01-18T20:00:00"
            )
        );

        assertEquals("E2E serie nueva", resultado.get("titulo"));
        assertEquals(Boolean.TRUE, resultado.get("recurrente"));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId
                + "/eventos?desde=2027-01-01T00:00:00&hasta=2027-03-01T23:59:59"
        );

        long antiguas = eventos.stream()
            .filter(e -> "E2E serie original".equals(e.get("titulo")))
            .count();

        long nuevas = eventos.stream()
            .filter(e -> "E2E serie nueva".equals(e.get("titulo")))
            .count();

        assertEquals(2, antiguas);
        assertTrue(nuevas >= 1);
        assertTrue(eventos.stream().noneMatch(e ->
            "E2E serie original".equals(e.get("titulo"))
                && String.valueOf(e.get("fechaInicio")).compareTo("2027-01-18T18:00") >= 0
        ));
    }

    @Test
    @Order(7)
    @DisplayName("07 - Recurrencia: TODA_LA_SERIE modifica todas las ocurrencias")
    void modificarTodaSerie() {
        Long calendarioId = crearCalendarioTemporal("toda-serie");
        Map<String, Object> creado = crearSerieSemanal(calendarioId, "E2E serie completa", 4);
        Long serieId = numeroLong(creado.get("id"));

        Map<String, Object> resultado = patchMap(
            "/api/eventos/" + serieId + "?alcance=TODA_LA_SERIE",
            Map.of("titulo", "E2E serie completa modificada")
        );

        assertEquals("E2E serie completa modificada", resultado.get("titulo"));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId
                + "/eventos?desde=2027-01-01T00:00:00&hasta=2027-02-15T23:59:59"
        );

        assertEquals(4, eventos.size());
        assertTrue(eventos.stream().allMatch(e ->
            "E2E serie completa modificada".equals(e.get("titulo"))
        ));
    }

    @Test
    @Order(8)
    @DisplayName("08 - Validación: fecha fin anterior a inicio -> 400")
    void fechasInvalidas() {
        Long calendarioId = crearCalendarioTemporal("fechas-invalidas");

        var ex = assertThrows(
            org.springframework.web.client.RestClientResponseException.class,
            () -> postMap(
                "/api/calendarios/" + calendarioId + "/eventos",
                Map.of(
                    "titulo", "E2E inválido",
                    "fechaInicio", "2027-01-10T11:00:00",
                    "fechaFin", "2027-01-10T10:00:00"
                )
            )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static Long crearCalendarioTemporal(String etiqueta) {
        Map<String, Object> creado = postMap(
            "/api/calendarios",
            Map.of(
                "entidadPropietariaId", residenciaId,
                "nombre", "E2E " + etiqueta + " " + sufijo(),
                "zonaHoraria", "Europe/Madrid",
                "activo", true
            )
        );
        return numeroLong(creado.get("id"));
    }

    private static Map<String, Object> crearSerieSemanal(
        Long calendarioId,
        String titulo,
        int ocurrencias
    ) {
        Map<String, Object> recurrencia = new LinkedHashMap<>();
        recurrencia.put("frecuencia", "SEMANAL");
        recurrencia.put("intervalo", 1);
        recurrencia.put("diasSemana", List.of("MO"));
        recurrencia.put("numeroOcurrencias", ocurrencias);
        recurrencia.put("zonaHoraria", "Europe/Madrid");

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("titulo", titulo);
        cuerpo.put("fechaInicio", "2027-01-04T18:00:00");
        cuerpo.put("fechaFin", "2027-01-04T19:00:00");
        cuerpo.put("zonaHoraria", "Europe/Madrid");
        cuerpo.put("estado", "PROGRAMADO");
        cuerpo.put("recurrencia", recurrencia);

        return postMap("/api/calendarios/" + calendarioId + "/eventos", cuerpo);
    }

    private static Long buscarEntidadExacta(String nombre) {
        String termino = nombre.split(" ")[0];
        return getLista("/api/entidades?nombre=" + termino).stream()
            .filter(e -> nombre.equals(e.get("nombre")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra " + nombre));
    }

    private static String normalizaFecha(Object valor) {
        if (valor == null) return null;
        String s = String.valueOf(valor);
        return s.length() >= 16 ? s.substring(0, 16) : s;
    }

    private static Sesion iniciarSesion(String usuario, String contrasena) {
        Map<String, Object> r = postMapSinAuth(
            "/api/autenticacion/iniciar-sesion",
            Map.of("nombreUsuario", usuario, "contrasena", contrasena)
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) r.get("contextos");

        return new Sesion(
            String.valueOf(r.get("tokenAcceso")),
            raw.stream()
                .map(c -> new Contexto(
                    numeroLong(c.get("entidadContextoId")),
                    String.valueOf(c.get("nombreContexto"))
                ))
                .toList()
        );
    }

    private static Contexto contextoPorNombre(Sesion sesion, String nombre) {
        return sesion.contextos().stream()
            .filter(c -> nombre.equals(c.nombre()))
            .findFirst()
            .orElseThrow();
    }

    private static Map<String, Object> getMap(String ruta) {
        var spec = api.get().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = spec.retrieve().body(Map.class);
        return body;
    }

    private static List<Map<String, Object>> getLista(String ruta) {
        var spec = api.get().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        Object[] raw = spec.retrieve().body(Object[].class);
        if (raw == null) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            Map<?, ?> m = (Map<?, ?>) item;
            Map<String, Object> c = new LinkedHashMap<>();
            m.forEach((k, v) -> c.put(String.valueOf(k), v));
            result.add(c);
        }
        return result;
    }

    private static Map<String, Object> postMap(String ruta, Object body) {
        var spec = api.post().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        spec.body(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = spec.retrieve().body(Map.class);
        return response;
    }

    private static Map<String, Object> postMapSinAuth(String ruta, Object body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = api.post().uri(ruta).body(body)
            .retrieve().body(Map.class);
        return response;
    }

    private static Map<String, Object> putMap(String ruta, Object body) {
        var spec = api.put().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        spec.body(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = spec.retrieve().body(Map.class);
        return response;
    }

    private static Map<String, Object> patchMap(String ruta, Object body) {
        var spec = api.patch().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        spec.body(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = spec.retrieve().body(Map.class);
        return response;
    }

    private static int delete(String ruta) {
        var spec = api.delete().uri(ruta);
        spec.headers(PruebasCalendariosE2E::cabeceras);
        return spec.retrieve().toBodilessEntity().getStatusCode().value();
    }

    private static void cabeceras(HttpHeaders h) {
        h.setBearerAuth(admin.token());
        h.set("X-Contexto-Id", residencia.entidadContextoId().toString());
    }

    private static Long numeroLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        return v == null ? null : Long.valueOf(v.toString());
    }

    private static String sufijo() {
        return ID_EJECUCION + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    record Sesion(String token, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombre) {}
}

package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas E2E contra una API ya arrancada y una BD cargada con
 * gestion_personas_completo_es_v2_corregido.sql.
 *
 * No se ejecuta con "mvn test" normal porque el nombre de la clase
 * no termina en Test/Tests/TestCase.
 *
 * Ejecutar expresamente:
 *   .\mvnw.cmd "-Dtest=PruebasApiE2E" test
 *
 * URL configurable:
 *   API_PRUEBAS_URL=http://localhost:8080
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasApiE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;

    private static Sesion carlos;
    private static Sesion pedro;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Test
    @Order(1)
    @DisplayName("01 - Salud: API y MySQL responden")
    void salud() {
        Map<String, Object> respuesta = getMap("/api/sistema/salud", null, null);

        assertEquals("ACTIVO", respuesta.get("estado"));
        assertEquals("gestion_personas", respuesta.get("baseDatos"));
        assertNotNull(respuesta.get("versionMySQL"));
    }

    @Test
    @Order(2)
    @DisplayName("02 - Seguridad: un endpoint protegido rechaza una petición sin token")
    void endpointProtegidoSinToken() {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getMap("/api/entidades", null, null)
        );

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @Order(3)
    @DisplayName("03 - Autenticación: Carlos puede iniciar sesión")
    void loginCarlos() {
        carlos = iniciarSesion("carlos", "demo");

        assertNotNull(carlos.token());
        assertFalse(carlos.token().isBlank());
        assertNotNull(carlos.cuentaId());
        assertFalse(carlos.contextos().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("04 - Autenticación: /yo devuelve la cuenta autenticada")
    void yoCarlos() {
        asegurarCarlos();

        Map<String, Object> respuesta =
            getMap("/api/autenticacion/yo", carlos.token(), null);

        assertEquals("carlos", respuesta.get("nombreUsuario"));
        assertEquals(carlos.cuentaId(), numeroLong(respuesta.get("cuentaId")));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Contexto: Carlos activa Infantil A y obtiene permisos esperados")
    void activarContextoCarlos() {
        asegurarCarlos();

        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        Map<String, Object> respuesta = postMap(
            "/api/autenticacion/contextos/" + contexto.entidadContextoId() + "/activar",
            carlos.token(),
            null,
            null
        );

        Object permisosObj = respuesta.get("permisos");
        assertInstanceOf(Collection.class, permisosObj);

        Collection<?> permisos = (Collection<?>) permisosObj;
        assertTrue(permisos.contains("ENTIDAD.LEER"));
        assertTrue(permisos.contains("CALENDARIO.LEER"));
        assertTrue(permisos.contains("EVENTO.LEER"));
    }

    @Test
    @Order(6)
    @DisplayName("06 - Entidades: buscar Diego Martín desde Infantil A")
    void buscarEntidad() {
        asegurarCarlos();
        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        List<Map<String, Object>> entidades = getLista(
            "/api/entidades?nombre=Diego",
            carlos.token(),
            contexto.entidadContextoId()
        );

        assertTrue(
            entidades.stream().anyMatch(e -> "Diego Martín".equals(e.get("nombre"))),
            "Debe aparecer Diego Martín"
        );
    }

    @Test
    @Order(7)
    @DisplayName("07 - Calendarios: Infantil A tiene calendario")
    void calendarioInfantil() {
        asegurarCarlos();
        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        List<Map<String, Object>> calendarios = getLista(
            "/api/calendarios?entidadPropietariaId=" + contexto.entidadContextoId(),
            carlos.token(),
            contexto.entidadContextoId()
        );

        assertFalse(calendarios.isEmpty(), "Infantil A debe tener un calendario");
        assertTrue(
            calendarios.stream().anyMatch(c -> "Infantil A".equals(c.get("nombre"))),
            "Debe existir el calendario Infantil A"
        );
    }

    @Test
    @Order(8)
    @DisplayName("08 - Eventos: el calendario de Infantil A devuelve ocurrencias")
    void eventosInfantil() {
        asegurarCarlos();
        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        List<Map<String, Object>> calendarios = getLista(
            "/api/calendarios?entidadPropietariaId=" + contexto.entidadContextoId(),
            carlos.token(),
            contexto.entidadContextoId()
        );

        Long calendarioId = numeroLong(calendarios.get(0).get("id"));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioId +
                "/eventos?desde=2026-10-01T00:00:00&hasta=2026-10-31T23:59:59",
            carlos.token(),
            contexto.entidadContextoId()
        );

        assertFalse(eventos.isEmpty(), "Debe existir al menos un evento/ocurrencia");
    }

    @Test
    @Order(9)
    @DisplayName("09 - Solicitudes: el servicio de listado responde")
    void solicitudes() {
        asegurarCarlos();
        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        List<Map<String, Object>> solicitudes = getLista(
            "/api/solicitudes",
            carlos.token(),
            contexto.entidadContextoId()
        );

        assertNotNull(solicitudes);
        assertFalse(solicitudes.isEmpty(), "Los datos demo contienen solicitudes");
    }

    @Test
    @Order(10)
    @DisplayName("10 - Autorización: Carlos no puede administrar cuentas desde Infantil A")
    void accesoDenegadoCuentas() {
        asegurarCarlos();
        Contexto contexto = contextoPorNombre(carlos, "CB Tres Cantos");

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getMap(
                "/api/cuentas/" + carlos.cuentaId(),
                carlos.token(),
                contexto.entidadContextoId()
            )
        );

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @Order(11)
    @DisplayName("11 - Autenticación: Pedro puede iniciar sesión en Residencia")
    void loginPedro() {
        pedro = iniciarSesion("pedro", "demo");

        Contexto residencia = contextoPorNombre(pedro, "Residencia Los Olivos");
        assertNotNull(residencia.entidadContextoId());
    }

    @Test
    @Order(12)
    @DisplayName("12 - Documentos: Pedro puede consultar documentos sensibles de Ana")
    void documentosAna() {
        asegurarPedro();
        Contexto contexto = contextoPorNombre(pedro, "Residencia Los Olivos");

        List<Map<String, Object>> entidades = getLista(
            "/api/entidades?nombre=Ana",
            pedro.token(),
            contexto.entidadContextoId()
        );

        Map<String, Object> ana = entidades.stream()
            .filter(e -> "Ana López".equals(e.get("nombre")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra Ana López"));

        Long anaId = numeroLong(ana.get("id"));

        List<Map<String, Object>> documentos = getLista(
            "/api/entidades/" + anaId + "/documentos",
            pedro.token(),
            contexto.entidadContextoId()
        );

        assertTrue(
            documentos.stream().anyMatch(d -> "INFORME_MEDICO".equals(d.get("codigoTipo"))),
            "Pedro debe poder ver el informe médico de Ana"
        );

        Map<String, Object> informe = documentos.stream()
            .filter(d -> "INFORME_MEDICO".equals(d.get("codigoTipo")))
            .filter(d -> "Informe médico septiembre 2026".equals(d.get("nombre")))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No se encuentra el documento demo 'Informe médico septiembre 2026'"
            ));

        assertEquals("SENSIBLE", informe.get("codigoClasificacion"));
        assertEquals("SALUD", informe.get("codigoCategoriaSensible"));
        assertEquals("DISPONIBLE", informe.get("estado"));
        assertFalse(
            informe.containsKey("referenciaArchivo"),
            "Los metadatos no deben exponer referenciaArchivo"
        );
    }

    @Test
    @Order(13)
    @DisplayName("13 - Documentos: consultar metadatos no consume accesos")
    void metadatosNoConsumenAcceso() {
        asegurarPedro();
        Contexto contexto = contextoPorNombre(pedro, "Residencia Los Olivos");

        Long documentoId = buscarDocumentoPorTipo("INFORME_MEDICO", pedro, contexto);

        Map<String, Object> antes =
            getMap("/api/documentos/" + documentoId, pedro.token(), contexto.entidadContextoId());

        long accesosAntes = numeroLong(antes.get("numeroAccesos"));

        Map<String, Object> despues =
            getMap("/api/documentos/" + documentoId, pedro.token(), contexto.entidadContextoId());

        long accesosDespues = numeroLong(despues.get("numeroAccesos"));

        assertEquals(accesosAntes, accesosDespues);
    }

    @Test
    @Order(14)
    @DisplayName("14 - OpenAPI: existen los servicios principales")
    void contratoOpenApi() {
        Map<String, Object> openApi = getMap("/v3/api-docs", null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");

        assertNotNull(paths);

        List<String> obligatorios = List.of(
            "/api/sistema/salud",
            "/api/autenticacion/iniciar-sesion",
            "/api/autenticacion/yo",
            "/api/autenticacion/contextos",
            "/api/entidades",
            "/api/entidades/{id}",
            "/api/cuentas/{id}",
            "/api/calendarios",
            "/api/calendarios/{calendarioId}/eventos",
            "/api/eventos/{eventoId}",
            "/api/solicitudes",
            "/api/solicitudes/{id}",
            "/api/documentos/{id}",
            "/api/documentos/{id}/acceder",
            "/api/documentos/{id}/accesos"
        );

        for (String path : obligatorios) {
            assertTrue(paths.containsKey(path), "Falta en OpenAPI: " + path);
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static Sesion iniciarSesion(String usuario, String contrasena) {
        Map<String, Object> respuesta = postMap(
            "/api/autenticacion/iniciar-sesion",
            null,
            null,
            Map.of("nombreUsuario", usuario, "contrasena", contrasena)
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contextosRaw =
            (List<Map<String, Object>>) respuesta.get("contextos");

        List<Contexto> contextos = contextosRaw.stream()
            .map(c -> new Contexto(
                numeroLong(c.get("entidadContextoId")),
                String.valueOf(c.get("nombreContexto")),
                String.valueOf(c.get("codigoRol")),
                c.get("alcance") == null ? null : String.valueOf(c.get("alcance"))
            ))
            .toList();

        return new Sesion(
            String.valueOf(respuesta.get("tokenAcceso")),
            numeroLong(respuesta.get("cuentaId")),
            respuesta.get("personaEntidadId") == null
                ? null
                : numeroLong(respuesta.get("personaEntidadId")),
            contextos
        );
    }

    private static Contexto contextoPorNombre(Sesion sesion, String nombre) {
        return sesion.contextos().stream()
            .filter(c -> nombre.equals(c.nombreContexto()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No existe el contexto '" + nombre + "'. Contextos: " + sesion.contextos()
            ));
    }

    private static Long buscarDocumentoPorTipo(String codigoTipo, Sesion sesion, Contexto contexto) {
        List<Map<String, Object>> entidades = getLista(
            "/api/entidades?nombre=Ana",
            sesion.token(),
            contexto.entidadContextoId()
        );

        Long anaId = entidades.stream()
            .filter(e -> "Ana López".equals(e.get("nombre")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseThrow();

        return getLista(
            "/api/entidades/" + anaId + "/documentos",
            sesion.token(),
            contexto.entidadContextoId()
        ).stream()
            .filter(d -> codigoTipo.equals(d.get("codigoTipo")))
            .filter(d -> !"INFORME_MEDICO".equals(codigoTipo)
                || "Informe médico septiembre 2026".equals(d.get("nombre")))
            .map(d -> numeroLong(d.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No existe documento " + codigoTipo));
    }

    private static void asegurarCarlos() {
        if (carlos == null) carlos = iniciarSesion("carlos", "demo");
    }

    private static void asegurarPedro() {
        if (pedro == null) pedro = iniciarSesion("pedro", "demo");
    }

    private static Map<String, Object> getMap(String ruta, String token, Long contextoId) {
        var spec = api.get().uri(ruta);
        spec.headers(h -> cabeceras(h, token, contextoId));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = spec.retrieve().body(Map.class);
        return body;
    }

    private static List<Map<String, Object>> getLista(String ruta, String token, Long contextoId) {
        var spec = api.get().uri(ruta);
        spec.headers(h -> cabeceras(h, token, contextoId));

        Object[] raw = spec.retrieve().body(Object[].class);
        if (raw == null) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            if (!(item instanceof Map<?, ?>)) {
                fail("Se esperaba un objeto JSON en la lista y se recibió " + item);
            }

            Map<?, ?> map = (Map<?, ?>) item;
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((k, v) -> converted.put(String.valueOf(k), v));
            result.add(converted);
        }
        return result;
    }

    private static Map<String, Object> postMap(
        String ruta,
        String token,
        Long contextoId,
        Object cuerpo
    ) {
        var spec = api.post().uri(ruta);
        spec.headers(h -> cabeceras(h, token, contextoId));

        RestClient.RequestBodySpec bodySpec = spec;
        if (cuerpo != null) bodySpec.body(cuerpo);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = bodySpec.retrieve().body(Map.class);
        return body;
    }

    private static void cabeceras(HttpHeaders headers, String token, Long contextoId) {
        if (token != null) headers.setBearerAuth(token);
        if (contextoId != null) headers.set("X-Contexto-Id", contextoId.toString());
    }

    private static Long numeroLong(Object valor) {
        if (valor instanceof Number n) return n.longValue();
        if (valor == null) return null;
        return Long.valueOf(valor.toString());
    }

    record Sesion(String token, Long cuentaId, Long personaEntidadId, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombreContexto, String codigoRol, String alcance) {}
}

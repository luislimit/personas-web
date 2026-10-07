package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Batería E2E de aislamiento transversal entre contextos.
 *
 * Objetivo:
 * comprobar que no basta con tener el permiso funcional. El recurso concreto
 * debe pertenecer al alcance del contexto activo.
 *
 * Requiere:
 * - BD gestion_personas_completo_es_v2_2.sql
 * - fix de alcance de entidades aplicado
 * - backend arrancado
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasAislamientoE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;

    private static Sesion adminResidencia;
    private static Contexto contextoResidencia;

    private static Sesion carlos;
    private static Contexto contextoInfantil;

    private static Sesion maria;
    private static Contexto contextoClub;

    private static Long infantilId;
    private static Long diegoId;
    private static Long mariaId;

    private static Long calendarioInfantilId;
    private static Long eventoInfantilId;
    private static Long documentoLicenciaDiegoId;
    private static Long solicitudClubId;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();

        adminResidencia = iniciarSesion("admin.residencia", "demo");
        contextoResidencia = contextoPorNombre(adminResidencia, "Residencia Los Olivos");

        carlos = iniciarSesion("carlos", "demo");
        contextoInfantil = contextoPorNombre(carlos, "CB Tres Cantos");

        maria = iniciarSesion("maria", "demo");
        contextoClub = contextoPorNombre(maria, "CB Tres Cantos");

        infantilId = buscarEntidadExacta("Infantil A", carlos, contextoInfantil);
        diegoId = buscarEntidadExacta("Diego Martín", carlos, contextoInfantil);
        mariaId = buscarEntidadExacta("María Martín", maria, contextoClub);

        List<Map<String, Object>> calendarios = getLista(
            "/api/calendarios?entidadPropietariaId=" + infantilId,
            carlos.token(),
            contextoInfantil.entidadContextoId()
        );
        calendarioInfantilId = calendarios.stream()
            .filter(c -> "Infantil A".equals(c.get("nombre")))
            .map(c -> numeroLong(c.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No se encuentra el calendario demo 'Infantil A'"
            ));

        List<Map<String, Object>> eventos = getLista(
            "/api/calendarios/" + calendarioInfantilId
                + "/eventos?desde=2026-10-01T00:00:00&hasta=2026-10-31T23:59:59",
            carlos.token(),
            contextoInfantil.entidadContextoId()
        );
        eventoInfantilId = eventos.stream()
            .filter(e -> "Entrenamiento Infantil A".equals(e.get("titulo")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseGet(() -> eventos.stream()
                .filter(e -> "Partido Infantil A".equals(e.get("titulo")))
                .map(e -> numeroLong(e.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                    "No se encuentra ningún evento demo de Infantil A"
                )));

        // Carlos (GESTOR_CONTEXTO) no tiene DOCUMENTO.LEER. Para obtener de forma
        // legítima el id de la licencia demo usamos a María, que actúa en el Club
        // con alcance RELACIONADOS y es madre de Diego.
        List<Map<String, Object>> documentos = getLista(
            "/api/entidades/" + diegoId + "/documentos",
            maria.token(),
            contextoClub.entidadContextoId()
        );
        documentoLicenciaDiegoId = documentos.stream()
            .filter(d -> "LICENCIA".equals(d.get("codigoTipo")))
            .filter(d -> "Licencia federativa 2026/27".equals(d.get("nombre")))
            .map(d -> numeroLong(d.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No se encuentra la licencia demo 'Licencia federativa 2026/27'"
            ));

        List<Map<String, Object>> solicitudes = getLista(
            "/api/solicitudes",
            maria.token(),
            contextoClub.entidadContextoId()
        );
        solicitudClubId = solicitudes.stream()
            .filter(s -> "Reunión con entrenador".equals(s.get("asunto")))
            .map(s -> numeroLong(s.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra la solicitud demo del Club"));
    }

    @Test
    @Order(1)
    @DisplayName("01 - Calendarios: Residencia no debe listar calendarios del Club")
    void residenciaNoListaCalendariosClub() {
        List<Map<String, Object>> calendarios = getLista(
            "/api/calendarios",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        );

        assertTrue(
            calendarios.stream().noneMatch(c ->
                "Infantil A".equals(c.get("nombre"))
                    || "Club - General".equals(c.get("nombre"))
                    || "Junior A".equals(c.get("nombre"))
            ),
            "Un contexto de Residencia no debe recibir calendarios del Club"
        );
    }

    @Test
    @Order(2)
    @DisplayName("02 - Calendarios: acceso directo a calendario fuera de alcance -> 403")
    void residenciaNoAbreCalendarioClub() {
        assertForbidden(() -> getMap(
            "/api/calendarios/" + calendarioInfantilId,
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        ));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Eventos: acceso directo a evento fuera de alcance -> 403")
    void residenciaNoAbreEventoClub() {
        assertForbidden(() -> getMap(
            "/api/eventos/" + eventoInfantilId,
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        ));
    }

    @Test
    @Order(4)
    @DisplayName("04 - Calendarios: no se puede crear calendario para propietario fuera del contexto")
    void residenciaNoCreaCalendarioEnClub() {
        assertForbidden(() -> postMap(
            "/api/calendarios",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId(),
            Map.of(
                "entidadPropietariaId", infantilId,
                "nombre", "E2E calendario indebido",
                "descripcion", "No debe crearse",
                "zonaHoraria", "Europe/Madrid",
                "activo", true
            )
        ));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Documentos: acceso directo a documento personal de otro contexto -> 403")
    void residenciaNoAbreDocumentoClub() {
        assertForbidden(() -> getMap(
            "/api/documentos/" + documentoLicenciaDiegoId,
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        ));
    }

    @Test
    @Order(6)
    @DisplayName("06 - Documentos: no se puede crear documento para destinatario fuera del contexto")
    void residenciaNoCreaDocumentoParaClub() {
        assertForbidden(() -> postMap(
            "/api/documentos",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId(),
            Map.of(
                "codigoTipo", "LICENCIA",
                "entidadEmisoraId", contextoResidencia.entidadContextoId(),
                "entidadDestinatariaId", diegoId,
                "nombre", "E2E documento indebido",
                "referenciaArchivo", "demo://e2e/no-debe-crearse.pdf",
                "fechaDocumento", "2026-10-04"
            )
        ));
    }

    @Test
    @Order(7)
    @DisplayName("07 - Solicitudes: Residencia no debe listar solicitudes del Club")
    void residenciaNoListaSolicitudesClub() {
        List<Map<String, Object>> solicitudes = getLista(
            "/api/solicitudes",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        );

        assertTrue(
            solicitudes.stream().noneMatch(s -> solicitudClubId.equals(numeroLong(s.get("id")))),
            "La solicitud del Club no debe aparecer en el contexto Residencia"
        );
    }

    @Test
    @Order(8)
    @DisplayName("08 - Solicitudes: acceso directo a solicitud fuera de alcance -> 403")
    void residenciaNoAbreSolicitudClub() {
        assertForbidden(() -> getMap(
            "/api/solicitudes/" + solicitudClubId,
            adminResidencia.token(),
            contextoResidencia.entidadContextoId()
        ));
    }

    @Test
    @Order(9)
    @DisplayName("09 - Solicitudes: no se puede escribir mensaje en solicitud de otro contexto")
    void residenciaNoEscribeSolicitudClub() {
        assertForbidden(() -> postMap(
            "/api/solicitudes/" + solicitudClubId + "/mensajes",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId(),
            Map.of(
                "entidadRemitenteId", contextoResidencia.entidadContextoId(),
                "mensaje", "Este mensaje no debe poder escribirse"
            )
        ));
    }

    @Test
    @Order(10)
    @DisplayName("10 - Solicitudes: no se puede crear solicitud usando creador fuera de alcance")
    void residenciaNoCreaSolicitudComoMaria() {
        assertForbidden(() -> postMap(
            "/api/solicitudes",
            adminResidencia.token(),
            contextoResidencia.entidadContextoId(),
            Map.of(
                "codigoTipoSolicitud", "CONSULTA_GENERAL",
                "asunto", "E2E solicitud indebida",
                "descripcion", "No debe crearse desde Residencia como María",
                "creadoPorEntidadId", mariaId,
                "participantes", List.of(
                    Map.of(
                        "entidadId", mariaId,
                        "rolParticipante", "SOLICITANTE"
                    )
                )
            )
        ));
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static void assertForbidden(ThrowingRunnable action) {
        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            action::run
        );
        assertEquals(
            403,
            ex.getStatusCode().value(),
            "Se esperaba 403 y se recibió " + ex.getStatusCode().value()
        );
    }

    private static Long buscarEntidadExacta(
        String nombre,
        Sesion sesion,
        Contexto contexto
    ) {
        String termino = nombre.split(" ")[0];

        List<Map<String, Object>> lista = getLista(
            "/api/entidades?nombre=" + termino,
            sesion.token(),
            contexto.entidadContextoId()
        );

        return lista.stream()
            .filter(e -> nombre.equals(e.get("nombre")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra la entidad " + nombre));
    }

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
                String.valueOf(c.get("codigoRol"))
            ))
            .toList();

        return new Sesion(
            String.valueOf(respuesta.get("tokenAcceso")),
            numeroLong(respuesta.get("cuentaId")),
            contextos
        );
    }

    private static Contexto contextoPorNombre(Sesion sesion, String nombre) {
        return sesion.contextos().stream()
            .filter(c -> nombre.equals(c.nombreContexto()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No existe contexto " + nombre));
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
                throw new AssertionError("Se esperaba objeto JSON y se recibió " + item);
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

    @FunctionalInterface
    interface ThrowingRunnable {
        void run();
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombreContexto, String codigoRol) {}
}

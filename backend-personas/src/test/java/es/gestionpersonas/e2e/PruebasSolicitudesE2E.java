package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas funcionales E2E del ciclo de solicitudes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasSolicitudesE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;
    private static Sesion admin;
    private static Contexto residencia;
    private static final String ID_EJECUCION =
        UUID.randomUUID().toString().substring(0, 8);

    private static Long residenciaId;
    private static Long anaId;
    private static Long administracionId;
    private static Long servicioMedicoId;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        admin = iniciarSesion("admin.residencia", "demo");
        residencia = contextoPorNombre(admin, "Residencia Los Olivos");

        residenciaId = residencia.entidadContextoId();
        anaId = buscarEntidadExacta("Ana López");
        administracionId = buscarEntidadExacta("Administración");
        servicioMedicoId = buscarEntidadExacta("Servicio médico");
    }

    @Test
    @Order(1)
    @DisplayName("01 - Crear solicitud genera participantes y acción CREADA")
    void crearSolicitud() {
        Map<String, Object> creada = crearSolicitudTemporal("E2E crear " + sufijo());

        Long id = numeroLong(creada.get("id"));

        assertNotNull(id);
        assertEquals("CONSULTA_GENERAL", creada.get("codigoTipoSolicitud"));
        assertEquals("ABIERTA", creada.get("estadoGlobal"));

        List<Map<String, Object>> participantes = lista(creada.get("participantes"));
        assertEquals(2, participantes.size());

        assertParticipante(participantes, anaId, "SOLICITANTE", "EN_ESPERA");
        assertParticipante(participantes, administracionId, "DESTINATARIO", "PENDIENTE");

        List<Map<String, Object>> acciones = lista(creada.get("acciones"));
        assertTrue(acciones.stream().anyMatch(a -> "CREADA".equals(a.get("tipoAccion"))));
    }

    @Test
    @Order(2)
    @DisplayName("02 - Modificar solicitud actualiza asunto y descripción")
    void modificarSolicitud() {
        Long id = numeroLong(crearSolicitudTemporal("E2E modificar " + sufijo()).get("id"));

        Map<String, Object> modificada = patchMap(
            "/api/solicitudes/" + id,
            Map.of(
                "asunto", "E2E asunto modificado",
                "descripcion", "Descripción modificada"
            )
        );

        assertEquals("E2E asunto modificado", modificada.get("asunto"));
        assertEquals("Descripción modificada", modificada.get("descripcion"));
        assertEquals("ABIERTA", modificada.get("estadoGlobal"));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Mensajes quedan trazados y generan acción")
    void mensajes() {
        Long id = numeroLong(crearSolicitudTemporal("E2E mensajes " + sufijo()).get("id"));

        Map<String, Object> mensaje = postMap(
            "/api/solicitudes/" + id + "/mensajes",
            Map.of(
                "entidadRemitenteId", anaId,
                "mensaje", "Mensaje funcional E2E"
            )
        );

        assertEquals(anaId, numeroLong(mensaje.get("entidadRemitenteId")));
        assertEquals("Mensaje funcional E2E", mensaje.get("mensaje"));
        assertNotNull(mensaje.get("fechaCreacion"));

        Map<String, Object> detalle = getMap("/api/solicitudes/" + id);

        List<Map<String, Object>> mensajes = lista(detalle.get("mensajes"));
        assertTrue(mensajes.stream().anyMatch(m ->
            "Mensaje funcional E2E".equals(m.get("mensaje"))
        ));

        List<Map<String, Object>> acciones = lista(detalle.get("acciones"));
        assertTrue(acciones.stream().anyMatch(a ->
            "MENSAJE_ANADIDO".equals(a.get("tipoAccion"))
        ));
    }

    @Test
    @Order(4)
    @DisplayName("04 - Participantes: añadir, cambiar estado/marcar leído y eliminar")
    void cicloParticipante() {
        Long id = numeroLong(crearSolicitudTemporal("E2E participante " + sufijo()).get("id"));

        Map<String, Object> añadido = postMap(
            "/api/solicitudes/" + id + "/participantes",
            Map.of(
                "entidadId", servicioMedicoId,
                "rolParticipante", "DESTINATARIO"
            )
        );

        assertEquals("PENDIENTE", añadido.get("estado"));

        Map<String, Object> actualizado = patchMap(
            "/api/solicitudes/" + id
                + "/participantes/" + servicioMedicoId
                + "?rolParticipante=DESTINATARIO",
            Map.of(
                "estado", "COMPLETADA",
                "marcarLeido", true
            )
        );

        assertEquals("COMPLETADA", actualizado.get("estado"));
        assertNotNull(actualizado.get("fechaLectura"));

        assertEquals(
            204,
            delete(
                "/api/solicitudes/" + id
                    + "/participantes/" + servicioMedicoId
                    + "?rolParticipante=DESTINATARIO"
            )
        );

        Map<String, Object> detalle = getMap("/api/solicitudes/" + id);
        assertTrue(lista(detalle.get("participantes")).stream()
            .noneMatch(p -> servicioMedicoId.equals(numeroLong(p.get("entidadId")))));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Acción puede cambiar estado global y estado de participante")
    void accionCambiosEstado() {
        Long id = numeroLong(crearSolicitudTemporal("E2E acción " + sufijo()).get("id"));

        Map<String, Object> accion = postMap(
            "/api/solicitudes/" + id + "/acciones",
            Map.of(
                "entidadActorId", administracionId,
                "tipoAccion", "RESPUESTA",
                "estadoGlobal", "RESUELTA",
                "entidadParticipanteId", administracionId,
                "rolParticipante", "DESTINATARIO",
                "estadoParticipante", "COMPLETADA",
                "datos", Map.of("resultado", "OK")
            )
        );

        assertEquals("RESPUESTA", accion.get("tipoAccion"));

        Map<String, Object> detalle = getMap("/api/solicitudes/" + id);
        assertEquals("RESUELTA", detalle.get("estadoGlobal"));
        assertNotNull(detalle.get("fechaResolucion"));

        List<Map<String, Object>> participantes = lista(detalle.get("participantes"));
        assertParticipante(
            participantes,
            administracionId,
            "DESTINATARIO",
            "COMPLETADA"
        );
    }

    @Test
    @Order(6)
    @DisplayName("06 - Flujo completo: ABIERTA -> RESUELTA -> CERRADA")
    void flujoEstados() {
        Long id = numeroLong(crearSolicitudTemporal("E2E estados " + sufijo()).get("id"));

        Map<String, Object> resuelta = patchMap(
            "/api/solicitudes/" + id,
            Map.of("estadoGlobal", "RESUELTA")
        );

        assertEquals("RESUELTA", resuelta.get("estadoGlobal"));
        assertNotNull(resuelta.get("fechaResolucion"));

        Map<String, Object> cerrada = patchMap(
            "/api/solicitudes/" + id,
            Map.of("estadoGlobal", "CERRADA")
        );

        assertEquals("CERRADA", cerrada.get("estadoGlobal"));
        assertNotNull(cerrada.get("fechaCierre"));
    }

    @Test
    @Order(7)
    @DisplayName("07 - Estado de solicitud no válido -> 400")
    void estadoSolicitudInvalido() {
        Long id = numeroLong(crearSolicitudTemporal("E2E estado inválido " + sufijo()).get("id"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> patchMap(
                "/api/solicitudes/" + id,
                Map.of("estadoGlobal", "INEXISTENTE")
            )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @Order(8)
    @DisplayName("08 - Estado de participante no válido -> 400")
    void estadoParticipanteInvalido() {
        Long id = numeroLong(crearSolicitudTemporal("E2E participante inválido " + sufijo()).get("id"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> patchMap(
                "/api/solicitudes/" + id
                    + "/participantes/" + administracionId
                    + "?rolParticipante=DESTINATARIO",
                Map.of("estado", "INEXISTENTE")
            )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @Order(9)
    @DisplayName("09 - Añadir dos veces el mismo participante es idempotente")
    void participanteDuplicado() {
        Long id = numeroLong(crearSolicitudTemporal("E2E duplicado " + sufijo()).get("id"));

        Map<String, Object> primero = postMap(
            "/api/solicitudes/" + id + "/participantes",
            Map.of(
                "entidadId", servicioMedicoId,
                "rolParticipante", "DESTINATARIO"
            )
        );

        Map<String, Object> segundo = postMap(
            "/api/solicitudes/" + id + "/participantes",
            Map.of(
                "entidadId", servicioMedicoId,
                "rolParticipante", "DESTINATARIO"
            )
        );

        assertEquals(numeroLong(primero.get("entidadId")), numeroLong(segundo.get("entidadId")));

        Map<String, Object> detalle = getMap("/api/solicitudes/" + id);

        long coincidencias = lista(detalle.get("participantes")).stream()
            .filter(p ->
                servicioMedicoId.equals(numeroLong(p.get("entidadId")))
                    && "DESTINATARIO".equals(p.get("rolParticipante"))
            )
            .count();

        assertEquals(1, coincidencias);
    }

    @Test
    @Order(10)
    @DisplayName("10 - Listado incluye la solicitud recién creada")
    void listado() {
        String asunto = "E2E listado " + sufijo();
        Long id = numeroLong(crearSolicitudTemporal(asunto).get("id"));

        List<Map<String, Object>> solicitudes = getLista("/api/solicitudes");

        assertTrue(solicitudes.stream().anyMatch(s ->
            id.equals(numeroLong(s.get("id")))
                && asunto.equals(s.get("asunto"))
        ));
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static Map<String, Object> crearSolicitudTemporal(String asunto) {
        return postMap(
            "/api/solicitudes",
            Map.of(
                "codigoTipoSolicitud", "CONSULTA_GENERAL",
                "asunto", asunto,
                "descripcion", "Solicitud creada por pruebas E2E",
                "creadoPorEntidadId", anaId,
                "participantes", List.of(
                    Map.of(
                        "entidadId", anaId,
                        "rolParticipante", "SOLICITANTE"
                    ),
                    Map.of(
                        "entidadId", administracionId,
                        "rolParticipante", "DESTINATARIO"
                    )
                )
            )
        );
    }

    private static void assertParticipante(
        List<Map<String, Object>> participantes,
        Long entidadId,
        String rol,
        String estado
    ) {
        Map<String, Object> p = participantes.stream()
            .filter(x ->
                entidadId.equals(numeroLong(x.get("entidadId")))
                    && rol.equals(x.get("rolParticipante"))
            )
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No existe participante " + entidadId + " / " + rol
            ));

        assertEquals(estado, p.get("estado"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object value) {
        if (value == null) return List.of();
        return (List<Map<String, Object>>) value;
    }

    private static Long buscarEntidadExacta(String nombre) {
        String termino = nombre.split(" ")[0];
        return getLista("/api/entidades?nombre=" + termino).stream()
            .filter(e -> nombre.equals(e.get("nombre")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra " + nombre));
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
        spec.headers(PruebasSolicitudesE2E::cabeceras);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = spec.retrieve().body(Map.class);
        return response;
    }

    private static List<Map<String, Object>> getLista(String ruta) {
        var spec = api.get().uri(ruta);
        spec.headers(PruebasSolicitudesE2E::cabeceras);
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
        spec.headers(PruebasSolicitudesE2E::cabeceras);
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

    private static Map<String, Object> patchMap(String ruta, Object body) {
        var spec = api.patch().uri(ruta);
        spec.headers(PruebasSolicitudesE2E::cabeceras);
        spec.body(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = spec.retrieve().body(Map.class);
        return response;
    }

    private static int delete(String ruta) {
        var spec = api.delete().uri(ruta);
        spec.headers(PruebasSolicitudesE2E::cabeceras);
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

package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Batería funcional E2E de gestión documental.
 *
 * Requisitos:
 * - API arrancada.
 * - BD demo actual cargada.
 * - Usuario pedro/demo disponible en "Residencia Los Olivos".
 *
 * Ejecutar:
 *   .\mvnw.cmd "-Dtest=PruebasDocumentosE2E" test
 *
 * URL:
 *   API_PRUEBAS_URL=http://localhost:8085
 *
 * IMPORTANTE:
 * Esta batería crea documentos de prueba en la BD.
 * Los documentos accedidos no se eliminan por diseño.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasDocumentosE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;
    private static Sesion pedro;
    private static Contexto residencia;
    private static final String ID_EJECUCION =
        UUID.randomUUID().toString().substring(0, 8);
    private static Long anaId;
    private static Long servicioMedicoId;

    /**
     * Si falta alguno de estos permisos no seguimos con las pruebas funcionales
     * destructivas, porque cualquier resultado sería 403 y ocultaría la regla
     * de negocio que queremos comprobar.
     */
    private static boolean permisosDocumentalesCompletos;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();

        pedro = iniciarSesion("pedro", "demo");
        residencia = contextoPorNombre(pedro, "Residencia Los Olivos");

        anaId = buscarEntidadExacta("Ana López");
        servicioMedicoId = buscarEntidadExacta("Servicio médico");

        Map<String, Object> activacion = postMap(
            "/api/autenticacion/contextos/" + residencia.entidadContextoId() + "/activar",
            pedro.token(),
            null,
            null
        );

        @SuppressWarnings("unchecked")
        Collection<String> permisos =
            (Collection<String>) activacion.getOrDefault("permisos", List.of());

        permisosDocumentalesCompletos =
            permisos.contains("DOCUMENTO.LEER")
            && permisos.contains("DOCUMENTO.CREAR")
            && permisos.contains("DOCUMENTO.MODIFICAR")
            && permisos.contains("DOCUMENTO.ELIMINAR")
            && permisos.contains("INFORMACION_SENSIBLE.LEER")
            && permisos.contains("INFORMACION_SENSIBLE.MODIFICAR");
    }

    @Test
    @Order(1)
    @DisplayName("01 - Diagnóstico: Pedro tiene permisos completos para probar el ciclo documental")
    void diagnosticoPermisos() {
        assertTrue(
            permisosDocumentalesCompletos,
            """
            Para ejecutar la batería funcional completa, el contexto de Pedro debe incluir:
            DOCUMENTO.LEER
            DOCUMENTO.CREAR
            DOCUMENTO.MODIFICAR
            DOCUMENTO.ELIMINAR
            INFORMACION_SENSIBLE.LEER
            INFORMACION_SENSIBLE.MODIFICAR

            Si falla esta prueba hemos detectado una carencia de configuración/permisos,
            no un fallo del servicio documental.
            """
        );
    }

    @Test
    @Order(2)
    @DisplayName("02 - Crear: un documento nuevo nace en BORRADOR")
    void crearDocumentoBorrador() {
        exigirPermisos();

        Map<String, Object> creado = crearDocumento(
            "INFORME_MEDICO",
            "E2E - borrador " + sufijo(),
            null
        );

        assertNotNull(creado.get("id"));
        assertEquals("BORRADOR", creado.get("estado"));
        assertEquals("INFORME_MEDICO", creado.get("codigoTipo"));
        assertEquals(0L, numeroLong(creado.get("numeroAccesos")));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Estado: un BORRADOR no puede ser accedido")
    void borradorNoSePuedeAcceder() {
        exigirPermisos();

        Map<String, Object> creado = crearDocumento(
            "INFORME_MEDICO",
            "E2E - no accesible " + sufijo(),
            null
        );

        Long documentoId = numeroLong(creado.get("id"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> postMap(
                "/api/documentos/" + documentoId + "/acceder",
                pedro.token(),
                residencia.entidadContextoId(),
                null
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(4)
    @DisplayName("04 - Publicación: BORRADOR -> DISPONIBLE")
    void publicarDocumento() {
        exigirPermisos();

        Long id = numeroLong(crearDocumento(
            "INFORME_MEDICO",
            "E2E - publicar " + sufijo(),
            null
        ).get("id"));

        Map<String, Object> publicado = patchMap(
            "/api/documentos/" + id,
            pedro.token(),
            residencia.entidadContextoId(),
            Map.of("estado", "DISPONIBLE")
        );

        assertEquals("DISPONIBLE", publicado.get("estado"));
        assertNotNull(publicado.get("fechaDisponibilidad"));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Acceso: un documento DISPONIBLE registra quién y cuándo lo accedió")
    void accesoRegistraTrazabilidad() {
        exigirPermisos();

        Long id = crearYPublicar("INFORME_MEDICO", "E2E - acceso " + sufijo(), null);

        Map<String, Object> acceso = postMap(
            "/api/documentos/" + id + "/acceder",
            pedro.token(),
            residencia.entidadContextoId(),
            null
        );

        assertEquals(id, numeroLong(acceso.get("documentoId")));
        assertNotNull(acceso.get("accesoId"));
        assertNotNull(acceso.get("fechaAcceso"));
        assertNotNull(acceso.get("referenciaArchivo"));

        List<Map<String, Object>> accesos = getLista(
            "/api/documentos/" + id + "/accesos",
            pedro.token(),
            residencia.entidadContextoId()
        );

        assertEquals(1, accesos.size());
        assertEquals("pedro", accesos.get(0).get("nombreUsuario"));
        assertEquals("Pedro Ruiz", accesos.get(0).get("nombrePersona"));
        assertNotNull(accesos.get(0).get("fechaAcceso"));
    }

    @Test
    @Order(6)
    @DisplayName("06 - Inmutabilidad: un documento accedido no puede modificarse")
    void documentoAccedidoNoSePuedeModificar() {
        exigirPermisos();

        Long id = crearYPublicar("INFORME_MEDICO", "E2E - inmutable " + sufijo(), null);
        acceder(id);

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> patchMap(
                "/api/documentos/" + id,
                pedro.token(),
                residencia.entidadContextoId(),
                Map.of("nombre", "Nombre que no debe guardarse")
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(7)
    @DisplayName("07 - Eliminación: un documento accedido no puede eliminarse")
    void documentoAccedidoNoSePuedeEliminar() {
        exigirPermisos();

        Long id = crearYPublicar("INFORME_MEDICO", "E2E - no eliminar " + sufijo(), null);
        acceder(id);

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> delete(
                "/api/documentos/" + id,
                pedro.token(),
                residencia.entidadContextoId()
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(8)
    @DisplayName("08 - Eliminación: un documento no accedido sí puede eliminarse")
    void documentoNoAccedidoSePuedeEliminar() {
        exigirPermisos();

        Long id = numeroLong(crearDocumento(
            "INFORME_MEDICO",
            "E2E - eliminable " + sufijo(),
            null
        ).get("id"));

        int status = delete(
            "/api/documentos/" + id,
            pedro.token(),
            residencia.entidadContextoId()
        );

        assertEquals(204, status);

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> getMap(
                "/api/documentos/" + id,
                pedro.token(),
                residencia.entidadContextoId()
            )
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @Order(9)
    @DisplayName("09 - Sustitución: solo se puede sustituir un documento que ya ha sido accedido")
    void soloDocumentoAccedidoPuedeSustituirse() {
        exigirPermisos();

        Long original = crearYPublicar(
            "INFORME_MEDICO",
            "E2E - original sin acceso " + sufijo(),
            null
        );

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> crearDocumento(
                "INFORME_MEDICO",
                "E2E - sustituto inválido " + sufijo(),
                original
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(10)
    @DisplayName("10 - Sustitución: el sustituto debe ser del mismo tipo")
    void sustitutoDebeSerMismoTipo() {
        exigirPermisos();

        Long original = crearYPublicar(
            "INFORME_MEDICO",
            "E2E - original tipo " + sufijo(),
            null
        );
        acceder(original);

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> crearDocumento(
                "FACTURA",
                "E2E - sustituto tipo incorrecto " + sufijo(),
                original
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(11)
    @DisplayName("11 - Sustitución: publicar sustituto marca el original como SUSTITUIDO")
    void sustitucionCompleta() {
        exigirPermisos();

        Long original = crearYPublicar(
            "INFORME_MEDICO",
            "E2E - original sustituible " + sufijo(),
            null
        );
        acceder(original);

        Map<String, Object> sustituto = crearDocumento(
            "INFORME_MEDICO",
            "E2E - sustituto " + sufijo(),
            original
        );

        Long sustitutoId = numeroLong(sustituto.get("id"));
        assertEquals("BORRADOR", sustituto.get("estado"));
        assertEquals(original, numeroLong(sustituto.get("sustituyeADocumentoId")));

        Map<String, Object> sustitutoPublicado = patchMap(
            "/api/documentos/" + sustitutoId,
            pedro.token(),
            residencia.entidadContextoId(),
            Map.of("estado", "DISPONIBLE")
        );

        Map<String, Object> originalActualizado = getMap(
            "/api/documentos/" + original,
            pedro.token(),
            residencia.entidadContextoId()
        );

        assertEquals("DISPONIBLE", sustitutoPublicado.get("estado"));
        assertEquals("SUSTITUIDO", originalActualizado.get("estado"));
    }

    @Test
    @Order(12)
    @DisplayName("12 - Sustitución: un documento no puede tener dos sustitutos")
    void soloUnSustituto() {
        exigirPermisos();

        Long original = crearYPublicar(
            "INFORME_MEDICO",
            "E2E - original un sustituto " + sufijo(),
            null
        );
        acceder(original);

        Map<String, Object> primero = crearDocumento(
            "INFORME_MEDICO",
            "E2E - primer sustituto " + sufijo(),
            original
        );

        assertNotNull(primero.get("id"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> crearDocumento(
                "INFORME_MEDICO",
                "E2E - segundo sustituto " + sufijo(),
                original
            )
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(13)
    @DisplayName("13 - Acceso único: el segundo acceso es rechazado")
    void documentoAccesoUnico() {
        exigirPermisos();

        Long id = crearYPublicar(
            "ACCESO_UNICO",
            "E2E - acceso único " + sufijo(),
            null
        );

        Map<String, Object> primero = acceder(id);
        assertNotNull(primero.get("accesoId"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> acceder(id)
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @Order(14)
    @DisplayName("14 - Concurrencia: acceder y eliminar simultáneamente nunca dejan una inconsistencia")
    void concurrenciaAccederEliminar() throws Exception {
        exigirPermisos();

        Long id = crearYPublicar(
            "INFORME_MEDICO",
            "E2E - concurrencia " + sufijo(),
            null
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);

        try {
            Future<Integer> accesoFuture = executor.submit(() -> {
                preparados.countDown();
                salida.await(5, TimeUnit.SECONDS);
                return ejecutarCodigo(() -> acceder(id));
            });

            Future<Integer> eliminarFuture = executor.submit(() -> {
                preparados.countDown();
                salida.await(5, TimeUnit.SECONDS);
                return ejecutarCodigo(() -> {
                    delete(
                        "/api/documentos/" + id,
                        pedro.token(),
                        residencia.entidadContextoId()
                    );
                    return null;
                });
            });

            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            salida.countDown();

            int codigoAcceso = accesoFuture.get(10, TimeUnit.SECONDS);
            int codigoEliminar = eliminarFuture.get(10, TimeUnit.SECONDS);

            boolean accesoGana =
                codigoAcceso == 200 && codigoEliminar == 409;

            boolean eliminacionGana =
                codigoEliminar == 200 && codigoAcceso == 404;

            assertTrue(
                accesoGana || eliminacionGana,
                "Resultado concurrente inesperado: acceso=" + codigoAcceso
                    + ", eliminar=" + codigoEliminar
            );

            if (accesoGana) {
                Map<String, Object> documento = getMap(
                    "/api/documentos/" + id,
                    pedro.token(),
                    residencia.entidadContextoId()
                );
                assertTrue(numeroLong(documento.get("numeroAccesos")) >= 1L);
            }

            if (eliminacionGana) {
                RestClientResponseException ex = assertThrows(
                    RestClientResponseException.class,
                    () -> getMap(
                        "/api/documentos/" + id,
                        pedro.token(),
                        residencia.entidadContextoId()
                    )
                );
                assertEquals(404, ex.getStatusCode().value());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static void exigirPermisos() {
        assumeTrue(
            permisosDocumentalesCompletos,
            "Pruebas funcionales omitidas porque faltan permisos documentales en el contexto de Pedro"
        );
    }

    private static Map<String, Object> crearDocumento(
        String codigoTipo,
        String nombre,
        Long sustituyeA
    ) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("codigoTipo", codigoTipo);
        cuerpo.put("entidadEmisoraId", servicioMedicoId);
        cuerpo.put("entidadDestinatariaId", anaId);
        cuerpo.put("nombre", nombre);
        cuerpo.put("descripcion", "Documento creado automáticamente por PruebasDocumentosE2E");
        cuerpo.put("referenciaArchivo", "demo://e2e/" + UUID.randomUUID() + ".pdf");
        cuerpo.put("fechaDocumento", "2026-10-04");
        if (sustituyeA != null) cuerpo.put("sustituyeADocumentoId", sustituyeA);

        return postMap(
            "/api/documentos",
            pedro.token(),
            residencia.entidadContextoId(),
            cuerpo
        );
    }

    private static Long crearYPublicar(
        String codigoTipo,
        String nombre,
        Long sustituyeA
    ) {
        Long id = numeroLong(crearDocumento(codigoTipo, nombre, sustituyeA).get("id"));

        Map<String, Object> publicado = patchMap(
            "/api/documentos/" + id,
            pedro.token(),
            residencia.entidadContextoId(),
            Map.of("estado", "DISPONIBLE")
        );

        assertEquals("DISPONIBLE", publicado.get("estado"));
        return id;
    }

    private static Map<String, Object> acceder(Long id) {
        return postMap(
            "/api/documentos/" + id + "/acceder",
            pedro.token(),
            residencia.entidadContextoId(),
            null
        );
    }

    private static Long buscarEntidadExacta(String nombreExacto) {
        String primerTermino = nombreExacto.split(" ")[0];

        List<Map<String, Object>> entidades = getLista(
            "/api/entidades?nombre=" + primerTermino,
            pedro.token(),
            residencia.entidadContextoId()
        );

        return entidades.stream()
            .filter(e -> nombreExacto.equals(e.get("nombre")))
            .map(e -> numeroLong(e.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encuentra la entidad " + nombreExacto));
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
            .orElseThrow(() -> new AssertionError("No existe el contexto " + nombre));
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

    private static Map<String, Object> patchMap(
        String ruta,
        String token,
        Long contextoId,
        Object cuerpo
    ) {
        var spec = api.patch().uri(ruta);
        spec.headers(h -> cabeceras(h, token, contextoId));
        spec.body(cuerpo);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = spec.retrieve().body(Map.class);
        return body;
    }

    private static int delete(String ruta, String token, Long contextoId) {
        var spec = api.delete().uri(ruta);
        spec.headers(h -> cabeceras(h, token, contextoId));
        return spec.retrieve().toBodilessEntity().getStatusCode().value();
    }

    private static int ejecutarCodigo(Callable<Object> accion) {
        try {
            accion.call();
            return 200;
        } catch (RestClientResponseException ex) {
            return ex.getStatusCode().value();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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

    private static String sufijo() {
        return ID_EJECUCION + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombreContexto, String codigoRol) {}
}

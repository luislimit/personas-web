package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Batería funcional E2E del CORE de entidades.
 *
 * Requiere la BD recreada con gestion_personas_completo_es_v2_2.sql
 * para disponer del usuario demo:
 *
 *   admin.residencia / demo
 *
 * Ejecutar:
 *   .\mvnw.cmd "-Dtest=PruebasEntidadesE2E" test
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasEntidadesE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8080");

    private static RestClient api;
    private static Sesion admin;
    private static Contexto residencia;

    private static boolean permisosCompletos;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();

        admin = iniciarSesion("admin.residencia", "demo");
        residencia = contextoPorNombre(admin, "Residencia Los Olivos");

        Map<String, Object> activacion = postMap(
            "/api/autenticacion/contextos/" + residencia.entidadContextoId() + "/activar",
            admin.token(),
            null,
            null
        );

        @SuppressWarnings("unchecked")
        Collection<String> permisos =
            (Collection<String>) activacion.getOrDefault("permisos", List.of());

        permisosCompletos =
            permisos.contains("ENTIDAD.LEER")
            && permisos.contains("ENTIDAD.CREAR")
            && permisos.contains("ENTIDAD.MODIFICAR")
            && permisos.contains("ENTIDAD.ELIMINAR")
            && permisos.contains("DATOS_PERSONALES.LEER")
            && permisos.contains("DATOS_PERSONALES.MODIFICAR");
    }

    @Test
    @Order(1)
    @DisplayName("01 - Diagnóstico: admin de organización tiene permisos completos de entidades")
    void diagnosticoPermisos() {
        assertTrue(
            permisosCompletos,
            """
            El usuario admin.residencia debe tener:
            ENTIDAD.LEER
            ENTIDAD.CREAR
            ENTIDAD.MODIFICAR
            ENTIDAD.ELIMINAR
            DATOS_PERSONALES.LEER
            DATOS_PERSONALES.MODIFICAR
            """
        );
    }

    @Test
    @Order(2)
    @DisplayName("02 - Persona: crear y recuperar datos personales completos")
    void crearPersona() {
        exigirPermisos();

        String marca = "E2E-" + sufijo();

        Map<String, Object> creada = postMap(
            "/api/entidades",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "codigoTipo", "PERSONA",
                "nombre", "Persona " + marca,
                "persona", Map.of(
                    "nombre", "Persona",
                    "primerApellido", marca,
                    "fechaNacimiento", "1990-01-15",
                    "genero", "NO_ESPECIFICADO"
                )
            )
        );

        assertEquals("PERSONA", creada.get("codigoTipo"));
        assertEquals("ACTIVO", creada.get("estado"));
        assertNotNull(creada.get("persona"));

        Long id = numeroLong(creada.get("id"));

        Map<String, Object> recuperada =
            getMap("/api/entidades/" + id, admin.token(), residencia.entidadContextoId());

        @SuppressWarnings("unchecked")
        Map<String, Object> persona = (Map<String, Object>) recuperada.get("persona");

        assertNotNull(persona);
        assertEquals("Persona", persona.get("nombre"));
        assertEquals(marca, persona.get("primerApellido"));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Persona: modificar nombre y datos personales")
    void modificarPersona() {
        exigirPermisos();

        Long id = crearPersonaTemporal("Original " + sufijo());

        Map<String, Object> modificada = patchMap(
            "/api/entidades/" + id,
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "nombre", "Nombre actualizado E2E",
                "persona", Map.of(
                    "nombre", "Nombre",
                    "primerApellido", "Actualizado",
                    "segundoApellido", "E2E",
                    "fechaNacimiento", "1985-02-10",
                    "genero", "NO_ESPECIFICADO"
                )
            )
        );

        assertEquals("Nombre actualizado E2E", modificada.get("nombre"));

        @SuppressWarnings("unchecked")
        Map<String, Object> persona = (Map<String, Object>) modificada.get("persona");

        assertEquals("Nombre", persona.get("nombre"));
        assertEquals("Actualizado", persona.get("primerApellido"));
        assertEquals("E2E", persona.get("segundoApellido"));
    }

    @Test
    @Order(4)
    @DisplayName("04 - Contactos: crear un contacto preferido")
    void crearContacto() {
        exigirPermisos();

        Long id = crearPersonaTemporal("Contacto " + sufijo());

        Map<String, Object> contacto = postMap(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "tipo", "CORREO",
                "valor", "e2e-" + sufijo() + "@example.test",
                "etiqueta", "Personal",
                "preferido", true,
                "activo", true
            )
        );

        assertEquals("CORREO", contacto.get("tipo"));
        assertEquals(Boolean.TRUE, contacto.get("preferido"));
        assertEquals(Boolean.TRUE, contacto.get("activo"));
    }

    @Test
    @Order(5)
    @DisplayName("05 - Contactos: el nuevo preferido desmarca el anterior")
    void soloUnContactoPreferido() {
        exigirPermisos();

        Long id = crearPersonaTemporal("Preferidos " + sufijo());

        Map<String, Object> primero = postMap(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "tipo", "CORREO",
                "valor", "primero-" + sufijo() + "@example.test",
                "preferido", true
            )
        );

        Map<String, Object> segundo = postMap(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "tipo", "CORREO",
                "valor", "segundo-" + sufijo() + "@example.test",
                "preferido", true
            )
        );

        List<Map<String, Object>> contactos = getLista(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId()
        );

        Map<String, Object> primeroActual = contactos.stream()
            .filter(c -> Objects.equals(numeroLong(c.get("id")), numeroLong(primero.get("id"))))
            .findFirst()
            .orElseThrow();

        Map<String, Object> segundoActual = contactos.stream()
            .filter(c -> Objects.equals(numeroLong(c.get("id")), numeroLong(segundo.get("id"))))
            .findFirst()
            .orElseThrow();

        assertEquals(Boolean.FALSE, primeroActual.get("preferido"));
        assertEquals(Boolean.TRUE, segundoActual.get("preferido"));
    }

    @Test
    @Order(6)
    @DisplayName("06 - Contactos: eliminar desactiva, no borra físicamente")
    void eliminarContactoDesactiva() {
        exigirPermisos();

        Long id = crearPersonaTemporal("Eliminar contacto " + sufijo());

        Map<String, Object> creado = postMap(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "tipo", "TELEFONO",
                "valor", "699" + String.format("%06d", new Random().nextInt(1_000_000)),
                "preferido", true
            )
        );

        Long contactoId = numeroLong(creado.get("id"));

        int codigo = delete(
            "/api/entidades/" + id + "/contactos/" + contactoId,
            admin.token(),
            residencia.entidadContextoId()
        );

        assertEquals(204, codigo);

        List<Map<String, Object>> contactos = getLista(
            "/api/entidades/" + id + "/contactos",
            admin.token(),
            residencia.entidadContextoId()
        );

        Map<String, Object> actual = contactos.stream()
            .filter(c -> Objects.equals(numeroLong(c.get("id")), contactoId))
            .findFirst()
            .orElseThrow();

        assertEquals(Boolean.FALSE, actual.get("activo"));
        assertEquals(Boolean.FALSE, actual.get("preferido"));
    }

    @Test
    @Order(7)
    @DisplayName("07 - Relaciones: crear y ver sentido directo e inverso")
    void relacionDirectaEInversa() {
        exigirPermisos();

        Long origen = crearGrupoTemporal("Grupo origen " + sufijo());
        Long destino = crearGrupoTemporal("Grupo destino " + sufijo());

        Map<String, Object> relacion = postMap(
            "/api/entidades/" + origen + "/relaciones",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "entidadDestinoId", destino,
                "codigoTipoRelacion", "GESTION",
                "validoDesde", "2026-10-04"
            )
        );

        Long relacionId = numeroLong(relacion.get("id"));
        assertEquals("DIRECTA", relacion.get("direccion"));
        assertEquals(destino, numeroLong(relacion.get("entidadRelacionadaId")));

        List<Map<String, Object>> inversas = getLista(
            "/api/entidades/" + destino + "/relaciones",
            admin.token(),
            residencia.entidadContextoId()
        );

        Map<String, Object> inversa = inversas.stream()
            .filter(r -> Objects.equals(numeroLong(r.get("id")), relacionId))
            .findFirst()
            .orElseThrow();

        assertEquals("INVERSA", inversa.get("direccion"));
        assertEquals(origen, numeroLong(inversa.get("entidadRelacionadaId")));
    }

    @Test
    @Order(8)
    @DisplayName("08 - Relaciones: no se permite relacionar una entidad consigo misma")
    void relacionConsigoMisma() {
        exigirPermisos();

        Long id = crearGrupoTemporal("Autorelación " + sufijo());

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> postMap(
                "/api/entidades/" + id + "/relaciones",
                admin.token(),
                residencia.entidadContextoId(),
                Map.of(
                    "entidadDestinoId", id,
                    "codigoTipoRelacion", "GESTION"
                )
            )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @Order(9)
    @DisplayName("09 - Relaciones: finalizar relación la conserva con estado FINALIZADO")
    void finalizarRelacion() {
        exigirPermisos();

        Long origen = crearGrupoTemporal("Finalizar origen " + sufijo());
        Long destino = crearGrupoTemporal("Finalizar destino " + sufijo());

        Map<String, Object> creada = postMap(
            "/api/entidades/" + origen + "/relaciones",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "entidadDestinoId", destino,
                "codigoTipoRelacion", "GESTION"
            )
        );

        Long relacionId = numeroLong(creada.get("id"));

        assertEquals(
            204,
            delete(
                "/api/entidades/" + origen + "/relaciones/" + relacionId,
                admin.token(),
                residencia.entidadContextoId()
            )
        );

        List<Map<String, Object>> relaciones = getLista(
            "/api/entidades/" + origen + "/relaciones",
            admin.token(),
            residencia.entidadContextoId()
        );

        Map<String, Object> finalizada = relaciones.stream()
            .filter(r -> Objects.equals(numeroLong(r.get("id")), relacionId))
            .findFirst()
            .orElseThrow();

        assertEquals("FINALIZADO", finalizada.get("estado"));
    }

    @Test
    @Order(10)
    @DisplayName("10 - Entidad: DELETE archiva y mantiene trazabilidad")
    void archivarEntidad() {
        exigirPermisos();

        Long id = crearGrupoTemporal("Archivable " + sufijo());

        assertEquals(
            204,
            delete("/api/entidades/" + id, admin.token(), residencia.entidadContextoId())
        );

        Map<String, Object> archivada =
            getMap("/api/entidades/" + id, admin.token(), residencia.entidadContextoId());

        assertEquals("ARCHIVADO", archivada.get("estado"));
    }

    @Test
    @Order(11)
    @DisplayName("11 - Privacidad: un usuario sin DATOS_PERSONALES.LEER no recibe bloque persona de terceros")
    void privacidadDatosPersonales() {
        Sesion carlos = iniciarSesion("carlos", "demo");
        Contexto infantil = contextoPorNombre(carlos, "CB Tres Cantos");

        // Debe ser un tercero visible dentro del alcance Infantil A.
        // Ana López pertenece a Residencia Los Olivos y, tras aplicar el aislamiento
        // por contexto, Carlos no debe poder ni localizarla desde Infantil A.
        Long laura = buscarEntidadExacta("Laura Sánchez", carlos, infantil);

        Map<String, Object> respuesta =
            getMap("/api/entidades/" + laura, carlos.token(), infantil.entidadContextoId());

        assertEquals("Laura Sánchez", respuesta.get("nombre"));
        assertNull(
            respuesta.get("persona"),
            "El backend no debe enviar datos personales si el usuario no está autorizado"
        );
    }

    @Test
    @Order(12)
    @DisplayName("12 - Alcance de contexto: admin de Residencia no debería listar entidades del Club")
    void contextoNoDebeFiltrarEntidadesDeOtroDominio() {
        exigirPermisos();

        List<Map<String, Object>> resultados = getLista(
            "/api/entidades?nombre=CB",
            admin.token(),
            residencia.entidadContextoId()
        );

        assertTrue(
            resultados.stream().noneMatch(e -> "CB Tres Cantos".equals(e.get("nombre"))),
            """
            El contexto activo es Residencia Los Olivos, pero la búsqueda ha devuelto
            una entidad perteneciente al Club.

            Esto identifica que ENTIDAD.LEER está validando el permiso, pero todavía
            falta aplicar el ALCANCE del contexto a los recursos devueltos.
            """
        );
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static void exigirPermisos() {
        assumeTrue(permisosCompletos, "Faltan permisos de administración de entidades");
    }

    private static Long crearPersonaTemporal(String nombreVisible) {
        Map<String, Object> creada = postMap(
            "/api/entidades",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "codigoTipo", "PERSONA",
                "nombre", nombreVisible,
                "persona", Map.of(
                    "nombre", "Prueba",
                    "primerApellido", "E2E",
                    "fechaNacimiento", "1990-01-01"
                )
            )
        );
        return numeroLong(creada.get("id"));
    }

    private static Long crearGrupoTemporal(String nombre) {
        Map<String, Object> creada = postMap(
            "/api/entidades",
            admin.token(),
            residencia.entidadContextoId(),
            Map.of(
                "codigoTipo", "GRUPO",
                "nombre", nombre
            )
        );
        return numeroLong(creada.get("id"));
    }

    private static Long buscarEntidadExacta(String nombre) {
        return buscarEntidadExacta(nombre, admin, residencia);
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
        return UUID.randomUUID().toString().substring(0, 8);
    }

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombreContexto, String codigoRol) {}
}

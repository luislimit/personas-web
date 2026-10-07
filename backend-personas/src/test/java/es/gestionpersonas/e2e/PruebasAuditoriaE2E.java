package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E transversal de auditoría y política temporal.
 *
 * Combina llamadas reales a la API con comprobaciones directas de BD:
 * - todas las tablas tienen columnas de auditoría;
 * - ningún DATETIME tiene fracciones de segundo;
 * - las fechas funcionales seleccionadas son DATE;
 * - los eventos conservan DATETIME;
 * - creación/modificación registra cuenta, IP y timestamps;
 * - la relación automática de pertenencia también queda auditada.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasAuditoriaE2E {

    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");

    private static final String DB_URL =
        System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/gestion_personas"
                + "?useUnicode=true&characterEncoding=utf8"
                + "&serverTimezone=Europe/Madrid"
                + "&allowPublicKeyRetrieval=true&useSSL=false"
        );

    private static final String DB_USER =
        System.getenv().getOrDefault("DB_USERNAME", "root");

    private static final String DB_PASSWORD =
        System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final List<String> COLUMNAS_AUDITORIA = List.of(
        "fecha_creacion",
        "fecha_modificacion",
        "auditoria_creado_por_cuenta_id",
        "auditoria_ip_creacion",
        "auditoria_modificado_por_cuenta_id",
        "auditoria_ip_modificacion"
    );

    private static RestClient api;
    private static Sesion admin;
    private static Contexto residencia;
    private static final String ID_EJECUCION =
        UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void preparar() throws Exception {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        admin = iniciarSesion("admin.residencia", "demo");
        residencia = contextoPorNombre(admin, "Residencia Los Olivos");

        try (Connection c = conexion()) {
            assertFalse(c.isClosed(), "No se ha podido abrir conexión de comprobación con MySQL");
        }
    }

    @Test
    @Order(1)
    @DisplayName("01 - Todas las tablas incorporan auditoría transversal")
    void todasLasTablasTienenAuditoria() throws Exception {
        Set<String> tablas = new TreeSet<>();

        try (
            Connection c = conexion();
            PreparedStatement ps = c.prepareStatement("""
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) tablas.add(rs.getString(1));
        }

        assertFalse(tablas.isEmpty(), "No se han encontrado tablas en el esquema activo");

        List<String> faltantes = new ArrayList<>();

        try (Connection c = conexion()) {
            for (String tabla : tablas) {
                Set<String> columnas = columnasDe(c, tabla);
                for (String requerida : COLUMNAS_AUDITORIA) {
                    if (!columnas.contains(requerida)) {
                        faltantes.add(tabla + "." + requerida);
                    }
                }
            }
        }

        assertTrue(
            faltantes.isEmpty(),
            "Faltan columnas de auditoría: " + String.join(", ", faltantes)
        );
    }

    @Test
    @Order(2)
    @DisplayName("02 - Los DATETIME no tienen precisión fraccionaria")
    void datetimeSoloHastaSegundos() throws Exception {
        List<String> incorrectas = new ArrayList<>();

        try (
            Connection c = conexion();
            PreparedStatement ps = c.prepareStatement("""
                SELECT TABLE_NAME, COLUMN_NAME, DATETIME_PRECISION
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND DATA_TYPE = 'datetime'
                  AND COALESCE(DATETIME_PRECISION, 0) <> 0
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                incorrectas.add(
                    rs.getString("TABLE_NAME") + "."
                        + rs.getString("COLUMN_NAME")
                        + " precision=" + rs.getInt("DATETIME_PRECISION")
                );
            }
        }

        assertTrue(
            incorrectas.isEmpty(),
            "Hay DATETIME con fracciones de segundo: " + String.join(", ", incorrectas)
        );
    }

    @Test
    @Order(3)
    @DisplayName("03 - Fechas funcionales relevantes son DATE")
    void fechasFuncionalesSonDate() throws Exception {
        Map<String, String> esperadas = new LinkedHashMap<>();

        esperadas.put("persona.fecha_nacimiento", "date");
        esperadas.put("relacion_entidad.valido_desde", "date");
        esperadas.put("relacion_entidad.valido_hasta", "date");
        esperadas.put("documento.fecha_documento", "date");
        esperadas.put("documento.fecha_disponibilidad", "date");
        esperadas.put("solicitud.fecha_solicitud", "date");
        esperadas.put("solicitud.fecha_resolucion", "date");
        esperadas.put("solicitud.fecha_cierre", "date");
        esperadas.put("participante_solicitud.fecha_lectura", "date");
        esperadas.put("participante_solicitud.fecha_respuesta", "date");
        esperadas.put("mensaje_solicitud.fecha_mensaje", "date");

        List<String> errores = new ArrayList<>();

        try (Connection c = conexion()) {
            for (var entry : esperadas.entrySet()) {
                String[] partes = entry.getKey().split("\\.");
                String real = tipoColumna(c, partes[0], partes[1]);

                if (!entry.getValue().equalsIgnoreCase(real)) {
                    errores.add(entry.getKey() + " esperado=" + entry.getValue() + " real=" + real);
                }
            }
        }

        assertTrue(
            errores.isEmpty(),
            "Tipos funcionales incorrectos: " + String.join(", ", errores)
        );
    }

    @Test
    @Order(4)
    @DisplayName("04 - Eventos mantienen fecha y hora")
    void eventosMantienenDatetime() throws Exception {
        try (Connection c = conexion()) {
            assertEquals("datetime", tipoColumna(c, "evento_calendario", "fecha_inicio"));
            assertEquals("datetime", tipoColumna(c, "evento_calendario", "fecha_fin"));
            assertEquals("datetime", tipoColumna(c, "recurrencia_evento", "hasta_fecha"));
        }
    }

    @Test
    @Order(5)
    @DisplayName("05 - Crear entidad registra cuenta, IP y segundos")
    void auditaCreacionEntidad() throws Exception {
        String nombre = "E2E auditoría creación " + sufijo();

        Map<String, Object> creada = postMap(
            "/api/entidades",
            Map.of(
                "codigoTipo", "GRUPO",
                "nombre", nombre
            )
        );

        Long entidadId = numeroLong(creada.get("id"));
        assertNotNull(entidadId);

        FilaAuditoria audit = auditoria("entidad", "id", entidadId);

        assertEquals(admin.cuentaId(), audit.creadoPor());
        assertEquals(admin.cuentaId(), audit.modificadoPor());
        assertIpValida(audit.ipCreacion());
        assertIpValida(audit.ipModificacion());

        assertNotNull(audit.fechaCreacion());
        assertNotNull(audit.fechaModificacion());
        assertEquals(0, audit.fechaCreacion().getNano());
        assertEquals(0, audit.fechaModificacion().getNano());
    }

    @Test
    @Order(6)
    @DisplayName("06 - Modificar entidad actualiza auditoría de modificación")
    void auditaModificacionEntidad() throws Exception {
        String marca = sufijo();

        Map<String, Object> creada = postMap(
            "/api/entidades",
            Map.of(
                "codigoTipo", "GRUPO",
                "nombre", "E2E auditoría original " + marca
            )
        );

        Long entidadId = numeroLong(creada.get("id"));
        FilaAuditoria antes = auditoria("entidad", "id", entidadId);

        // La precisión es de segundos: aseguramos cambio de segundo.
        Thread.sleep(1100);

        patchMap(
            "/api/entidades/" + entidadId,
            Map.of("nombre", "E2E auditoría modificada " + marca)
        );

        FilaAuditoria despues = auditoria("entidad", "id", entidadId);

        assertEquals(antes.creadoPor(), despues.creadoPor());
        assertEquals(antes.ipCreacion(), despues.ipCreacion());
        assertEquals(admin.cuentaId(), despues.modificadoPor());
        assertIpValida(despues.ipModificacion());

        assertEquals(antes.fechaCreacion(), despues.fechaCreacion());
        assertTrue(
            despues.fechaModificacion().isAfter(antes.fechaModificacion()),
            "fecha_modificacion debe avanzar tras una modificación real"
        );
        assertEquals(0, despues.fechaModificacion().getNano());
    }

    @Test
    @Order(7)
    @DisplayName("07 - La pertenencia automática de una entidad nueva también queda auditada")
    void auditaRelacionAutomatica() throws Exception {
        Map<String, Object> creada = postMap(
            "/api/entidades",
            Map.of(
                "codigoTipo", "GRUPO",
                "nombre", "E2E relación auditada " + sufijo()
            )
        );

        Long entidadId = numeroLong(creada.get("id"));

        try (
            Connection c = conexion();
            PreparedStatement ps = c.prepareStatement("""
                SELECT
                    r.id,
                    r.auditoria_creado_por_cuenta_id,
                    r.auditoria_ip_creacion,
                    r.fecha_creacion
                FROM relacion_entidad r
                JOIN tipo_relacion tr ON tr.id = r.tipo_relacion_id
                WHERE r.entidad_origen_id = ?
                  AND r.entidad_destino_id = ?
                  AND tr.codigo = 'PERTENENCIA'
                  AND r.estado = 'ACTIVO'
                ORDER BY r.id DESC
                """)
        ) {
            ps.setLong(1, residencia.entidadContextoId());
            ps.setLong(2, entidadId);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No se creó la pertenencia automática al contexto");
                assertEquals(admin.cuentaId(), nullableLong(rs, "auditoria_creado_por_cuenta_id"));
                assertIpValida(rs.getString("auditoria_ip_creacion"));

                Timestamp ts = rs.getTimestamp("fecha_creacion");
                assertNotNull(ts);
                assertEquals(0, ts.getNanos());

                assertFalse(
                    rs.next(),
                    "Se ha creado más de una PERTENENCIA automática para la misma entidad/contexto"
                );
            }
        }
    }

    // ------------------------------------------------------------
    // JDBC
    // ------------------------------------------------------------

    private static Connection conexion() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static Set<String> columnasDe(Connection c, String tabla) throws SQLException {
        Set<String> columnas = new HashSet<>();

        try (PreparedStatement ps = c.prepareStatement("""
            SELECT COLUMN_NAME
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
            """)) {
            ps.setString(1, tabla);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) columnas.add(rs.getString(1));
            }
        }

        return columnas;
    }

    private static String tipoColumna(Connection c, String tabla, String columna)
        throws SQLException {

        try (PreparedStatement ps = c.prepareStatement("""
            SELECT DATA_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "<no-existe>";
                }
                return rs.getString(1).toLowerCase(Locale.ROOT);
            }
        }
    }

    private static FilaAuditoria auditoria(
        String tabla,
        String columnaId,
        Long id
    ) throws SQLException {

        // tabla/columna son constantes internas del test, no datos de usuario.
        String sql = """
            SELECT
                fecha_creacion,
                fecha_modificacion,
                auditoria_creado_por_cuenta_id,
                auditoria_ip_creacion,
                auditoria_modificado_por_cuenta_id,
                auditoria_ip_modificacion
            FROM %s
            WHERE %s = ?
            """.formatted(tabla, columnaId);

        try (
            Connection c = conexion();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No existe la fila auditada " + tabla + "#" + id);

                Timestamp fc = rs.getTimestamp("fecha_creacion");
                Timestamp fm = rs.getTimestamp("fecha_modificacion");

                return new FilaAuditoria(
                    fc == null ? null : fc.toLocalDateTime(),
                    fm == null ? null : fm.toLocalDateTime(),
                    nullableLong(rs, "auditoria_creado_por_cuenta_id"),
                    rs.getString("auditoria_ip_creacion"),
                    nullableLong(rs, "auditoria_modificado_por_cuenta_id"),
                    rs.getString("auditoria_ip_modificacion")
                );
            }
        }
    }

    private static Long nullableLong(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    private static void assertIpValida(String ip) {
        assertNotNull(ip, "La IP de auditoría no debe ser NULL para una petición HTTP");
        assertFalse(ip.isBlank(), "La IP de auditoría no debe estar vacía");
        assertTrue(ip.length() <= 45, "Una IP normalizada no debe superar 45 caracteres");
    }

    // ------------------------------------------------------------
    // API
    // ------------------------------------------------------------

    private static Sesion iniciarSesion(String usuario, String contrasena) {
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = api.post()
            .uri("/api/autenticacion/iniciar-sesion")
            .body(Map.of(
                "nombreUsuario", usuario,
                "contrasena", contrasena
            ))
            .retrieve()
            .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contextosRaw =
            (List<Map<String, Object>>) respuesta.get("contextos");

        return new Sesion(
            String.valueOf(respuesta.get("tokenAcceso")),
            numeroLong(respuesta.get("cuentaId")),
            contextosRaw.stream()
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
            .orElseThrow(() -> new AssertionError("No existe contexto " + nombre));
    }

    private static Map<String, Object> postMap(String ruta, Object body) {
        var spec = api.post().uri(ruta);
        spec.headers(PruebasAuditoriaE2E::cabeceras);
        spec.body(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = spec.retrieve().body(Map.class);
        return respuesta;
    }

    private static Map<String, Object> patchMap(String ruta, Object body) {
        var spec = api.patch().uri(ruta);
        spec.headers(PruebasAuditoriaE2E::cabeceras);
        spec.body(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = spec.retrieve().body(Map.class);
        return respuesta;
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

    record Sesion(String token, Long cuentaId, List<Contexto> contextos) {}
    record Contexto(Long entidadContextoId, String nombre) {}

    record FilaAuditoria(
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion,
        Long creadoPor,
        String ipCreacion,
        Long modificadoPor,
        String ipModificacion
    ) {}
}

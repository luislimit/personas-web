package es.gestionpersonas.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebasMfaE2E {
    private static final String BASE_URL = System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/gestion_personas?useUnicode=true&characterEncoding=utf8&serverTimezone=Europe/Madrid&allowPublicKeyRetrieval=true&useSSL=false");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static RestClient api;
    private static Sesion sesion;
    private static Contexto contexto;
    private static Long desafioId;
    private static String codigo;

    @BeforeAll
    static void preparar() {
        api = RestClient.builder().baseUrl(BASE_URL).build();
        sesion = login("mfa.demo", "demo");
        contexto = sesion.contextos().stream().filter(c -> "Residencia MFA Demo".equals(c.nombre())).findFirst()
            .orElseThrow(() -> new AssertionError("Falta el contexto demo MFA; recrea la BD con v2.7"));
    }

    @Test @Order(1)
    @DisplayName("01 - La sesión comienza en nivel BASICO y ofrece EMAIL/SMS")
    void estadoBasico() {
        Map<String,Object> estado = getMapa("/api/autenticacion/mfa/estado", sesion.token(), null);
        assertEquals("BASICO", estado.get("nivel"));
        @SuppressWarnings("unchecked") List<Map<String,Object>> canales=(List<Map<String,Object>>)estado.get("canales");
        assertTrue(canales.stream().anyMatch(c -> "EMAIL".equals(c.get("canal"))));
        assertTrue(canales.stream().anyMatch(c -> "SMS".equals(c.get("canal"))));
    }

    @Test @Order(2)
    @DisplayName("02 - Un contexto protegido devuelve MFA_REQUERIDA")
    void requiereMfa() {
        RestClientResponseException ex=assertThrows(RestClientResponseException.class,
            () -> getLista("/api/entidades?nombre=Usuario",sesion.token(),contexto.id()));
        assertEquals(403,ex.getStatusCode().value());
        assertTrue(ex.getResponseBodyAsString().contains("MFA_REQUERIDA"));
    }

    @Test @Order(3)
    @DisplayName("03 - Puede crearse un desafío por EMAIL con código de un solo uso")
    void crearCodigo() throws Exception {
        Map<String,Object> r=postMapa("/api/autenticacion/mfa/desafios",sesion.token(),null,Map.of("canal","EMAIL","mecanismo","CODIGO"));
        desafioId=numero(r.get("desafioId"));
        assertNotNull(desafioId);
        assertEquals("EMAIL",r.get("canal"));
        codigo=recuperarCodigoPorHash(desafioId);
        assertNotNull(codigo);
    }

    @Test @Order(4)
    @DisplayName("04 - Al validar el código la sesión queda elevada a MFA")
    void verificarCodigo() {
        Map<String,Object> r=postMapa("/api/autenticacion/mfa/verificar-codigo",sesion.token(),null,Map.of("desafioId",desafioId,"codigo",codigo));
        assertEquals(Boolean.TRUE,r.get("verificada"));
        assertEquals("MFA",r.get("nivel"));
    }

    @Test @Order(5)
    @DisplayName("05 - La MFA válida sirve para distintos puntos protegidos sin volver a pedirla")
    void reutilizaElevacion() {
        assertDoesNotThrow(() -> getLista("/api/entidades?nombre=Usuario",sesion.token(),contexto.id()));
        assertDoesNotThrow(() -> getLista("/api/calendarios",sesion.token(),contexto.id()));
    }

    @Test @Order(6)
    @DisplayName("06 - El código ya consumido no puede reutilizarse")
    void codigoUnSoloUso() {
        RestClientResponseException ex=assertThrows(RestClientResponseException.class,
            () -> postMapa("/api/autenticacion/mfa/verificar-codigo",sesion.token(),null,Map.of("desafioId",desafioId,"codigo",codigo)));
        assertEquals(403,ex.getStatusCode().value());
    }

    @Test @Order(7)
    @DisplayName("07 - Se puede crear también un enlace de un solo uso por SMS")
    void enlaceSms() throws Exception {
        Map<String,Object> r=postMapa("/api/autenticacion/mfa/desafios",sesion.token(),null,Map.of("canal","SMS","mecanismo","ENLACE"));
        Long id=numero(r.get("desafioId"));
        assertEquals("SMS",r.get("canal"));
        assertEquals("ENLACE",r.get("mecanismo"));
        try(Connection c=DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);PreparedStatement ps=c.prepareStatement("select mecanismo,fecha_consumo,fecha_expiracion from desafio_mfa where id=?")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){assertTrue(rs.next());assertEquals("ENLACE",rs.getString(1));assertNull(rs.getTimestamp(2));assertNotNull(rs.getTimestamp(3));}
        }
    }

    private static String recuperarCodigoPorHash(Long id) throws Exception {
        String hash;
        try(Connection c=DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);PreparedStatement ps=c.prepareStatement("select hash_secreto from desafio_mfa where id=?")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){assertTrue(rs.next());hash=rs.getString(1);}
        }
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        for(int i=0;i<1_000_000;i++){
            String candidato=String.format("%06d",i);
            String h=HexFormat.of().formatHex(md.digest(candidato.getBytes(StandardCharsets.UTF_8)));
            if(hash.equals(h))return candidato;
        }
        throw new AssertionError("No se ha podido recuperar el código MFA de prueba a partir de su hash");
    }

    private static Sesion login(String usuario,String password){
        @SuppressWarnings("unchecked") Map<String,Object> b=api.post().uri("/api/autenticacion/iniciar-sesion").body(Map.of("nombreUsuario",usuario,"contrasena",password)).retrieve().body(Map.class);
        @SuppressWarnings("unchecked") List<Map<String,Object>> raw=(List<Map<String,Object>>)b.get("contextos");
        List<Contexto> cs=raw.stream().map(x->new Contexto(numero(x.get("entidadContextoId")),String.valueOf(x.get("nombreContexto")))).toList();
        return new Sesion(String.valueOf(b.get("tokenAcceso")),numero(b.get("cuentaId")),cs);
    }
    private static Map<String,Object> getMapa(String ruta,String token,Long ctx){var s=api.get().uri(ruta);s.headers(h->headers(h,token,ctx));return s.retrieve().body(Map.class);}
    private static List<?> getLista(String ruta,String token,Long ctx){var s=api.get().uri(ruta);s.headers(h->headers(h,token,ctx));return s.retrieve().body(List.class);}
    private static Map<String,Object> postMapa(String ruta,String token,Long ctx,Object body){var s=api.post().uri(ruta);s.headers(h->headers(h,token,ctx));return s.body(body).retrieve().body(Map.class);}
    private static void headers(HttpHeaders h,String token,Long ctx){if(token!=null)h.setBearerAuth(token);if(ctx!=null)h.set("X-Contexto-Id",ctx.toString());}
    private static Long numero(Object v){return v instanceof Number n?n.longValue():Long.valueOf(String.valueOf(v));}
    record Sesion(String token,Long cuentaId,List<Contexto> contextos){} record Contexto(Long id,String nombre){}
}

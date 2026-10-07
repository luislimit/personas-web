package es.gestionpersonas.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PruebasContextosJerarquicosE2E {
    private static final String BASE_URL =
        System.getenv().getOrDefault("API_PRUEBAS_URL", "http://localhost:8085");

    private final RestClient api = RestClient.builder().baseUrl(BASE_URL).build();

    @Test
    @DisplayName("Contexto raíz + asignaciones internas acumulan permisos sin exponer el equipo como contexto")
    void contextoRaizYPermisosAcumulados() {
        Sesion carlos = login("carlos", "demo");

        assertEquals(1, carlos.contextos().size(),
            "Carlos tiene CB Tres Cantos + Infantil A, pero solo debe ver una raíz de conexión");
        Contexto club = carlos.contextos().get(0);
        assertEquals("CB Tres Cantos", club.nombre());

        Map<String,Object> activado = postSinCuerpoMapa(
            "/api/autenticacion/contextos/" + club.id() + "/activar",
            carlos.token(), null
        );

        @SuppressWarnings("unchecked")
        Collection<String> permisos = (Collection<String>) activado.get("permisos");
        assertTrue(permisos.contains("DOCUMENTO.LEER"),
            "Debe conservar permisos de la asignación raíz AUTOSERVICIO");
        assertTrue(permisos.contains("EVENTO.CREAR"),
            "Debe sumar permisos de la asignación interna Infantil A / GESTOR_CONTEXTO");

        List<Map<String,Object>> calendarios = getListaMap(
            "/api/calendarios", carlos.token(), club.id()
        );

        Map<String,Object> calendarioClub = porNombre(calendarios, "Club - General");
        Map<String,Object> calendarioInfantil = porNombre(calendarios, "Infantil A");
        assertFalse(calendarios.stream().anyMatch(c -> "Junior A".equals(c.get("nombre"))),
            "Una asignación sobre Infantil A no debe abrir Junior A");

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Map<String,Object> evento = postMapa(
            "/api/calendarios/" + numero(calendarioInfantil.get("id")) + "/eventos",
            carlos.token(), club.id(),
            Map.of(
                "titulo", "E2E jerarquía " + sufijo,
                "fechaInicio", LocalDateTime.of(2026, 10, 20, 18, 0).toString(),
                "fechaFin", LocalDateTime.of(2026, 10, 20, 19, 0).toString(),
                "estado", "PROGRAMADO"
            )
        );
        assertNotNull(evento.get("id"));

        RestClientResponseException ex = assertThrows(
            RestClientResponseException.class,
            () -> postMapa(
                "/api/calendarios/" + numero(calendarioClub.get("id")) + "/eventos",
                carlos.token(), club.id(),
                Map.of(
                    "titulo", "E2E no permitido " + sufijo,
                    "fechaInicio", LocalDateTime.of(2026, 10, 21, 18, 0).toString(),
                    "fechaFin", LocalDateTime.of(2026, 10, 21, 19, 0).toString(),
                    "estado", "PROGRAMADO"
                )
            )
        );
        assertEquals(403, ex.getStatusCode().value(),
            "EVENTO.CREAR de Infantil A no debe convertirse en permiso de creación sobre todo el club");
    }

    private Sesion login(String usuario, String password) {
        @SuppressWarnings("unchecked")
        Map<String,Object> body = api.post().uri("/api/autenticacion/iniciar-sesion")
            .body(Map.of("nombreUsuario", usuario, "contrasena", password))
            .retrieve().body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> raw = (List<Map<String,Object>>) body.get("contextos");
        List<Contexto> contextos = raw.stream()
            .map(c -> new Contexto(numero(c.get("entidadContextoId")), String.valueOf(c.get("nombreContexto"))))
            .toList();
        return new Sesion(String.valueOf(body.get("tokenAcceso")), contextos);
    }

    private Map<String,Object> postMapa(String ruta, String token, Long contexto, Object body) {
        var spec = api.post().uri(ruta).headers(h -> headers(h, token, contexto)).body(body);
        return spec.retrieve().body(Map.class);
    }

    private Map<String,Object> postSinCuerpoMapa(String ruta, String token, Long contexto) {
        var spec = api.post().uri(ruta).headers(h -> headers(h, token, contexto));
        return spec.retrieve().body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> getListaMap(String ruta, String token, Long contexto) {
        var spec = api.get().uri(ruta).headers(h -> headers(h, token, contexto));
        return (List<Map<String,Object>>)(List<?>)spec.retrieve().body(List.class);
    }

    private Map<String,Object> porNombre(List<Map<String,Object>> items, String nombre) {
        return items.stream().filter(x -> nombre.equals(x.get("nombre"))).findFirst()
            .orElseThrow(() -> new AssertionError("No aparece " + nombre));
    }

    private static void headers(HttpHeaders headers, String token, Long contexto) {
        headers.setBearerAuth(token);
        if (contexto != null) headers.set("X-Contexto-Id", String.valueOf(contexto));
    }

    private static Long numero(Object value) {
        return ((Number) value).longValue();
    }

    private record Contexto(Long id, String nombre) {}
    private record Sesion(String token, List<Contexto> contextos) {}
}

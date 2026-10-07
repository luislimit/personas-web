package es.gestionpersonas.configuracion;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionOpenApi {

    @Bean
    OpenAPI gestionPersonasOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Gestión de Personas API")
                .version("v1")
                .description("""
                    API REST del CORE genérico de Gestión de Personas.
                    Trabaja sobre la base MySQL existente y expone entidades,
                    relaciones, contactos, cuentas, contextos, calendarios,
                    solicitudes y documentos.
                    """)
                .contact(new Contact().name("Equipo Gestión de Personas")));
    }
}

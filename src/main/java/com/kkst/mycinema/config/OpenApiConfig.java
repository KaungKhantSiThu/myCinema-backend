package com.kkst.mycinema.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${tmdb.api.enabled:false}")
    private boolean tmdbEnabled;

    @Bean
    public OpenAPI customOpenAPI() {
        String description = "A scalable, thread-safe cinema ticket booking system with optimistic locking for concurrency control.\n\n" +
                "**Key Features:**\n" +
                "- Movie and Show Management\n" +
                "- Seat Booking with Optimistic Locking\n" +
                "- Seat Hold/Lock Mechanism\n" +
                "- User Authentication with JWT\n" +
                "- Admin Dashboard and Revenue Reporting\n";
        
        if (tmdbEnabled) {
            description += "- **TMDb Integration** - Search and import movies from The Movie Database\n";
        } else {
            description += "- ⚠️  TMDb Integration is **disabled**. Set TMDB_API_KEY to enable movie import features.\n";
        }

        description += "\n**Authentication:**\n" +
                "1. Login via `/api/auth/login` to get JWT token\n" +
                "2. Click 'Authorize' button and enter: `Bearer <your-token>`\n" +
                "3. Admin endpoints require ROLE_ADMIN\n\n" +
                "**Default Admin Credentials:**\n" +
                "- Email: `admin@cinema.com`\n" +
                "- Password: `Admin123!`";

        return new OpenAPI()
                .info(new Info()
                        .title("Cinema Booking System API")
                        .version("1.0.0")
                        .description(description)
                        .contact(new Contact()
                                .name("Cinema Booking Team")
                                .email("support@cinema-booking.com")
                                .url("https://github.com/your-repo/cinema-booking"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT token obtained from /api/auth/login. Format: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}


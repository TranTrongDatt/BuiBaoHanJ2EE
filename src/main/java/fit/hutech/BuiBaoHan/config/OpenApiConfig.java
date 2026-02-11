package fit.hutech.BuiBaoHan.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
@SecuritySchemes({
        @SecurityScheme(
                name = "bearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "JWT token authentication"
        ),
        @SecurityScheme(
                name = "cookieAuth",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.COOKIE,
                paramName = "MV_ACCESS_TOKEN",
                description = "JWT token in HttpOnly cookie"
        )
})
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:9090}")
    private String serverPort;

    @Bean
    public OpenAPI miniverseOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .servers(servers())
                .tags(apiTags())
                .components(new Components());
    }

    private Info apiInfo() {
        return new Info()
                .title("MiniVerse API")
                .description("""
                        # MiniVerse - E-Commerce & Library Management System
                        
                        Welcome to the MiniVerse API documentation. This API powers:
                        
                        - 📚 **E-Commerce**: Browse, search, and purchase books
                        - 📖 **Library System**: Borrow, return, and manage library cards
                        - ✍️ **Blog Platform**: Read and write book reviews
                        - 🤖 **AI Assistant**: Chat with our AI for book recommendations
                        - 💬 **Real-time Chat**: Connect with other readers
                        
                        ## Authentication
                        
                        Most endpoints require authentication. We support:
                        - **JWT Bearer Token**: Send token in `Authorization: Bearer <token>` header
                        - **HttpOnly Cookie**: Automatic authentication via secure cookies
                        
                        ## Rate Limiting
                        
                        API requests are rate-limited per user/IP:
                        - **General API**: 100 requests/minute
                        - **Login/Register**: 10 requests/minute
                        - **AI Chat**: 20 requests/minute
                        """)
                .version(appVersion)
                .contact(contact())
                .license(license());
    }

    private Contact contact() {
        return new Contact()
                .name("MiniVerse Support")
                .email("support@miniverse.com")
                .url("https://miniverse.com");
    }

    private License license() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("MiniVerse Documentation")
                .url("https://docs.miniverse.com");
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Development Server"),
                new Server()
                        .url("https://api.miniverse.com")
                        .description("Production Server")
        );
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag().name("Authentication").description("User authentication and authorization"),
                new Tag().name("Users").description("User profile and account management"),
                new Tag().name("Books").description("Book catalog and search"),
                new Tag().name("Categories").description("Book categories management"),
                new Tag().name("Fields").description("Book fields/genres management"),
                new Tag().name("Authors").description("Author management"),
                new Tag().name("Publishers").description("Publisher management"),
                new Tag().name("Cart").description("Shopping cart operations"),
                new Tag().name("Orders").description("Order management"),
                new Tag().name("Payments").description("Payment processing"),
                new Tag().name("Wishlist").description("User wishlist"),
                new Tag().name("Library").description("Library card and borrow management"),
                new Tag().name("Blog").description("Blog posts and articles"),
                new Tag().name("Comments").description("Comments on posts and books"),
                new Tag().name("Likes").description("Like/unlike content"),
                new Tag().name("Follow").description("Follow/unfollow users"),
                new Tag().name("Notifications").description("User notifications"),
                new Tag().name("Chat").description("AI and user-to-user chat"),
                new Tag().name("Admin").description("Administrative operations")
        );
    }
}

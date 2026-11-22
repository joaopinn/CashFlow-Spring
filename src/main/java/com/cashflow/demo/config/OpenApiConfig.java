package com.cashflow.demo.config;

import com.cashflow.demo.error.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI cashflowOpenApi() {
        return new OpenAPI().info(new Info().title("CashFlow API").version("1.0.0")
                .description("Gestão financeira. Cadastre-se em /auth/register e obtenha um token em /auth/login. " +
                        "Informe o JWT no botão Authorize. USER acessa seus próprios dados; ADMIN gerencia usuários e tipos. " +
                        "Paginação: page inicia em 0, size de 1 a 100, sort=campo,asc ou campo,desc. " +
                        "Filtros textuais são parciais e ignoram maiúsculas. Datas usam yyyy-MM-dd."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer standardResponses() {
        return api -> {
            ModelConverters.getInstance().read(ApiError.class).forEach(api.getComponents()::addSchemas);
            var statuses = Map.of("400", "Dados, filtros ou parâmetros inválidos",
                    "401", "Credenciais inválidas, token ausente, inválido ou expirado",
                    "403", "Sem permissão para este usuário ou operação",
                    "404", "Recurso não encontrado no escopo do usuário",
                    "409", "Conflito de e-mail único ou vínculos existentes",
                    "500", "Erro interno, sem exposição de detalhes sensíveis");
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (path.startsWith("/auth/")) operation.setSecurity(java.util.List.of());
                String success = switch (method) {
                    case POST -> path.equals("/auth/login") ? "200" : "201";
                    case DELETE -> "204";
                    default -> "200";
                };
                if (!success.equals("200") && operation.getResponses().containsKey("200")) {
                    var response = operation.getResponses().remove("200");
                    if (success.equals("204")) response.setContent(null);
                    operation.getResponses().addApiResponse(success, response.description(
                            success.equals("201") ? "Criado; Location aponta para o recurso" : "Excluído, sem corpo"));
                }
                statuses.forEach((code, description) -> operation.getResponses().addApiResponse(code,
                        new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))))));
            }));
        };
    }
}

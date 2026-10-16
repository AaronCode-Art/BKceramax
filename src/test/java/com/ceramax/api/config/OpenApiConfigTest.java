package com.ceramax.api.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void marksOnlyProtectedOperationsAsBearerAuthenticated() {
        OpenAPI openApi = new OpenAPI().paths(new Paths()
            .addPathItem("/api/auth/staff/login", new PathItem().post(new Operation()))
            .addPathItem("/api/v1/public/catalogo", new PathItem().get(new Operation()))
            .addPathItem("/api/v1/clientes", new PathItem().post(new Operation()))
            .addPathItem("/api/v1/usuarios", new PathItem().get(new Operation()))
            .addPathItem("/actuator/health/readiness", new PathItem().get(new Operation())));

        new OpenApiConfig().documentOperations().customise(openApi);

        assertNull(openApi.getPaths().get("/api/auth/staff/login").getPost().getSecurity());
        assertNull(openApi.getPaths().get("/api/v1/public/catalogo").getGet().getSecurity());
        assertNull(openApi.getPaths().get("/api/v1/clientes").getPost().getSecurity());
        assertTrue(openApi.getPaths().get("/api/v1/usuarios").getGet().getSecurity().getFirst()
            .containsKey("bearerAuth"));
        assertNull(openApi.getPaths().get("/actuator/health/readiness").getGet().getSecurity());
        assertTrue(openApi.getPaths().get("/api/v1/public/catalogo").getGet().getTags()
            .contains("Catálogo público"));
        assertTrue(openApi.getPaths().get("/api/v1/usuarios").getGet().getTags().contains("Usuarios"));
    }
}

package com.qualitypilot.campaignlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiContractTests {
    private static final Path CONTRACT = Path.of("openapi", "openapi.json");
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options", "trace");

    @Autowired ObjectMapper mapper;
    @Autowired RequestMappingHandlerMapping handlerMapping;

    @Test
    void contractIsOpenApi31AndEveryReferenceResolves() throws IOException {
        JsonNode document = mapper.readTree(CONTRACT.toFile());

        assertThat(document.path("openapi").asText()).isEqualTo("3.1.0");
        assertThat(document.path("info").path("version").asText()).isEqualTo("1.0.0");
        assertThat(document.path("paths").size()).isPositive();
        assertReferencesResolve(document, document);
    }

    @Test
    void openApiOperationsExactlyMatchImplementedApiRoutes() throws IOException {
        JsonNode document = mapper.readTree(CONTRACT.toFile());
        Set<String> documented = documentedOperations(document);
        Set<String> implemented = implementedOperations();

        assertThat(documented).hasSize(21);
        assertThat(documented).containsExactlyInAnyOrderElementsOf(implemented);
    }

    @Test
    void everyOperationHasStableIdTagAndResponseContract() throws IOException {
        JsonNode paths = mapper.readTree(CONTRACT.toFile()).path("paths");
        Set<String> operationIds = new HashSet<>();

        paths.properties().forEach(path -> path.getValue().properties().forEach(method -> {
            if (!HTTP_METHODS.contains(method.getKey())) return;
            JsonNode operation = method.getValue();
            assertThat(operation.path("operationId").asText()).isNotBlank();
            assertThat(operationIds.add(operation.path("operationId").asText())).isTrue();
            assertThat(operation.path("tags").isArray()).isTrue();
            assertThat(operation.path("tags")).isNotEmpty();
            assertThat(operation.path("responses").isObject()).isTrue();
            assertThat(operation.path("responses")).isNotEmpty();
        }));
    }

    private Set<String> documentedOperations(JsonNode document) {
        Set<String> operations = new HashSet<>();
        document.path("paths").properties().forEach(path ->
                path.getValue().properties().forEach(method -> {
                    if (HTTP_METHODS.contains(method.getKey())) {
                        operations.add(method.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey());
                    }
                }));
        return operations;
    }

    private Set<String> implementedOperations() {
        Set<String> operations = new HashSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().equals("com.qualitypilot.campaignlab.api")) return;
            for (String pattern : mapping.getPatternValues()) {
                if (!pattern.startsWith("/api")) continue;
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    operations.add(method.name() + " " + pattern);
                }
            }
        });
        return operations;
    }

    private void assertReferencesResolve(JsonNode root, JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().equals("$ref")) {
                    String reference = field.getValue().asText();
                    assertThat(reference).startsWith("#/");
                    JsonNode resolved = root.at(reference.substring(1));
                    assertThat(resolved.isMissingNode())
                            .as("unresolved OpenAPI reference: %s", reference)
                            .isFalse();
                } else {
                    assertReferencesResolve(root, field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(child -> assertReferencesResolve(root, child));
        }
    }
}

package com.gatling.openapi.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GenerateGatlingDataMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void testOpenApiLoader() throws Exception {
        File testFile = new File("src/test/resources/test-openapi.yml");
        if (!testFile.exists()) {
            System.out.println("Fichier de test non trouvé: " + testFile.getAbsolutePath());
            return;
        }

        OpenApiLoader loader = new OpenApiLoader();
        var openAPI = loader.load(testFile);

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("API de Test pour Gatling", openAPI.getInfo().getTitle());
        assertNotNull(openAPI.getPaths());
        assertTrue(openAPI.getPaths().containsKey("/users"));
    }

    @Test
    void testSchemaExampleGenerator() {
        io.swagger.v3.oas.models.media.Schema<Object> schema = new io.swagger.v3.oas.models.media.Schema<>();
        schema.setType("string");
        schema.setFormat("email");

        RefResolver refResolver = new RefResolver(null);
        SchemaExampleGenerator generator = new SchemaExampleGenerator(refResolver, 12345L, 3);

        Object example = generator.generateExample(schema, 0);
        assertNotNull(example);
        assertTrue(example.toString().contains("@example.com"));
    }
}

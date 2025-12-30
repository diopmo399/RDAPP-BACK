package com.gatling.openapi.plugin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;

public class OpenApiLoader {

    public OpenAPI load(File file) throws Exception {
        if (!file.exists()) {
            throw new Exception("Le fichier OpenAPI n'existe pas: " + file.getAbsolutePath());
        }

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readLocation(file.getAbsolutePath(), null, options);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            System.err.println("Avertissements lors du parsing:");
            result.getMessages().forEach(msg -> System.err.println("  - " + msg));
        }

        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new Exception("Impossible de parser le fichier OpenAPI. Messages: " + result.getMessages());
        }

        return openAPI;
    }
}

package com.gatling.openapi.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.io.File;
import java.util.*;

public class EndpointDatasetGenerator {

    private final OpenAPI openAPI;
    private final RefResolver refResolver;
    private final SchemaExampleGenerator exampleGenerator;
    private final int rows;
    private final String jsonColumnName;
    private final boolean addCorrelationColumns;
    private final ObjectMapper objectMapper;

    public EndpointDatasetGenerator(OpenAPI openAPI, RefResolver refResolver,
                                   SchemaExampleGenerator exampleGenerator, int rows,
                                   String jsonColumnName, boolean addCorrelationColumns) {
        this.openAPI = openAPI;
        this.refResolver = refResolver;
        this.exampleGenerator = exampleGenerator;
        this.rows = rows;
        this.jsonColumnName = jsonColumnName;
        this.addCorrelationColumns = addCorrelationColumns;
        this.objectMapper = new ObjectMapper();
    }

    public void generateEndpointDatasets(File endpointsDir, List<String> includePaths,
                                        GenerateGatlingDataMojo.OutputFormat format) throws Exception {

        openAPI.getPaths().forEach((path, pathItem) -> {
            if (shouldIncludePath(path, includePaths)) {
                try {
                    generatePathDatasets(endpointsDir, path, pathItem, format);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la génération du dataset pour " + path + ": " + e.getMessage());
                }
            }
        });
    }

    private boolean shouldIncludePath(String path, List<String> includePaths) {
        if (includePaths == null || includePaths.isEmpty()) {
            return true;
        }

        for (String pattern : includePaths) {
            if (path.contains(pattern) || path.matches(pattern)) {
                return true;
            }
        }

        return false;
    }

    private void generatePathDatasets(File endpointsDir, String path, PathItem pathItem,
                                     GenerateGatlingDataMojo.OutputFormat format) throws Exception {

        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : operations.entrySet()) {
            PathItem.HttpMethod method = entry.getKey();
            Operation operation = entry.getValue();

            String fileName = sanitizePath(method.name() + "_" + path) + "_request";

            // Générer le dataset pour la requête
            generateOperationDataset(endpointsDir, fileName, path, method.name(), operation, format);
        }
    }

    private void generateOperationDataset(File endpointsDir, String fileName, String path,
                                         String method, Operation operation,
                                         GenerateGatlingDataMojo.OutputFormat format) throws Exception {

        List<Map<String, Object>> dataset = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            Map<String, Object> row = new LinkedHashMap<>();

            // Ajouter les paramètres de path
            List<String> pathParams = extractPathParams(path);
            for (String param : pathParams) {
                row.put(param, "value_" + i);
            }

            // Ajouter les query parameters
            if (operation.getParameters() != null) {
                for (Parameter parameter : operation.getParameters()) {
                    if ("query".equals(parameter.getIn())) {
                        row.put(parameter.getName(), generateParameterValue(parameter, i));
                    } else if ("path".equals(parameter.getIn())) {
                        // Override la valeur par défaut si un schema existe
                        row.put(parameter.getName(), generateParameterValue(parameter, i));
                    }
                }
            }

            // Ajouter le requestBody
            if (operation.getRequestBody() != null) {
                addRequestBody(row, operation.getRequestBody(), i);
            }

            // Ajouter des colonnes de corrélation
            if (addCorrelationColumns) {
                row.putIfAbsent("correlationId", "corr_" + i);
                row.putIfAbsent("userId", "user_" + (i % 100));
            }

            dataset.add(row);
        }

        // Écrire le dataset
        if (format == GenerateGatlingDataMojo.OutputFormat.CSV ||
            format == GenerateGatlingDataMojo.OutputFormat.BOTH) {
            File csvFile = new File(endpointsDir, fileName + ".csv");
            CsvWriter csvWriter = new CsvWriter(csvFile);
            csvWriter.writeDataset(dataset);
        }

        if (format == GenerateGatlingDataMojo.OutputFormat.JSON ||
            format == GenerateGatlingDataMojo.OutputFormat.BOTH) {
            File jsonFile = new File(endpointsDir, fileName + ".json");
            JsonWriter jsonWriter = new JsonWriter(jsonFile);
            jsonWriter.writeDataset(dataset);
        }
    }

    private List<String> extractPathParams(String path) {
        List<String> params = new ArrayList<>();
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                params.add(part.substring(1, part.length() - 1));
            }
        }
        return params;
    }

    private Object generateParameterValue(Parameter parameter, int index) {
        Schema<?> schema = parameter.getSchema();
        if (schema != null) {
            schema = refResolver.resolveSchema(schema);
            schema.setName(parameter.getName());
            return exampleGenerator.generateExample(schema, index);
        }

        // Fallback
        return "param_" + index;
    }

    private void addRequestBody(Map<String, Object> row, RequestBody requestBody, int index) {
        Content content = requestBody.getContent();
        if (content == null) {
            return;
        }

        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            // Essayer de prendre le premier content type disponible
            mediaType = content.values().stream().findFirst().orElse(null);
        }

        if (mediaType != null && mediaType.getSchema() != null) {
            Schema<?> schema = refResolver.resolveSchema(mediaType.getSchema());

            if (schema != null) {
                Object example = exampleGenerator.generateExample(schema, index);

                // Générer le JSON complet dans une colonne
                try {
                    String jsonBody = objectMapper.writeValueAsString(example);
                    row.put(jsonColumnName, jsonBody);

                    // Si c'est un objet, ajouter aussi quelques champs utiles pour corrélation
                    if (example instanceof Map) {
                        Map<String, Object> exampleMap = (Map<String, Object>) example;
                        // Ajouter id, email si présents
                        if (exampleMap.containsKey("id")) {
                            row.put("id", exampleMap.get("id"));
                        }
                        if (exampleMap.containsKey("email")) {
                            row.put("email", exampleMap.get("email"));
                        }
                        if (exampleMap.containsKey("userId")) {
                            row.put("userId", exampleMap.get("userId"));
                        }
                    }
                } catch (Exception e) {
                    row.put(jsonColumnName, "{}");
                }
            }
        }
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[/{}:?&=]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }
}

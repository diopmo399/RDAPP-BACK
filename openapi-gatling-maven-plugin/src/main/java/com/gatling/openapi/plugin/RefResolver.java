package com.gatling.openapi.plugin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RefResolver {

    private final OpenAPI openAPI;

    public RefResolver(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public Schema<?> resolveSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }

        if (schema.get$ref() != null) {
            return resolveRef(schema.get$ref());
        }

        // Gérer allOf (fusion des schemas)
        if (schema instanceof ComposedSchema) {
            ComposedSchema composed = (ComposedSchema) schema;
            if (composed.getAllOf() != null && !composed.getAllOf().isEmpty()) {
                return mergeAllOf(composed.getAllOf());
            }
            // Pour oneOf/anyOf, prendre le premier
            if (composed.getOneOf() != null && !composed.getOneOf().isEmpty()) {
                return resolveSchema(composed.getOneOf().get(0));
            }
            if (composed.getAnyOf() != null && !composed.getAnyOf().isEmpty()) {
                return resolveSchema(composed.getAnyOf().get(0));
            }
        }

        return schema;
    }

    private Schema<?> resolveRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }

        String schemaName = ref.substring("#/components/schemas/".length());

        if (openAPI.getComponents() != null &&
            openAPI.getComponents().getSchemas() != null &&
            openAPI.getComponents().getSchemas().containsKey(schemaName)) {

            Schema<?> resolved = openAPI.getComponents().getSchemas().get(schemaName);

            // Résoudre récursivement si le schema résolu contient lui-même une ref
            if (resolved.get$ref() != null) {
                return resolveRef(resolved.get$ref());
            }

            return resolved;
        }

        return null;
    }

    private Schema<?> mergeAllOf(List<Schema> allOfSchemas) {
        Schema<?> merged = new Schema<>();
        Map<String, Schema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Schema<?> schema : allOfSchemas) {
            Schema<?> resolved = resolveSchema(schema);
            if (resolved == null) continue;

            // Fusionner les propriétés
            if (resolved.getProperties() != null) {
                properties.putAll(resolved.getProperties());
            }

            // Fusionner les required
            if (resolved.getRequired() != null) {
                required.addAll(resolved.getRequired());
            }

            // Copier d'autres attributs du premier schema
            if (merged.getType() == null && resolved.getType() != null) {
                merged.setType(resolved.getType());
            }
            if (merged.getDescription() == null && resolved.getDescription() != null) {
                merged.setDescription(resolved.getDescription());
            }
        }

        merged.setProperties(properties);
        if (!required.isEmpty()) {
            merged.setRequired(required);
        }

        return merged;
    }

    public Schema<?> getItemsSchema(Schema<?> schema) {
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            return resolveSchema(arraySchema.getItems());
        }
        return null;
    }
}

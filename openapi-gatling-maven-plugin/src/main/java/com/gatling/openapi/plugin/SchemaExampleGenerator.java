package com.gatling.openapi.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SchemaExampleGenerator {

    private final RefResolver refResolver;
    private final Random random;
    private final int arraysMaxSize;
    private final ObjectMapper objectMapper;

    private static final String[] FIRST_NAMES = {"Jean", "Marie", "Pierre", "Sophie", "Luc", "Anne", "Michel", "Claire"};
    private static final String[] LAST_NAMES = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand"};
    private static final String[] CITIES = {"Paris", "Lyon", "Marseille", "Toulouse", "Nice", "Nantes", "Bordeaux", "Lille"};

    public SchemaExampleGenerator(RefResolver refResolver, long seed, int arraysMaxSize) {
        this.refResolver = refResolver;
        this.random = new Random(seed);
        this.arraysMaxSize = arraysMaxSize;
        this.objectMapper = new ObjectMapper();
    }

    public Object generateExample(Schema<?> schema, int index) {
        schema = refResolver.resolveSchema(schema);

        if (schema == null) {
            return null;
        }

        // Vérifier si un exemple existe déjà
        if (schema.getExample() != null) {
            return schema.getExample();
        }

        // Vérifier l'enum
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            int enumIndex = (index + random.nextInt(schema.getEnum().size())) % schema.getEnum().size();
            return schema.getEnum().get(enumIndex);
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if (type == null && schema.getProperties() != null) {
            type = "object";
        }

        switch (type != null ? type : "string") {
            case "string":
                return generateString(schema, format, index);
            case "integer":
                return generateInteger(schema, index);
            case "number":
                return generateNumber(schema, index);
            case "boolean":
                return index % 2 == 0;
            case "array":
                return generateArray(schema, index);
            case "object":
                return generateObject(schema, index);
            default:
                return "unknown-type-" + type;
        }
    }

    private String generateString(Schema<?> schema, String format, int index) {
        if (format != null) {
            switch (format) {
                case "email":
                    return "test" + index + "@example.com";
                case "uuid":
                    return generateUUID(index);
                case "date":
                    return LocalDate.of(2025, 1, 1).plusDays(index).format(DateTimeFormatter.ISO_DATE);
                case "date-time":
                    return LocalDateTime.of(2025, 1, 1, 0, 0).plusHours(index).format(DateTimeFormatter.ISO_DATE_TIME);
                case "uri":
                case "url":
                    return "https://example.com/resource/" + index;
                case "phone":
                    return String.format("+33%09d", 600000000 + index);
                default:
                    break;
            }
        }

        // Vérifier le pattern ou les contraintes de longueur
        Integer minLength = schema.getMinLength();
        Integer maxLength = schema.getMaxLength();

        String baseValue = generateStringByName(schema.getName(), index);

        if (minLength != null || maxLength != null) {
            int targetLength = maxLength != null ? maxLength : (minLength != null ? minLength : baseValue.length());
            if (baseValue.length() > targetLength) {
                baseValue = baseValue.substring(0, targetLength);
            } else if (baseValue.length() < (minLength != null ? minLength : 0)) {
                baseValue = baseValue + "x".repeat(minLength - baseValue.length());
            }
        }

        return baseValue;
    }

    private String generateStringByName(String name, int index) {
        if (name == null) {
            return "value_" + index;
        }

        String lowerName = name.toLowerCase();
        if (lowerName.contains("email")) {
            return "test" + index + "@example.com";
        } else if (lowerName.contains("firstname") || lowerName.contains("prenom")) {
            return FIRST_NAMES[index % FIRST_NAMES.length];
        } else if (lowerName.contains("lastname") || lowerName.contains("nom")) {
            return LAST_NAMES[index % LAST_NAMES.length];
        } else if (lowerName.contains("city") || lowerName.contains("ville")) {
            return CITIES[index % CITIES.length];
        } else if (lowerName.contains("phone") || lowerName.contains("tel")) {
            return String.format("+33%09d", 600000000 + index);
        } else if (lowerName.contains("address") || lowerName.contains("adresse")) {
            return (index + 1) + " rue de la Paix";
        } else if (lowerName.contains("description")) {
            return "Description du " + name + " numéro " + index;
        } else if (lowerName.contains("title") || lowerName.contains("titre")) {
            return "Titre " + index;
        } else if (lowerName.contains("name")) {
            return "Name_" + index;
        } else {
            return name + "_" + index;
        }
    }

    private Integer generateInteger(Schema<?> schema, int index) {
        BigDecimal min = schema.getMinimum();
        BigDecimal max = schema.getMaximum();

        int minInt = min != null ? min.intValue() : 0;
        int maxInt = max != null ? max.intValue() : 1000;

        if (minInt >= maxInt) {
            return minInt + index;
        }

        return minInt + (index % (maxInt - minInt));
    }

    private Number generateNumber(Schema<?> schema, int index) {
        BigDecimal min = schema.getMinimum();
        BigDecimal max = schema.getMaximum();

        double minDouble = min != null ? min.doubleValue() : 0.0;
        double maxDouble = max != null ? max.doubleValue() : 1000.0;

        if (minDouble >= maxDouble) {
            return minDouble + index;
        }

        return minDouble + ((index * 1.5) % (maxDouble - minDouble));
    }

    private List<Object> generateArray(Schema<?> schema, int index) {
        Schema<?> itemsSchema = refResolver.getItemsSchema(schema);
        if (itemsSchema == null) {
            return Collections.emptyList();
        }

        Integer minItems = schema.getMinItems();
        Integer maxItems = schema.getMaxItems();

        int size = arraysMaxSize;
        if (maxItems != null && maxItems < size) {
            size = maxItems;
        }
        if (minItems != null && minItems > size) {
            size = minItems;
        }

        List<Object> array = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            array.add(generateExample(itemsSchema, index * 10 + i));
        }

        return array;
    }

    private Map<String, Object> generateObject(Schema<?> schema, int index) {
        Map<String, Object> object = new LinkedHashMap<>();

        if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
            return object;
        }

        schema.getProperties().forEach((propName, propSchema) -> {
            Schema<?> resolvedPropSchema = refResolver.resolveSchema((Schema<?>) propSchema);
            if (resolvedPropSchema != null) {
                resolvedPropSchema.setName(propName);
                object.put(propName, generateExample(resolvedPropSchema, index));
            }
        });

        return object;
    }

    private String generateUUID(int index) {
        // Générer un UUID déterministe basé sur l'index
        long mostSigBits = random.nextLong() + index;
        long leastSigBits = random.nextLong() + index;
        UUID uuid = new UUID(mostSigBits, leastSigBits);
        return uuid.toString();
    }

    public String generateExampleAsJson(Schema<?> schema, int index) {
        try {
            Object example = generateExample(schema, index);
            return objectMapper.writeValueAsString(example);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Map<String, Object> flattenObject(Map<String, Object> object, String prefix) {
        Map<String, Object> flattened = new LinkedHashMap<>();

        object.forEach((key, value) -> {
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map) {
                flattened.putAll(flattenObject((Map<String, Object>) value, newKey));
            } else if (value instanceof List) {
                try {
                    flattened.put(newKey, objectMapper.writeValueAsString(value));
                } catch (Exception e) {
                    flattened.put(newKey, value.toString());
                }
            } else {
                flattened.put(newKey, value);
            }
        });

        return flattened;
    }
}

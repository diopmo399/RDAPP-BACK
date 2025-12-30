package com.gatling.openapi.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonWriter {

    private final File file;
    private final ObjectMapper objectMapper;

    public JsonWriter(File file) {
        this.file = file;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void writeSchemaData(Schema<?> schema, int rows, SchemaExampleGenerator generator) throws IOException {
        List<Object> data = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            Object example = generator.generateExample(schema, i);
            data.add(example);
        }

        try (FileWriter writer = new FileWriter(file)) {
            objectMapper.writeValue(writer, data);
        }
    }

    public void writeDataset(List<Map<String, Object>> dataset) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            objectMapper.writeValue(writer, dataset);
        }
    }
}
